import {
  createCipheriv,
  createDecipheriv,
  createHash,
  randomBytes,
  timingSafeEqual,
} from 'node:crypto';
import {
  extractNeonAuthCookies,
  handleAuthProxyRequest,
  NEON_AUTH_SESSION_COOKIE_NAME,
  parseCookieValue,
  parseSetCookies,
  processAuthMiddleware,
} from '@neondatabase/auth/server';
import { z } from 'zod';

import {
  errorResponse,
  HttpError,
  json,
  methodNotAllowed,
  parseJson,
  requestId,
} from '../http.js';

const PKCE_CHALLENGE = /^[A-Za-z0-9_-]{43}$/;
const PKCE_VERIFIER = /^[A-Za-z0-9._~-]{43,128}$/;
const HANDOFF = /^[A-Za-z0-9_-]{32,8192}$/;
const HANDOFF_TTL_MS = 2 * 60 * 1000;

const exchangeSchema = z.object({
  handoff: z.string().min(32).max(8192).regex(HANDOFF),
  verifier: z.string().min(43).max(128).regex(PKCE_VERIFIER),
});

export interface MobileHandoffPayload {
  v: 1;
  sessionToken: string;
  userId: string;
  challenge: string;
  exp: number;
}

interface NeonSessionResponse {
  session?: { id?: string; token?: string };
  user?: { id?: string; email?: string };
}

interface SocialStartResponse {
  url?: string;
  redirect?: boolean;
  status?: boolean;
}

export async function handleMobileGoogleStart(request: Request): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'GET') return methodNotAllowed(['GET'], id);

  try {
    const url = new URL(request.url);
    requireHttps(url);
    const challenge = url.searchParams.get('challenge')?.trim() ?? '';
    if (!PKCE_CHALLENGE.test(challenge)) {
      throw new HttpError(400, 'The mobile sign-in challenge is invalid.');
    }

    const config = authProxyConfig();
    const callbackUrl = new URL('/api/v1/account/mobile-google-complete', url.origin);
    callbackUrl.searchParams.set('challenge', challenge);

    const upstreamRequest = new Request(
      new URL('/api/auth/sign-in/social', url.origin),
      {
        method: 'POST',
        headers: {
          accept: 'application/json',
          'content-type': 'application/json',
          origin: url.origin,
          cookie: request.headers.get('cookie') ?? '',
        },
        body: JSON.stringify({
          provider: 'google',
          callbackURL: callbackUrl.toString(),
          disableRedirect: true,
        }),
      },
    );

    const upstream = await handleAuthProxyRequest({
      request: upstreamRequest,
      path: 'sign-in/social',
      ...config,
    });
    if (!upstream.ok) return browserFailure('Google sign-in could not be started.', upstream.status);

    const body = (await upstream.json()) as SocialStartResponse;
    if (!body.url) throw new HttpError(502, 'Neon Auth did not return an OAuth authorization URL.');

    return redirectWithCookies(body.url, upstream.headers.getSetCookie());
  } catch (error) {
    return browserErrorResponse(error, id, 'account.mobile-google-start');
  }
}

