import {
  NEON_AUTH_SESSION_COOKIE_NAME,
  handleAuthProxyRequest,
  parseSetCookies,
  processAuthMiddleware,
} from '@neondatabase/auth/server';
import { z } from 'zod';

import { HttpError, errorResponse, json, methodNotAllowed, parseJson, requestId } from '../http.js';
import {
  decryptSessionToken,
  encryptSessionToken,
  pkceChallenge,
  randomBase64Url,
  sha256Hex,
} from './mobile-auth-crypto.js';
import {
  PostgresMobileAuthHandoffStore,
  type MobileAuthHandoffStore,
} from './mobile-auth-store.js';

const HANDOFF_TTL_MS = 5 * 60 * 1000;
const PKCE_COOKIE_NAME = '__Secure-wmw-mobile-pkce';
const PKCE_CHALLENGE_PATTERN = /^[A-Za-z0-9_-]{43}$/;
const PKCE_VERIFIER_PATTERN = /^[A-Za-z0-9._~-]{43,128}$/;
const CODE_PATTERN = /^[A-Za-z0-9_-]{43}$/;

const exchangeSchema = z.object({
  code: z.string().regex(CODE_PATTERN),
  verifier: z.string().regex(PKCE_VERIFIER_PATTERN),
}).strict();

interface MobileAuthConfig {
  neonAuthBaseUrl: string;
  publicBaseUrl: URL;
  cookieSecret: string;
}

export interface MobileAuthDependencies {
  store: MobileAuthHandoffStore;
  now: () => Date;
}

export async function handleMobileGoogleStart(request: Request): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'GET') return methodNotAllowed(['GET'], id);

  try {
    const config = configuredMobileAuth();
    const challenge = new URL(request.url).searchParams.get('code_challenge') ?? '';
    if (!PKCE_CHALLENGE_PATTERN.test(challenge)) {
      throw new HttpError(400, 'Invalid mobile sign-in challenge.');
    }

    const callbackUrl = new URL('/api/v1/account/auth/mobile/callback', config.publicBaseUrl);

    const proxyRequest = new Request(
      new URL('/api/v1/account/auth/sign-in/social', config.publicBaseUrl),
      {
        method: 'POST',
        headers: {
          accept: 'application/json',
          'content-type': 'application/json',
          origin: config.publicBaseUrl.origin,
          'user-agent': request.headers.get('user-agent') ?? 'WakeMyWay-Mobile-OAuth',
        },
        body: JSON.stringify({
          provider: 'google',
          callbackURL: callbackUrl.toString(),
          newUserCallbackURL: callbackUrl.toString(),
          errorCallbackURL: callbackUrl.toString(),
          disableRedirect: true,
        }),
      },
    );

    const authResponse = await handleAuthProxyRequest({
      request: proxyRequest,
      path: 'sign-in/social',
      baseUrl: config.neonAuthBaseUrl,
      cookieSecret: config.cookieSecret,
      sameSite: 'lax',
    });

    if (!authResponse.ok) {
      throw new HttpError(502, 'Google sign-in could not be started.');
    }

    const payload = await authResponse.json() as { url?: unknown };
    if (typeof payload.url !== 'string' || !payload.url.startsWith('https://')) {
      throw new HttpError(502, 'Google sign-in could not be started.');
    }

    const headers = new Headers({
      location: payload.url,
      'cache-control': 'no-store',
      'x-request-id': id,
    });
    for (const cookie of getSetCookies(authResponse.headers)) headers.append('set-cookie', cookie);
    headers.append('set-cookie', pkceCookie(challenge));
    return new Response(null, { status: 302, headers });
  } catch (error) {
    return errorResponse(error, id, 'mobile-google-auth-start');
  }
}

export async function handleMobileGoogleCallback(
  request: Request,
  suppliedDependencies?: Partial<MobileAuthDependencies>,
): Promise<Response> {
  if (request.method !== 'GET') return mobileAuthRedirect('method_not_allowed');

  try {
    const config = configuredMobileAuth();
    const url = new URL(request.url);
    const challenge = readCookie(request, PKCE_COOKIE_NAME) ?? '';
    if (!PKCE_CHALLENGE_PATTERN.test(challenge)) {
      return mobileAuthRedirect('invalid_challenge');
    }

    const result = await processAuthMiddleware({
      request,
      pathname: url.pathname,
      skipRoutes: [url.pathname],
      loginUrl: url.pathname,
      baseUrl: config.neonAuthBaseUrl,
      cookieSecret: config.cookieSecret,
      sameSite: 'lax',
      logLevel: 'warn',
    });

    if (result.action !== 'redirect_oauth') {
      return mobileAuthRedirect('session_exchange_failed');
    }

    const sessionToken = extractSessionToken(result.cookies);
    if (!sessionToken) return mobileAuthRedirect('session_missing');

    const code = randomBase64Url();
    const now = (suppliedDependencies?.now ?? (() => new Date()))();
    const store = suppliedDependencies?.store ?? new PostgresMobileAuthHandoffStore();
    await store.put({
      codeHash: sha256Hex(code),
      pkceChallenge: challenge,
      tokenEnvelope: encryptSessionToken(sessionToken, config.cookieSecret),
      expiresAt: new Date(now.getTime() + HANDOFF_TTL_MS),
      createdAt: now,
    });

    const headers = new Headers({
      location: mobileAuthDeepLink({ code }).toString(),
      'cache-control': 'no-store',
    });
    for (const cookie of result.cookies) headers.append('set-cookie', cookie);
    headers.append('set-cookie', clearPkceCookie());
    return new Response(null, { status: 302, headers });
  } catch (error) {
    console.error('[wmw-cloud]', {
      operation: 'mobile-google-auth-callback',
      errorType: error instanceof Error ? error.name : typeof error,
    });
    return mobileAuthRedirect('sign_in_failed');
  }
}

