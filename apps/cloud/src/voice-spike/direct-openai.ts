import { z } from 'zod';

import { HttpError } from '../http';

const OPENAI_CLIENT_SECRETS_URL = 'https://api.openai.com/v1/realtime/client_secrets';
export const OPENAI_REALTIME_CALLS_URL = 'https://api.openai.com/v1/realtime/calls';
export const DIRECT_OPENAI_CONFIGURATION_ID = 'direct-openai:webrtc-ephemeral-v1';

const booleanStringSchema = z
  .enum(['true', 'false'])
  .default('false')
  .transform(value => value === 'true');

const environmentSchema = z.object({
  OPENAI_API_KEY: z.string().trim().min(1).optional(),
  WMW_ENABLE_DIRECT_OPENAI_REALTIME_SPIKE: booleanStringSchema,
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
  apiKey?: string;
  model: string;
  voice: string;
  safetyIdentifier?: string;
}

export interface DirectOpenAiRealtimeClientSecret {
  candidate: 'direct-openai';
  configurationId: string;
  connectionMode: 'webrtc-ephemeral';
  token: string;
  expiresAt: number | null;
  realtimeCallsUrl: string;
  model: string;
  voice: string;
  privacyEligibility: 'synthetic-only';
  authority: 'speech-enrichment-only';
}

type FetchLike = (input: string | URL | Request, init?: RequestInit) => Promise<Response>;

export function parseDirectOpenAiRealtimeConfig(
  environment: NodeJS.ProcessEnv,
): DirectOpenAiRealtimeConfig {
  const env = environmentSchema.parse(environment);

  return {
    configured: Boolean(env.OPENAI_API_KEY),
    enabled: env.WMW_ENABLE_DIRECT_OPENAI_REALTIME_SPIKE,
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
  if (!config.apiKey) {
    throw new HttpError(503, 'Direct OpenAI realtime spike is not configured.');
  }
  if (!config.safetyIdentifier) {
    throw new HttpError(503, 'OpenAI safety identifier is not configured for the realtime spike.');
  }

  return { ...config, apiKey: config.apiKey, safetyIdentifier: config.safetyIdentifier };
}

export async function createDirectOpenAiRealtimeClientSecret(options: {
  environment?: NodeJS.ProcessEnv;
  fetchImpl?: FetchLike;
} = {}): Promise<DirectOpenAiRealtimeClientSecret> {
  const config = requireDirectOpenAiRealtimeSpike(
    parseDirectOpenAiRealtimeConfig(options.environment ?? process.env),
  );
  const fetchImpl = options.fetchImpl ?? fetch;

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
    configurationId: DIRECT_OPENAI_CONFIGURATION_ID,
    connectionMode: 'webrtc-ephemeral',
    token: parsed.data.value,
    expiresAt: parsed.data.expires_at ?? null,
    realtimeCallsUrl: OPENAI_REALTIME_CALLS_URL,
    model: config.model,
    voice: config.voice,
    // The Realtime endpoint may be eligible for stricter data controls, but WMW
    // does not promote this configuration until the exact OpenAI project/account
    // posture has been independently verified and recorded as evidence.
    privacyEligibility: 'synthetic-only',
    authority: 'speech-enrichment-only',
  };
}
