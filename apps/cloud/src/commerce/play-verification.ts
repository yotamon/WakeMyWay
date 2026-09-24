import { importPKCS8, SignJWT } from 'jose';
import { z } from 'zod';

import { HttpError } from '../http.js';

const ANDROID_PUBLISHER_SCOPE = 'https://www.googleapis.com/auth/androidpublisher';
const GOOGLE_TOKEN_URL = 'https://oauth2.googleapis.com/token';
const GOOGLE_PUBLISHER_BASE = 'https://androidpublisher.googleapis.com/androidpublisher/v3';
const MAX_PRODUCT_ID_LENGTH = 128;
const MAX_PURCHASE_TOKEN_LENGTH = 4096;
const ACCESS_TOKEN_SKEW_MS = 60_000;

const verifyRequestSchema = z.object({
  productId: z.string().trim().min(1).max(MAX_PRODUCT_ID_LENGTH),
  purchaseToken: z.string().trim().min(16).max(MAX_PURCHASE_TOKEN_LENGTH),
}).strict();

const googleTokenSchema = z.object({
  access_token: z.string().min(1),
  expires_in: z.number().int().positive(),
  token_type: z.string().min(1),
});

const googleSubscriptionSchema = z.object({
  subscriptionState: z.string().min(1),
  acknowledgementState: z.string().optional(),
  linkedPurchaseToken: z.string().min(1).optional(),
  lineItems: z.array(
    z.object({
      productId: z.string().min(1),
      expiryTime: z.string().optional(),
    }).passthrough(),
  ).min(1),
}).passthrough();

export const playVerificationRequestSchema = verifyRequestSchema;

export type PlayEntitlementState =
  | 'ACTIVE'
  | 'CANCELLED_ENTITLED'
  | 'GRACE_PERIOD'
  | 'ON_HOLD'
  | 'PAUSED'
  | 'EXPIRED'
  | 'UNKNOWN';

export interface VerifiedPlaySubscription {
  productId: string;
  entitlement: PlayEntitlementState;
  acknowledged: boolean;
  expiryAt?: string;
  linkedPurchaseToken?: string;
}

export interface PlayVerificationConfig {
  enabled: boolean;
  packageName?: string;
  allowedProductIds: Set<string>;
  serviceAccountEmail?: string;
  serviceAccountPrivateKey?: string;
}

export interface PlayVerificationDependencies {
  fetch: typeof fetch;
  now: () => Date;
  accessToken?: () => Promise<string>;
}

interface CachedAccessToken {
  cacheKey: string;
  value: string;
  expiresAtMs: number;
}

let cachedAccessToken: CachedAccessToken | undefined;

export function parsePlayVerificationConfig(
  environment: NodeJS.ProcessEnv = process.env,
): PlayVerificationConfig {
  const enabled = environment.WMW_PLAY_VERIFICATION_ENABLED?.trim().toLowerCase() === 'true';
  const packageName = nonBlank(environment.WMW_PLAY_PACKAGE_NAME);
  const serviceAccountEmail = nonBlank(environment.GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL);
  const serviceAccountPrivateKey = nonBlank(environment.GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY)
    ?.replace(/\\n/g, '\n');
  const allowedProductIds = new Set(
    (environment.WMW_PLAY_SUBSCRIPTION_PRODUCT_IDS ?? '')
      .split(',')
      .map(value => value.trim())
      .filter(Boolean),
  );

  return {
    enabled,
    allowedProductIds,
    ...(packageName ? { packageName } : {}),
    ...(serviceAccountEmail ? { serviceAccountEmail } : {}),
    ...(serviceAccountPrivateKey ? { serviceAccountPrivateKey } : {}),
  };
}

export function requirePlayVerificationConfig(
  environment: NodeJS.ProcessEnv = process.env,
): PlayVerificationConfig & {
  enabled: true;
  packageName: string;
  serviceAccountEmail: string;
  serviceAccountPrivateKey: string;
} {
  const config = parsePlayVerificationConfig(environment);
  if (!config.enabled) {
    throw new HttpError(503, 'Play purchase verification is not enabled.');
  }
  if (
    !config.packageName ||
    config.allowedProductIds.size === 0 ||
    !config.serviceAccountEmail ||
    !config.serviceAccountPrivateKey
  ) {
    throw new HttpError(503, 'Play purchase verification is not configured.');
  }
  if (!/^[A-Za-z0-9._]+(?:\.[A-Za-z0-9._]+)*$/.test(config.packageName)) {
    throw new HttpError(503, 'Play purchase verification is not configured correctly.');
  }

  return {
    ...config,
    enabled: true,
    packageName: config.packageName,
    serviceAccountEmail: config.serviceAccountEmail,
    serviceAccountPrivateKey: config.serviceAccountPrivateKey,
  };
}