export async function handleMobileAuthExchange(
  request: Request,
  suppliedDependencies?: Partial<MobileAuthDependencies>,
): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

  try {
    const config = configuredMobileAuth();
    const body = await parseJson(request, exchangeSchema, 8 * 1024);
    const challenge = pkceChallenge(body.verifier);
    const store = suppliedDependencies?.store ?? new PostgresMobileAuthHandoffStore();
    const now = (suppliedDependencies?.now ?? (() => new Date()))();
    const handoff = await store.consume(sha256Hex(body.code), challenge, now);
    if (!handoff) throw new HttpError(401, 'Invalid or expired sign-in handoff.');

    const sessionToken = decryptSessionToken(handoff.tokenEnvelope, config.cookieSecret);
    return json({ sessionToken }, 200, id);
  } catch (error) {
    return errorResponse(error, id, 'mobile-auth-exchange');
  }
}

function configuredMobileAuth(): MobileAuthConfig {
  const neonAuthBaseUrl = process.env.NEON_AUTH_BASE_URL?.trim().replace(/\/+$/, '');
  const cookieSecret = process.env.NEON_AUTH_COOKIE_SECRET?.trim();
  const publicBase = process.env.ACCOUNT_PUBLIC_BASE_URL?.trim();

  if (!neonAuthBaseUrl || !cookieSecret || !publicBase) {
    throw new HttpError(503, 'Mobile account sign-in is not configured.');
  }
  if (cookieSecret.length < 32) {
    throw new HttpError(503, 'Mobile account sign-in is not configured correctly.');
  }

  let publicBaseUrl: URL;
  try {
    publicBaseUrl = new URL(publicBase);
  } catch {
    throw new HttpError(503, 'Mobile account sign-in is not configured correctly.');
  }
  if (publicBaseUrl.protocol !== 'https:') {
    throw new HttpError(503, 'Mobile account sign-in is not configured correctly.');
  }
  publicBaseUrl.pathname = '/';
  publicBaseUrl.search = '';
  publicBaseUrl.hash = '';

  return { neonAuthBaseUrl, publicBaseUrl, cookieSecret };
}

function pkceCookie(challenge: string): string {
  return [
    `${PKCE_COOKIE_NAME}=${challenge}`,
    'Path=/api/v1/account/auth/mobile',
    'HttpOnly',
    'Secure',
    'SameSite=Lax',
    'Max-Age=600',
  ].join('; ');
}

function clearPkceCookie(): string {
  return [
    `${PKCE_COOKIE_NAME}=`,
    'Path=/api/v1/account/auth/mobile',
    'HttpOnly',
    'Secure',
    'SameSite=Lax',
    'Max-Age=0',
  ].join('; ');
}

function readCookie(request: Request, name: string): string | null {
  const header = request.headers.get('cookie');
  if (!header) return null;
  for (const part of header.split(';')) {
    const trimmed = part.trim();
    if (!trimmed.startsWith(`${name}=`)) continue;
    return trimmed.slice(name.length + 1);
  }
  return null;
}

function extractSessionToken(cookieHeaders: string[]): string | null {
  for (const header of cookieHeaders) {
    for (const cookie of parseSetCookies(header)) {
      if (cookie.name === NEON_AUTH_SESSION_COOKIE_NAME && cookie.value) return cookie.value;
    }
  }
  return null;
}

function getSetCookies(headers: Headers): string[] {
  const candidate = headers as Headers & { getSetCookie?: () => string[] };
  return candidate.getSetCookie?.() ?? [];
}

function mobileAuthRedirect(error: string): Response {
  return new Response(null, {
    status: 302,
    headers: {
      location: mobileAuthDeepLink({ error }).toString(),
      'cache-control': 'no-store',
    },
  });
}

function mobileAuthDeepLink(params: { code?: string; error?: string }): URL {
  const url = new URL('wakemyway://auth');
  if (params.code) url.searchParams.set('code', params.code);
  if (params.error) url.searchParams.set('error', params.error);
  return url;
}
