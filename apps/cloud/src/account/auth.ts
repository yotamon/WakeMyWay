import { createRemoteJWKSet, errors, jwtVerify } from 'jose';

import { HttpError } from '../http.js';

export interface AccountIdentity {
  userId: string;
  email?: string;
}

export type AccountTokenVerifier = (accessToken: string) => Promise<AccountIdentity>;

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

let cachedSupabaseUrl: string | undefined;
let cachedVerifier: AccountTokenVerifier | undefined;

export function bearerAccessToken(request: Request): string | undefined {
  const header = request.headers.get('authorization')?.trim();
  if (!header) return undefined;
  const match = /^Bearer\s+([^\s]+)$/i.exec(header);
  return match?.[1];
}

export async function requireAccountIdentity(
  request: Request,
  verifier: AccountTokenVerifier = configuredAccountTokenVerifier(),
): Promise<AccountIdentity> {
  const token = bearerAccessToken(request);
  if (!token) throw new HttpError(401, 'Unauthorized.');
  return verifier(token);
}

export function createSupabaseAccountTokenVerifier(supabaseUrl: string): AccountTokenVerifier {
  const baseUrl = normalizeSupabaseUrl(supabaseUrl);
  const issuer = `${baseUrl}/auth/v1`;
  const jwks = createRemoteJWKSet(new URL(`${issuer}/.well-known/jwks.json`));

  return async (accessToken: string): Promise<AccountIdentity> => {
    try {
      const { payload } = await jwtVerify(accessToken, jwks, {
        issuer,
        audience: 'authenticated',
      });
      const subject = payload.sub;
      if (!subject || !UUID_PATTERN.test(subject)) throw new HttpError(401, 'Unauthorized.');

      const email = typeof payload.email === 'string' && payload.email.length <= 320
        ? payload.email
        : undefined;
      return {
        userId: subject,
        ...(email ? { email } : {}),
      };
    } catch (error) {
      if (error instanceof HttpError) throw error;
      if (error instanceof errors.JWKSTimeout) {
        throw new HttpError(503, 'Account authentication is temporarily unavailable.');
      }
      if (error instanceof errors.JOSEError) throw new HttpError(401, 'Unauthorized.');
      throw new HttpError(503, 'Account authentication is temporarily unavailable.');
    }
  };
}

export function configuredAccountTokenVerifier(): AccountTokenVerifier {
  const configuredUrl = process.env.SUPABASE_URL?.trim();
  if (!configuredUrl) throw new HttpError(503, 'Account authentication is not configured.');

  if (cachedVerifier && cachedSupabaseUrl === configuredUrl) return cachedVerifier;
  cachedVerifier = createSupabaseAccountTokenVerifier(configuredUrl);
  cachedSupabaseUrl = configuredUrl;
  return cachedVerifier;
}

function normalizeSupabaseUrl(value: string): string {
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    throw new HttpError(503, 'Account authentication is not configured correctly.');
  }

  const localDevelopment = url.hostname === 'localhost' || url.hostname === '127.0.0.1';
  if (url.protocol !== 'https:' && !(localDevelopment && url.protocol === 'http:')) {
    throw new HttpError(503, 'Account authentication is not configured correctly.');
  }

  url.pathname = url.pathname.replace(/\/+$/, '');
  url.search = '';
  url.hash = '';
  return url.toString().replace(/\/$/, '');
}