export async function handleMobileGoogleComplete(request: Request): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'GET') return methodNotAllowed(['GET'], id);

  try {
    const url = new URL(request.url);
    requireHttps(url);
    const challenge = url.searchParams.get('challenge')?.trim() ?? '';
    if (!PKCE_CHALLENGE.test(challenge)) {
      throw new HttpError(400, 'The mobile sign-in challenge is invalid.');
    }

    const config = authProxyConfig();

    // Neon Auth's official framework adapters finalize OAuth in middleware.
    // The first callback includes neon_auth_session_verifier + the challenge
    // cookie. processAuthMiddleware exchanges that verifier for the real
    // session cookie and returns a redirect back to this same URL with the
    // verifier removed. We must preserve every Set-Cookie header on that
    // redirect. The browser then performs the second pass below with a valid
    // Neon session cookie.
    const middlewareResult = await processAuthMiddleware({
      request,
      pathname: url.pathname,
      skipRoutes: [url.pathname],
      loginUrl: '/api/v1/account/mobile-google-start',
      baseUrl: config.baseUrl,
      cookieSecret: config.cookieSecret,
      sessionDataTtl: config.sessionDataTtl,
      sameSite: config.sameSite,
    });

    if (middlewareResult.action === 'redirect_oauth') {
      return redirectWithCookies(
        middlewareResult.redirectUrl.toString(),
        middlewareResult.cookies,
      );
    }
    if (middlewareResult.action === 'redirect_login') {
      return browserFailure('Google sign-in could not create a Neon session.', 401);
    }

    const sessionRequest = new Request(request.url, {
      method: 'GET',
      headers: request.headers,
    });
    const sessionResponse = await handleAuthProxyRequest({
      request: sessionRequest,
      path: 'get-session',
      ...config,
    });
    if (!sessionResponse.ok) {
      return browserFailure('Google sign-in could not create a Neon session.', sessionResponse.status);
    }

    const sessionData = (await sessionResponse.json()) as NeonSessionResponse;
    const userId = sessionData.user?.id;
    if (!userId || !isUuid(userId)) {
      throw new HttpError(502, 'Neon Auth did not return an authenticated user.');
    }

    const sessionToken =
      parseCookieValue(
        extractNeonAuthCookies(request.headers),
        NEON_AUTH_SESSION_COOKIE_NAME,
      ) ?? sessionTokenFromSetCookies(sessionResponse.headers.getSetCookie());
    if (!sessionToken) {
      throw new HttpError(502, 'Neon Auth did not return a session token.');
    }

    const handoff = sealMobileHandoff({
      v: 1,
      sessionToken,
      userId,
      challenge,
      exp: Date.now() + HANDOFF_TTL_MS,
    });

    const appUrl = new URL('wakemyway://auth');
    appUrl.searchParams.set('handoff', handoff);
    return redirectWithCookies(appUrl.toString(), sessionResponse.headers.getSetCookie());
  } catch (error) {
    return browserErrorResponse(error, id, 'account.mobile-google-complete');
  }
}

export async function handleMobileGoogleExchange(request: Request): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

  try {
    const { handoff, verifier } = await parseJson(request, exchangeSchema, 12 * 1024);
    const payload = openMobileHandoff(handoff);
    if (payload.exp < Date.now()) throw new HttpError(401, 'The mobile sign-in handoff expired.');

    const challenge = mobilePkceChallenge(verifier);
    if (!safeEqual(challenge, payload.challenge)) {
      throw new HttpError(401, 'The mobile sign-in verifier is invalid.');
    }

    return json(
      {
        sessionToken: payload.sessionToken,
        userId: payload.userId,
      },
      200,
      id,
    );
  } catch (error) {
    return errorResponse(error, id, 'account.mobile-google-exchange');
  }
}

function authProxyConfig() {
  const baseUrl = process.env.NEON_AUTH_BASE_URL?.trim().replace(/\/+$/, '');
  const cookieSecret = process.env.NEON_AUTH_COOKIE_SECRET?.trim();
  if (!baseUrl) throw new HttpError(503, 'Account authentication is not configured.');
  if (!cookieSecret || cookieSecret.length < 32) {
    throw new HttpError(503, 'Account authentication cookies are not configured.');
  }
  return {
    baseUrl,
    cookieSecret,
    sessionDataTtl: 300,
    sameSite: 'lax' as const,
  };
}

export function sealMobileHandoff(payload: MobileHandoffPayload): string {
  const iv = randomBytes(12);
  const cipher = createCipheriv('aes-256-gcm', handoffKey(), iv);
  const plaintext = Buffer.from(JSON.stringify(payload), 'utf8');
  const ciphertext = Buffer.concat([cipher.update(plaintext), cipher.final()]);
  const tag = cipher.getAuthTag();
  return Buffer.concat([iv, tag, ciphertext]).toString('base64url');
}

