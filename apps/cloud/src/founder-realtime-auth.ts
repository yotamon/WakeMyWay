import { createHmac } from 'node:crypto';
import { z } from 'zod';

import { getAIConfig, parseAIConfig } from './ai/config';
import { HttpError, isInternallyAuthorized, secureEqual } from './http';
import { parseDirectOpenAiRealtimeConfig } from './voice-spike/direct-openai';

const FOUNDER_SCOPE = 'founder-realtime-wake';
const DEVICE_TOKEN_VERSION = 1;
const DEVICE_TOKEN_TTL_SECONDS = 90 * 24 * 60 * 60;
const MAX_FUTURE_SKEW_SECONDS = 5 * 60;
export const FOUNDER_PAIRING_CODE_MIN_LENGTH = 24;

const pairingInputSchema = z.object({
  code: z.string().trim().min(FOUNDER_PAIRING_CODE_MIN_LENGTH).max(128),
  installationId: z.string().uuid(),
});

const tokenPayloadSchema = z.object({
  v: z.literal(DEVICE_TOKEN_VERSION),
  scope: z.literal(FOUNDER_SCOPE),
  sub: z.string().uuid(),
  iat: z.number().int().positive(),
  exp: z.number().int().positive(),
});

type TokenPayload = z.infer<typeof tokenPayloadSchema>;

export interface FounderRealtimeSetupStatus {
  available: boolean;
  missing: string[];
}

export interface FounderPairingResult {
  deviceToken: string;
  expiresAt: number;
}

function pairingCode(environment: NodeJS.ProcessEnv): string | undefined {
  const value = environment.WMW_FOUNDER_PAIRING_CODE?.trim();
  return value && value.length >= FOUNDER_PAIRING_CODE_MIN_LENGTH ? value : undefined;
}

export function founderRealtimeSetupStatus(
  environment: NodeJS.ProcessEnv = process.env,
): FounderRealtimeSetupStatus {
  const realtime = parseDirectOpenAiRealtimeConfig(environment);
  const ai = parseAIConfig(environment);
  const missing: string[] = [];

  if (!realtime.configured) missing.push('OpenAI API key');
  if (!realtime.founderDogfoodEnabled) missing.push('founder Realtime gate');
  if (!realtime.safetyIdentifier) missing.push('OpenAI safety identifier');
  if (!ai.internalApiKey) missing.push('server signing key');
  if (!pairingCode(environment)) missing.push('founder access code');

  return { available: missing.length === 0, missing };
}

function requireSigningKey(environment: NodeJS.ProcessEnv): string {
  const key = parseAIConfig(environment).internalApiKey;
  if (!key) throw new HttpError(503, 'Founder Realtime server signing is not configured.');
  return key;
}

function requirePairingCode(environment: NodeJS.ProcessEnv): string {
  const code = pairingCode(environment);
  if (!code) throw new HttpError(503, 'Founder Realtime pairing is not configured.');
  return code;
}

export function pairFounderInstallation(
  input: unknown,
  options: { environment?: NodeJS.ProcessEnv; nowSeconds?: number } = {},
): FounderPairingResult {
  const environment = options.environment ?? process.env;
  const status = founderRealtimeSetupStatus(environment);
  if (!status.available) throw new HttpError(503, 'Conversational Alfred is not configured on the server yet.');

  const parsed = pairingInputSchema.safeParse(input);
  if (!parsed.success) throw new HttpError(400, 'Pairing request is invalid.');

  const expectedCode = requirePairingCode(environment);
  if (!secureEqual(parsed.data.code, expectedCode)) {
    throw new HttpError(401, 'Founder access code was not accepted.');
  }

  const now = options.nowSeconds ?? Math.floor(Date.now() / 1000);
  const payload: TokenPayload = {
    v: DEVICE_TOKEN_VERSION,
    scope: FOUNDER_SCOPE,
    sub: parsed.data.installationId,
    iat: now,
    exp: now + DEVICE_TOKEN_TTL_SECONDS,
  };

  return {
    deviceToken: signPayload(payload, requireSigningKey(environment)),
    expiresAt: payload.exp,
  };
}

export function verifyFounderDeviceToken(
  token: string,
  options: { environment?: NodeJS.ProcessEnv; nowSeconds?: number } = {},
): TokenPayload {
  const environment = options.environment ?? process.env;
  const signingKey = requireSigningKey(environment);
  const parts = token.trim().split('.');
  if (parts.length !== 2 || !parts[0] || !parts[1]) throw new HttpError(401, 'Unauthorized.');

  const [body, suppliedSignature] = parts;
  const expectedSignature = signatureFor(body, signingKey);
  if (!secureEqual(suppliedSignature, expectedSignature)) throw new HttpError(401, 'Unauthorized.');

  let decoded: unknown;
  try {
    decoded = JSON.parse(Buffer.from(body, 'base64url').toString('utf8'));
  } catch {
    throw new HttpError(401, 'Unauthorized.');
  }
  const parsed = tokenPayloadSchema.safeParse(decoded);
  if (!parsed.success) throw new HttpError(401, 'Unauthorized.');

  const now = options.nowSeconds ?? Math.floor(Date.now() / 1000);
  if (parsed.data.iat > now + MAX_FUTURE_SKEW_SECONDS || parsed.data.exp <= now) {
    throw new HttpError(401, 'Founder device credential expired.');
  }
  if (parsed.data.exp - parsed.data.iat > DEVICE_TOKEN_TTL_SECONDS + MAX_FUTURE_SKEW_SECONDS) {
    throw new HttpError(401, 'Unauthorized.');
  }

  return parsed.data;
}

export function requireFounderRealtimeAuthorization(
  request: Request,
  environment: NodeJS.ProcessEnv = process.env,
): void {
  const internalKey = parseAIConfig(environment).internalApiKey;
  if (internalKey && isInternallyAuthorized(request, internalKey)) return;

  const header = request.headers.get('authorization')?.trim();
  const match = header ? /^Bearer\s+(.+)$/i.exec(header) : null;
  if (!match?.[1]) throw new HttpError(401, 'Unauthorized.');
  verifyFounderDeviceToken(match[1], { environment });
}

function signPayload(payload: TokenPayload, signingKey: string): string {
  const body = Buffer.from(JSON.stringify(payload), 'utf8').toString('base64url');
  return `${body}.${signatureFor(body, signingKey)}`;
}

function signatureFor(body: string, signingKey: string): string {
  return createHmac('sha256', signingKey).update(body).digest('base64url');
}

// Keep a direct reference so bundlers/type-checkers preserve the same authoritative environment
// schema used by existing internal authentication.
void getAIConfig;