export async function verifyPlaySubscription(
  input: z.output<typeof verifyRequestSchema>,
  config: ReturnType<typeof requirePlayVerificationConfig>,
  suppliedDependencies: Partial<PlayVerificationDependencies> = {},
): Promise<VerifiedPlaySubscription> {
  if (!config.allowedProductIds.has(input.productId)) {
    throw new HttpError(400, 'Subscription product is not supported.');
  }

  const fetchImpl = suppliedDependencies.fetch ?? fetch;
  const now = suppliedDependencies.now ?? (() => new Date());
  const accessToken = suppliedDependencies.accessToken
    ? await suppliedDependencies.accessToken()
    : await configuredGoogleAccessToken(config, fetchImpl, now);

  const url =
    `${GOOGLE_PUBLISHER_BASE}/applications/${encodeURIComponent(config.packageName)}` +
    `/purchases/subscriptionsv2/tokens/${encodeURIComponent(input.purchaseToken)}`;
  const response = await fetchImpl(url, {
    method: 'GET',
    headers: {
      authorization: `Bearer ${accessToken}`,
      accept: 'application/json',
    },
  });

  if (response.status === 404) {
    throw new HttpError(401, 'Purchase could not be verified.');
  }
  if (response.status === 401 || response.status === 403) {
    throw new HttpError(503, 'Play purchase verification is temporarily unavailable.');
  }
  if (!response.ok) {
    throw new HttpError(503, 'Play purchase verification is temporarily unavailable.');
  }

  const raw: unknown = await response.json();
  const parsed = googleSubscriptionSchema.safeParse(raw);
  if (!parsed.success) {
    throw new HttpError(503, 'Play purchase verification returned an unsupported response.');
  }

  const productMatches = parsed.data.lineItems.some(
    lineItem => lineItem.productId === input.productId,
  );
  if (!productMatches) {
    throw new HttpError(401, 'Purchase could not be verified.');
  }

  const entitlement = mapSubscriptionState(parsed.data.subscriptionState);
  const matchingLineItems = parsed.data.lineItems.filter(
    lineItem => lineItem.productId === input.productId,
  );
  const expiryAt = matchingLineItems
    .map(lineItem => lineItem.expiryTime)
    .filter((value): value is string => Boolean(value))
    .sort()
    .at(-1);

  let acknowledged =
    parsed.data.acknowledgementState === 'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED';

  if (!acknowledged && grantsPaidAccess(entitlement)) {
    acknowledged = await acknowledgePlaySubscription(
      input,
      config,
      accessToken,
      fetchImpl,
    );
  }

  return {
    productId: input.productId,
    entitlement,
    acknowledged,
    ...(expiryAt ? { expiryAt } : {}),
    ...(parsed.data.linkedPurchaseToken
      ? { linkedPurchaseToken: parsed.data.linkedPurchaseToken }
      : {}),
  };
}

async function acknowledgePlaySubscription(
  input: z.output<typeof verifyRequestSchema>,
  config: ReturnType<typeof requirePlayVerificationConfig>,
  accessToken: string,
  fetchImpl: typeof fetch,
): Promise<boolean> {
  const url =
    `${GOOGLE_PUBLISHER_BASE}/applications/${encodeURIComponent(config.packageName)}` +
    `/purchases/subscriptions/${encodeURIComponent(input.productId)}/tokens/` +
    `${encodeURIComponent(input.purchaseToken)}:acknowledge`;

  try {
    const response = await fetchImpl(url, {
      method: 'POST',
      headers: {
        authorization: `Bearer ${accessToken}`,
        accept: 'application/json',
        'content-type': 'application/json',
      },
      body: '{}',
    });
    return response.ok;
  } catch {
    return false;
  }
}

export function grantsPaidAccess(entitlement: PlayEntitlementState): boolean {
  return (
    entitlement === 'ACTIVE' ||
    entitlement === 'CANCELLED_ENTITLED' ||
    entitlement === 'GRACE_PERIOD'
  );
}

export function mapSubscriptionState(state: string): PlayEntitlementState {
  switch (state) {
    case 'SUBSCRIPTION_STATE_ACTIVE':
      return 'ACTIVE';
    case 'SUBSCRIPTION_STATE_CANCELED':
      return 'CANCELLED_ENTITLED';
    case 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD':
      return 'GRACE_PERIOD';
    case 'SUBSCRIPTION_STATE_ON_HOLD':
      return 'ON_HOLD';
    case 'SUBSCRIPTION_STATE_PAUSED':
      return 'PAUSED';
    case 'SUBSCRIPTION_STATE_EXPIRED':
      return 'EXPIRED';
    default:
      return 'UNKNOWN';
  }
}

async function configuredGoogleAccessToken(
  config: ReturnType<typeof requirePlayVerificationConfig>,
  fetchImpl: typeof fetch,
  now: () => Date,
): Promise<string> {
  const nowMs = now().getTime();
  const cacheKey = `${config.serviceAccountEmail}|${config.packageName}`;
  if (
    cachedAccessToken &&
    cachedAccessToken.cacheKey === cacheKey &&
    cachedAccessToken.expiresAtMs - ACCESS_TOKEN_SKEW_MS > nowMs
  ) {
    return cachedAccessToken.value;
  }

  const issuedAtSeconds = Math.floor(nowMs / 1000);
  const key = await importPKCS8(config.serviceAccountPrivateKey, 'RS256');
  const assertion = await new SignJWT({
    scope: ANDROID_PUBLISHER_SCOPE,
  })
    .setProtectedHeader({ alg: 'RS256', typ: 'JWT' })
    .setIssuer(config.serviceAccountEmail)
    .setAudience(GOOGLE_TOKEN_URL)
    .setIssuedAt(issuedAtSeconds)
    .setExpirationTime(issuedAtSeconds + 3600)
    .sign(key);

  const response = await fetchImpl(GOOGLE_TOKEN_URL, {
    method: 'POST',
    headers: {
      'content-type': 'application/x-www-form-urlencoded',
      accept: 'application/json',
    },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion,
    }),
  });
  if (!response.ok) {
    throw new HttpError(503, 'Play purchase verification is temporarily unavailable.');
  }

  const raw: unknown = await response.json();
  const parsed = googleTokenSchema.safeParse(raw);
  if (!parsed.success) {
    throw new HttpError(503, 'Play purchase verification is temporarily unavailable.');
  }

  cachedAccessToken = {
    cacheKey,
    value: parsed.data.access_token,
    expiresAtMs: nowMs + parsed.data.expires_in * 1000,
  };
  return parsed.data.access_token;
}

function nonBlank(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}