export function openMobileHandoff(value: string): MobileHandoffPayload {
  let bytes: Buffer;
  try {
    bytes = Buffer.from(value, 'base64url');
  } catch {
    throw new HttpError(400, 'The mobile sign-in handoff is malformed.');
  }
  if (bytes.length < 12 + 16 + 2) throw new HttpError(400, 'The mobile sign-in handoff is malformed.');

  try {
    const iv = bytes.subarray(0, 12);
    const tag = bytes.subarray(12, 28);
    const ciphertext = bytes.subarray(28);
    const decipher = createDecipheriv('aes-256-gcm', handoffKey(), iv);
    decipher.setAuthTag(tag);
    const plaintext = Buffer.concat([decipher.update(ciphertext), decipher.final()]).toString('utf8');
    const decoded = JSON.parse(plaintext) as Partial<MobileHandoffPayload>;
    if (
      decoded.v !== 1 ||
      typeof decoded.sessionToken !== 'string' ||
      decoded.sessionToken.length < 16 ||
      typeof decoded.userId !== 'string' ||
      !isUuid(decoded.userId) ||
      typeof decoded.challenge !== 'string' ||
      !PKCE_CHALLENGE.test(decoded.challenge) ||
      typeof decoded.exp !== 'number'
    ) {
      throw new Error('invalid payload');
    }
    return decoded as MobileHandoffPayload;
  } catch {
    throw new HttpError(401, 'The mobile sign-in handoff is invalid.');
  }
}

function handoffKey(): Buffer {
  const secret = process.env.NEON_AUTH_COOKIE_SECRET?.trim();
  if (!secret || secret.length < 32) {
    throw new HttpError(503, 'Account authentication cookies are not configured.');
  }
  return createHash('sha256')
    .update('WakeMyWay mobile auth handoff v1\0', 'utf8')
    .update(secret, 'utf8')
    .digest();
}

export function mobilePkceChallenge(verifier: string): string {
  return createHash('sha256').update(verifier, 'ascii').digest('base64url');
}

function safeEqual(left: string, right: string): boolean {
  const leftBytes = Buffer.from(left, 'utf8');
  const rightBytes = Buffer.from(right, 'utf8');
  return leftBytes.length === rightBytes.length && timingSafeEqual(leftBytes, rightBytes);
}

function sessionTokenFromSetCookies(headers: string[]): string | null {
  for (const header of headers) {
    for (const cookie of parseSetCookies(header)) {
      if (cookie.name === NEON_AUTH_SESSION_COOKIE_NAME) return cookie.value;
    }
  }
  return null;
}

function redirectWithCookies(location: string, cookies: string[] = []): Response {
  const headers = new Headers({
    location,
    'cache-control': 'no-store',
  });
  for (const cookie of cookies) headers.append('set-cookie', cookie);
  return new Response(null, { status: 302, headers });
}

function browserFailure(message: string, status = 400): Response {
  return new Response(
    `<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1"><title>WakeMyWay sign-in</title><main style="font-family:system-ui;padding:32px;max-width:560px;margin:auto"><h1>WakeMyWay</h1><p>${escapeHtml(message)}</p><p>You can close this tab and return to the app.</p></main>`,
    {
      status,
      headers: {
        'content-type': 'text/html; charset=utf-8',
        'cache-control': 'no-store',
      },
    },
  );
}

function browserErrorResponse(error: unknown, id: string, operation: string): Response {
  if (error instanceof HttpError) return browserFailure(error.message, error.status);
  const response = errorResponse(error, id, operation);
  return browserFailure('Sign-in failed. Return to WakeMyWay and try again.', response.status);
}

function requireHttps(url: URL): void {
  if (url.protocol !== 'https:') throw new HttpError(400, 'Secure HTTPS is required for sign-in.');
}

function isUuid(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, character => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  })[character] ?? character);
}
