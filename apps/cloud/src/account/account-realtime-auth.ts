import { createHash, createHmac, randomUUID, timingSafeEqual } from 'node:crypto';

import { HttpError } from '../http.js';

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const TOKEN_VERSION = 2;
const TOKEN_SCOPE = 'account-realtime-wake';
export const ACCOUNT_REALTIME_DEVICE_TTL_SECONDS = 14 * 24 * 60 * 60;
const SIGNING_KEY_MIN_LENGTH = 32;

interface AccountRealtimeTokenPayload {
  v: number;
  jti: string;
  sub: string;
  account: string;
  scope: typeof TOKEN_SCOPE;
  iat: number;
  exp: number;
}

export interface AccountRealtimeCredential {
  deviceToken: string;
  credentialId: string;
  expiresAt: number;
}

export interface VerifiedAccountRealtimeCredential {
  credentialId: string;
  installationId: string;
  accountPseudonym: string;
  expiresAt: number;
}

interface CredentialOptions {
  environment?: NodeJS.ProcessEnv;
  nowSeconds?: number;
  credentialId?: string;
}

export function issueAccountRealtimeCredential(
  userId: string,
  installationId: string,
  options: CredentialOptions = {},
): AccountRealtimeCredential {
  if (!UUID_PATTERN.test(userId) || !UUID_PATTERN.test(installationId)) {
    throw new HttpError(400, 'Realtime provisioning request is invalid.');
  }
  const credentialId = options.credentialId ?? randomUUID();
  if (!UUID_PATTERN.test(credentialId)) {
    throw new HttpError(500, 'Realtime credential id is invalid.');
  }

  const now = options.nowSeconds ?? Math.floor(Date.now() / 1000);
  const payload: AccountRealtimeTokenPayload = {
    v: TOKEN_VERSION,
    jti: credentialId,
    sub: installationId,
    account: accountRealtimePseudonym(userId),
    scope: TOKEN_SCOPE,
    iat: now,
    exp: now + ACCOUNT_REALTIME_DEVICE_TTL_SECONDS,
  };
  const encoded = Buffer.from(JSON.stringify(payload), 'utf8').toString('base64url');
  const signature = sign(encoded, options.environment ?? process.env);
  return {
    deviceToken: `${encoded}.${signature}`,
    credentialId,
    expiresAt: payload.exp,
  };
}

export function verifyAccountRealtimeCredential(
  token: string,
  options: CredentialOptions = {},
): VerifiedAccountRealtimeCredential {
  const [encoded, signature, ...extra] = token.trim().split('.');
  if (!encoded || !signature || extra.length > 0) throw new HttpError(401, 'Unauthorized.');

  const expected = sign(encoded, options.environment ?? process.env);
  const actualBuffer = Buffer.from(signature);
  const expectedBuffer = Buffer.from(expected);
  if (
    actualBuffer.length !== expectedBuffer.length ||
    !timingSafeEqual(actualBuffer, expectedBuffer)
  ) {
    throw new HttpError(401, 'Unauthorized.');
  }

  let payload: AccountRealtimeTokenPayload;
  try {
    payload = JSON.parse(Buffer.from(encoded, 'base64url').toString('utf8')) as AccountRealtimeTokenPayload;
  } catch {
    throw new HttpError(401, 'Unauthorized.');
  }

  const now = options.nowSeconds ?? Math.floor(Date.now() / 1000);
  if (
    payload.v !== TOKEN_VERSION ||
    payload.scope !== TOKEN_SCOPE ||
    !UUID_PATTERN.test(payload.jti) ||
    !UUID_PATTERN.test(payload.sub) ||
    !/^[a-f0-9]{40}$/.test(payload.account) ||
    !Number.isInteger(payload.iat) ||
    !Number.isInteger(payload.exp) ||
    payload.exp <= now ||
    payload.exp - payload.iat > ACCOUNT_REALTIME_DEVICE_TTL_SECONDS ||
    payload.iat > now + 60
  ) {
    throw new HttpError(401, 'Unauthorized.');
  }

  return {
    credentialId: payload.jti,
    installationId: payload.sub,
    accountPseudonym: payload.account,
    expiresAt: payload.exp,
  };
}

export function accountRealtimeSafetyIdentifier(
  credential: Pick<VerifiedAccountRealtimeCredential, 'installationId' | 'accountPseudonym'>,
): string {
  const digest = createHash('sha256')
    .update('wakemyway-account-realtime-safety-v1:')
    .update(credential.accountPseudonym)
    .update(':')
    .update(credential.installationId)
    .digest('hex');
  return `wmw_${digest.slice(0, 60)}`;
}

export function accountRealtimePseudonym(userId: string): string {
  return createHash('sha256')
    .update('wakemyway-account-v1:')
    .update(userId)
    .digest('hex')
    .slice(0, 40);
}

function sign(encodedPayload: string, environment: NodeJS.ProcessEnv): string {
  const key = environment.WMW_REALTIME_TOKEN_SIGNING_KEY?.trim()
    || environment.WMW_FOUNDER_TOKEN_SIGNING_KEY?.trim();
  if (!key || key.length < SIGNING_KEY_MIN_LENGTH) {
    throw new HttpError(503, 'Realtime device authorization is not configured.');
  }
  return createHmac('sha256', key).update(encodedPayload).digest('base64url');
}
