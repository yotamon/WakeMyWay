import { createRemoteJWKSet, jwtVerify } from 'jose';
import { z } from 'zod';

import { HttpError } from '../http.js';

const GOOGLE_OIDC_JWKS = createRemoteJWKSet(
  new URL('https://www.googleapis.com/oauth2/v3/certs'),
);
const MAX_AUTH_TOKEN_LENGTH = 16 * 1024;
const MAX_PUSH_DATA_CHARS = 32 * 1024;
const MAX_DECODED_NOTIFICATION_BYTES = 16 * 1024;

export const playRtdnPushSchema = z.object({
  message: z.object({
    data: z.string().min(1).max(MAX_PUSH_DATA_CHARS),
    messageId: z.string().trim().min(1).max(256),
  }).passthrough(),
  subscription: z.string().max(1024).optional(),
}).passthrough();

const developerNotificationSchema = z.object({
  version: z.string().min(1).max(16),
  packageName: z.string().min(1).max(256),
  eventTimeMillis: z.string().regex(/^\d{1,20}$/),
  subscriptionNotification: z.object({
    version: z.string().min(1).max(16),
    notificationType: z.number().int().min(1).max(100),
    purchaseToken: z.string().min(16).max(4096),
  }).passthrough().optional(),
  testNotification: z.object({
    version: z.string().min(1).max(16),
  }).passthrough().optional(),
}).passthrough();

export type PlayDeveloperNotification = z.output<typeof developerNotificationSchema>;

export interface PlayRtdnConfig {
  enabled: boolean;
  audience?: string;
  serviceAccountEmail?: string;
}

export type PlayRtdnAuthorizationVerifier = (
  request: Request,
  config: Required<Omit<PlayRtdnConfig, 'enabled'>> & { enabled: true },
) => Promise<void>;

export function parsePlayRtdnConfig(
  environment: NodeJS.ProcessEnv = process.env,
): PlayRtdnConfig {
  const enabled = environment.WMW_PLAY_RTDN_ENABLED?.trim().toLowerCase() === 'true';
  const audience = nonBlank(environment.WMW_PLAY_RTDN_AUDIENCE);
  const serviceAccountEmail = nonBlank(environment.WMW_PLAY_RTDN_SERVICE_ACCOUNT_EMAIL);

  return {
    enabled,
    ...(audience ? { audience } : {}),
    ...(serviceAccountEmail ? { serviceAccountEmail } : {}),
  };
}

export function requirePlayRtdnConfig(
  environment: NodeJS.ProcessEnv = process.env,
): Required<Omit<PlayRtdnConfig, 'enabled'>> & { enabled: true } {
  const config = parsePlayRtdnConfig(environment);
  if (!config.enabled) throw new HttpError(503, 'Play RTDN is not enabled.');
  if (!config.audience || !config.serviceAccountEmail) {
    throw new HttpError(503, 'Play RTDN is not configured.');
  }
  if (!config.audience.startsWith('https://')) {
    throw new HttpError(503, 'Play RTDN audience must be HTTPS.');
  }
  if (!/^[^@\s]+@[^@\s]+\.gserviceaccount\.com$/.test(config.serviceAccountEmail)) {
    throw new HttpError(503, 'Play RTDN service-account identity is invalid.');
  }

  return {
    enabled: true,
    audience: config.audience,
    serviceAccountEmail: config.serviceAccountEmail,
  };
}

export async function verifyPlayRtdnAuthorization(
  request: Request,
  config: ReturnType<typeof requirePlayRtdnConfig>,
): Promise<void> {
  const header = request.headers.get('authorization')?.trim();
  const match = /^Bearer\s+(.+)$/i.exec(header ?? '');
  const token = match?.[1];
  if (!token || token.length > MAX_AUTH_TOKEN_LENGTH) {
    throw new HttpError(401, 'Unauthorized.');
  }

  try {
    const { payload } = await jwtVerify(token, GOOGLE_OIDC_JWKS, {
      audience: config.audience,
      issuer: ['https://accounts.google.com', 'accounts.google.com'],
    });

    if (
      payload.email !== config.serviceAccountEmail ||
      payload.email_verified !== true
    ) {
      throw new HttpError(401, 'Unauthorized.');
    }
  } catch (error) {
    if (error instanceof HttpError) throw error;
    throw new HttpError(401, 'Unauthorized.');
  }
}

export function decodePlayRtdnNotification(
  push: z.output<typeof playRtdnPushSchema>,
): PlayDeveloperNotification {
  let bytes: Buffer;
  try {
    bytes = Buffer.from(push.message.data, 'base64');
  } catch {
    throw new HttpError(400, 'RTDN data is invalid.');
  }
  if (bytes.length === 0 || bytes.length > MAX_DECODED_NOTIFICATION_BYTES) {
    throw new HttpError(400, 'RTDN data is invalid.');
  }

  let decoded: unknown;
  try {
    decoded = JSON.parse(bytes.toString('utf8'));
  } catch {
    throw new HttpError(400, 'RTDN data is invalid.');
  }

  const parsed = developerNotificationSchema.safeParse(decoded);
  if (!parsed.success) throw new HttpError(400, 'RTDN data is invalid.');
  return parsed.data;
}

function nonBlank(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}
