import { z } from 'zod';

import { HttpError } from '../http';

const OPENAI_CLIENT_SECRETS_URL = 'https://api.openai.com/v1/realtime/client_secrets';
export const OPENAI_REALTIME_CALLS_URL = 'https://api.openai.com/v1/realtime/calls';
export const DIRECT_OPENAI_CONFIGURATION_ID = 'direct-openai:webrtc-ephemeral-v1';
export const FOUNDER_WAKE_CONFIGURATION_ID = 'direct-openai:webrtc-founder-wake-v1';

const booleanStringSchema = z
  .enum(['true', 'false'])
  .default('false')
  .transform(value => value === 'true');

const environmentSchema = z.object({
  OPENAI_API_KEY: z.string().trim().min(1).optional(),
  WMW_ENABLE_DIRECT_OPENAI_REALTIME_SPIKE: booleanStringSchema,
  WMW_ENABLE_FOUNDER_REALTIME_DOGFOOD: booleanStringSchema,
  WMW_OPENAI_REALTIME_MODEL: z.string().trim().min(1).default('gpt-realtime-2.1'),
  WMW_OPENAI_REALTIME_VOICE: z.string().trim().min(1).max(128).default('marin'),
  WMW_OPENAI_SAFETY_IDENTIFIER: z.string().trim().min(8).max(256).optional(),
});

const clientSecretResponseSchema = z
  .object({
    value: z.string().trim().min(1),
    expires_at: z.number().int().positive().optional(),
  })
  .passthrough();

export interface DirectOpenAiRealtimeConfig {
  configured: boolean;
  enabled: boolean;
  founderDogfoodEnabled: boolean;
  apiKey?: string;
  model: string;
  voice: string;
  safetyIdentifier?: string;
}

interface RealtimeClientSecretBase {
  candidate: 'direct-openai';
  connectionMode: 'webrtc-ephemeral';
  token: string;
  expiresAt: number | null;
  realtimeCallsUrl: string;
  model: string;
  voice: string;
  authority: 'speech-enrichment-only';
}

export interface DirectOpenAiRealtimeClientSecret extends RealtimeClientSecretBase {
  configurationId: typeof DIRECT_OPENAI_CONFIGURATION_ID;
  privacyEligibility: 'synthetic-only';
}

export interface FounderWakeRealtimeClientSecret extends RealtimeClientSecretBase {
  configurationId: typeof FOUNDER_WAKE_CONFIGURATION_ID;
  privacyEligibility: 'founder-consented-default-api-retention';
}

type FetchLike = (input: string | URL | Request, init?: RequestInit) => Promise<Response>;

export function parseDirectOpenAiRealtimeConfig(
  environment: NodeJS.ProcessEnv,
): DirectOpenAiRealtimeConfig {
  const env = environmentSchema.parse(environment);

  return {
    configured: Boolean(env.OPENAI_API_KEY),
    enabled: env.WMW_ENABLE_DIRECT_OPENAI_REALTIME_SPIKE,
    founderDogfoodEnabled: env.WMW_ENABLE_FOUNDER_REALTIME_DOGFOOD,
    ...(env.OPENAI_API_KEY ? { apiKey: env.OPENAI_API_KEY } : {}),
    model: env.WMW_OPENAI_REALTIME_MODEL,
    voice: env.WMW_OPENAI_REALTIME_VOICE,
    ...(env.WMW_OPENAI_SAFETY_IDENTIFIER
      ? { safetyIdentifier: env.WMW_OPENAI_SAFETY_IDENTIFIER }
      : {}),
  };
}

export function requireDirectOpenAiRealtimeSpike(
  config = parseDirectOpenAiRealtimeConfig(process.env),
): DirectOpenAiRealtimeConfig & { apiKey: string; safetyIdentifier: string } {
  if (!config.enabled) {
    throw new HttpError(
      503,
      'Direct OpenAI realtime spike is disabled. Enable it only for synthetic, non-sensitive M8 experiments.',
    );
  }
  return requireRealtimeCredentials(config, 'Direct OpenAI realtime spike');
}

