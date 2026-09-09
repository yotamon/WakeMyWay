import { z } from 'zod';

const modelIdSchema = z
  .string()
  .trim()
  .min(3)
  .refine(value => value.includes('/'), 'AI Gateway model IDs must use creator/model form');

const optionalSecretSchema = z.string().trim().min(1).optional();
const booleanStringSchema = z
  .enum(['true', 'false'])
  .default('false')
  .transform(value => value === 'true');

const environmentSchema = z.object({
  AI_GATEWAY_API_KEY: optionalSecretSchema,
  VERCEL_OIDC_TOKEN: optionalSecretSchema,
  WMW_INTERNAL_API_KEY: z.string().min(32).optional(),
  WMW_ENABLE_NON_ZDR_AUDIO_SPIKES: booleanStringSchema,
  WMW_AI_FAST_MODEL: modelIdSchema.default('openai/gpt-5.4-mini'),
  WMW_AI_FAST_FALLBACKS: z.string().default('anthropic/claude-haiku-4.5,google/gemini-3.5-flash'),
  WMW_AI_SMART_MODEL: modelIdSchema.default('anthropic/claude-sonnet-4.6'),
  WMW_AI_SMART_FALLBACKS: z.string().default('openai/gpt-5.4,google/gemini-3.5-flash'),
  WMW_AI_EMBEDDING_MODEL: modelIdSchema.default('openai/text-embedding-3-small'),
  WMW_AI_TRANSCRIPTION_MODEL: modelIdSchema.default('openai/whisper-1'),
  WMW_AI_SPEECH_MODEL: modelIdSchema.default('openai/tts-1'),
  WMW_AI_SPEECH_VOICE: z.string().trim().min(1).max(128).default('alloy'),
  WMW_AI_REALTIME_MODEL: modelIdSchema.default('openai/gpt-realtime-2.1'),
});

export type TextTask = 'fast' | 'smart';

export interface AIConfig {
  gatewayConfigured: boolean;
  internalApiKey?: string;
  allowNonZdrAudioSpikes: boolean;
  fast: TextModelPolicy;
  smart: TextModelPolicy;
  embeddingModel: string;
  transcriptionModel: string;
  speechModel: string;
  speechVoice: string;
  realtimeModel: string;
}

export interface TextModelPolicy {
  primary: string;
  fallbacks: string[];
}

function parseFallbacks(raw: string, primary: string): string[] {
  const unique = new Set<string>();

  for (const candidate of raw.split(',')) {
    const value = candidate.trim();
    if (!value || value === primary) continue;
    modelIdSchema.parse(value);
    unique.add(value);
  }

  return [...unique];
}

export function parseAIConfig(environment: NodeJS.ProcessEnv): AIConfig {
  const env = environmentSchema.parse(environment);

  return {
    gatewayConfigured: Boolean(env.AI_GATEWAY_API_KEY || env.VERCEL_OIDC_TOKEN),
    ...(env.WMW_INTERNAL_API_KEY ? { internalApiKey: env.WMW_INTERNAL_API_KEY } : {}),
    allowNonZdrAudioSpikes: env.WMW_ENABLE_NON_ZDR_AUDIO_SPIKES,
    fast: {
      primary: env.WMW_AI_FAST_MODEL,
      fallbacks: parseFallbacks(env.WMW_AI_FAST_FALLBACKS, env.WMW_AI_FAST_MODEL),
    },
    smart: {
      primary: env.WMW_AI_SMART_MODEL,
      fallbacks: parseFallbacks(env.WMW_AI_SMART_FALLBACKS, env.WMW_AI_SMART_MODEL),
    },
    embeddingModel: env.WMW_AI_EMBEDDING_MODEL,
    transcriptionModel: env.WMW_AI_TRANSCRIPTION_MODEL,
    speechModel: env.WMW_AI_SPEECH_MODEL,
    speechVoice: env.WMW_AI_SPEECH_VOICE,
    realtimeModel: env.WMW_AI_REALTIME_MODEL,
  };
}

export function getAIConfig(): AIConfig {
  return parseAIConfig(process.env);
}

export function requireGatewayConfiguration(config = getAIConfig()): AIConfig {
  if (!config.gatewayConfigured) {
    throw new Error('AI Gateway is not configured. Set AI_GATEWAY_API_KEY or use Vercel OIDC.');
  }
  return config;
}

export function requireNonZdrAudioSpike(config = requireGatewayConfiguration()): AIConfig {
  if (!config.allowNonZdrAudioSpikes) {
    throw new Error(
      'Non-ZDR audio/realtime spikes are disabled. Enable WMW_ENABLE_NON_ZDR_AUDIO_SPIKES only for synthetic, non-sensitive M8 engineering experiments.',
    );
  }
  return config;
}

export function publicAIConfig(config = getAIConfig()) {
  return {
    gatewayConfigured: config.gatewayConfigured,
    privacy: {
      privateTextAndEmbeddingsRequireZdr: true,
      nonZdrAudioSpikesEnabled: config.allowNonZdrAudioSpikes,
    },
    text: {
      fast: { primary: config.fast.primary, fallbacks: [...config.fast.fallbacks] },
      smart: { primary: config.smart.primary, fallbacks: [...config.smart.fallbacks] },
    },
    embeddingModel: config.embeddingModel,
    transcriptionModel: config.transcriptionModel,
    speechModel: config.speechModel,
    realtimeModel: config.realtimeModel,
  };
}