export function requireFounderWakeRealtimeDogfood(
  config = parseDirectOpenAiRealtimeConfig(process.env),
): DirectOpenAiRealtimeConfig & { apiKey: string; safetyIdentifier: string } {
  if (!config.founderDogfoodEnabled) {
    throw new HttpError(503, 'Founder Realtime Wake dogfood is disabled.');
  }
  return requireRealtimeCredentials(config, 'Founder Realtime Wake dogfood');
}

export async function createDirectOpenAiRealtimeClientSecret(options: {
  environment?: NodeJS.ProcessEnv;
  fetchImpl?: FetchLike;
} = {}): Promise<DirectOpenAiRealtimeClientSecret> {
  const config = requireDirectOpenAiRealtimeSpike(
    parseDirectOpenAiRealtimeConfig(options.environment ?? process.env),
  );
  const token = await mintRealtimeClientSecret(config, options.fetchImpl ?? fetch);

  return {
    ...token,
    configurationId: DIRECT_OPENAI_CONFIGURATION_ID,
    privacyEligibility: 'synthetic-only',
  };
}

/**
 * Founder-only real-audio dogfood credential.
 *
 * This mode is intentionally explicit rather than silently reclassifying the synthetic M8 route.
 * The OpenAI API does not use API inputs/outputs for training by default, but default abuse
 * monitoring retention may still apply unless the concrete OpenAI project has approved data
 * controls. The Android UI therefore requires founder consent and sends no Tomorrow Contract or
 * other prepared private context in this phase.
 */
export async function createFounderWakeRealtimeClientSecret(options: {
  environment?: NodeJS.ProcessEnv;
  fetchImpl?: FetchLike;
} = {}): Promise<FounderWakeRealtimeClientSecret> {
  const config = requireFounderWakeRealtimeDogfood(
    parseDirectOpenAiRealtimeConfig(options.environment ?? process.env),
  );
  const token = await mintRealtimeClientSecret(config, options.fetchImpl ?? fetch);

  return {
    ...token,
    configurationId: FOUNDER_WAKE_CONFIGURATION_ID,
    privacyEligibility: 'founder-consented-default-api-retention',
  };
}

function requireRealtimeCredentials(
  config: DirectOpenAiRealtimeConfig,
  label: string,
): DirectOpenAiRealtimeConfig & { apiKey: string; safetyIdentifier: string } {
  if (!config.apiKey) {
    throw new HttpError(503, `${label} is not configured.`);
  }
  if (!config.safetyIdentifier) {
    throw new HttpError(503, `OpenAI safety identifier is not configured for ${label.toLowerCase()}.`);
  }
  return { ...config, apiKey: config.apiKey, safetyIdentifier: config.safetyIdentifier };
}

async function mintRealtimeClientSecret(
  config: DirectOpenAiRealtimeConfig & { apiKey: string; safetyIdentifier: string },
  fetchImpl: FetchLike,
): Promise<RealtimeClientSecretBase> {
  const response = await fetchImpl(OPENAI_CLIENT_SECRETS_URL, {
    method: 'POST',
    headers: {
      authorization: `Bearer ${config.apiKey}`,
      'content-type': 'application/json',
      'openai-safety-identifier': config.safetyIdentifier,
    },
    body: JSON.stringify({
      session: {
        type: 'realtime',
        model: config.model,
        audio: {
          output: {
            voice: config.voice,
          },
        },
      },
    }),
    signal: AbortSignal.timeout(15_000),
  });

  if (!response.ok) {
    // Never include the provider response body here: it can contain operational
    // details that do not belong in our application logs or client surface.
    throw new Error(`Direct OpenAI realtime client-secret request failed with HTTP ${response.status}`);
  }

  const parsed = clientSecretResponseSchema.safeParse(await response.json());
  if (!parsed.success) {
    throw new Error('Direct OpenAI realtime client-secret response was malformed');
  }

  return {
    candidate: 'direct-openai',
    connectionMode: 'webrtc-ephemeral',
    token: parsed.data.value,
    expiresAt: parsed.data.expires_at ?? null,
    realtimeCallsUrl: OPENAI_REALTIME_CALLS_URL,
    model: config.model,
    voice: config.voice,
    authority: 'speech-enrichment-only',
  };
}
