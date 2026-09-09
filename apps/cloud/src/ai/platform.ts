import { gateway, type GatewayProviderOptions } from '@ai-sdk/gateway';
import {
  Output,
  embed,
  experimental_generateSpeech as generateSpeech,
  experimental_transcribe as transcribe,
  generateText,
  streamText,
} from 'ai';
import type { z } from 'zod';

import {
  getAIConfig,
  requireGatewayConfiguration,
  requireNonZdrAudioSpike,
  type TextModelPolicy,
  type TextTask,
} from './config';

const DEFAULT_TEXT_TIMEOUT_MS = 20_000;
const DEFAULT_AUDIO_TIMEOUT_MS = 30_000;

export interface TextGenerationInput {
  task: TextTask;
  prompt: string;
  system?: string;
  timeoutMs?: number;
}

export interface SpeechGenerationInput {
  text: string;
  voice?: string;
  language?: string;
  speed?: number;
}

function textPolicy(task: TextTask): TextModelPolicy {
  const config = requireGatewayConfiguration();
  return task === 'fast' ? config.fast : config.smart;
}

/**
 * Private WMW text/embedding requests fail closed to Gateway routes with
 * provider-level Zero Data Retention. ZDR also implies prompt-training opt-out.
 * We intentionally do not enable Gateway prompt caching for private wake data.
 */
export function privateGatewayRouting(policy?: TextModelPolicy) {
  const options = {
    zeroDataRetention: true,
    disallowPromptTraining: true,
    ...(policy && policy.fallbacks.length > 0 ? { models: [...policy.fallbacks] } : {}),
  } satisfies GatewayProviderOptions;

  return { gateway: options };
}

function signal(timeoutMs: number): AbortSignal {
  return AbortSignal.timeout(timeoutMs);
}

export async function generateTextWithAI(input: TextGenerationInput) {
  const policy = textPolicy(input.task);
  const result = await generateText({
    model: gateway(policy.primary),
    prompt: input.prompt,
    ...(input.system ? { system: input.system } : {}),
    providerOptions: privateGatewayRouting(policy),
    maxRetries: 2,
    abortSignal: signal(input.timeoutMs ?? DEFAULT_TEXT_TIMEOUT_MS),
  });

  return {
    text: result.text,
    finishReason: result.finishReason,
    usage: result.usage,
    route: { primary: policy.primary, fallbacks: [...policy.fallbacks], privacy: 'zdr' as const },
  };
}

export function streamTextWithAI(input: TextGenerationInput): Response {
  const policy = textPolicy(input.task);
  const result = streamText({
    model: gateway(policy.primary),
    prompt: input.prompt,
    ...(input.system ? { system: input.system } : {}),
    providerOptions: privateGatewayRouting(policy),
    maxRetries: 2,
    abortSignal: signal(input.timeoutMs ?? DEFAULT_TEXT_TIMEOUT_MS),
  });

  return result.toTextStreamResponse({
    headers: {
      'cache-control': 'no-store',
      'x-wmw-ai-primary-model': policy.primary,
      'x-wmw-ai-privacy': 'zdr',
    },
  });
}

export async function generateStructuredWithAI<T extends z.ZodType>(input: {
  task: TextTask;
  prompt: string;
  schema: T;
  system?: string;
  timeoutMs?: number;
}): Promise<z.output<T>> {
  const policy = textPolicy(input.task);
  const result = await generateText({
    model: gateway(policy.primary),
    prompt: input.prompt,
    ...(input.system ? { system: input.system } : {}),
    output: Output.object({ schema: input.schema }),
    providerOptions: privateGatewayRouting(policy),
    maxRetries: 2,
    abortSignal: signal(input.timeoutMs ?? DEFAULT_TEXT_TIMEOUT_MS),
  });

  return result.output as z.output<T>;
}

export async function embedTextWithAI(value: string) {
  const config = requireGatewayConfiguration();
  const result = await embed({
    model: gateway.textEmbeddingModel(config.embeddingModel),
    value,
    providerOptions: privateGatewayRouting(),
    maxRetries: 2,
    abortSignal: signal(DEFAULT_TEXT_TIMEOUT_MS),
  });

  return {
    embedding: result.embedding,
    usage: result.usage,
    model: config.embeddingModel,
    privacy: 'zdr' as const,
  };
}

/**
 * Current Gateway audio models in this foundation do not provide ZDR. These
 * functions are therefore disabled by default and are only for synthetic,
 * non-sensitive engineering spikes until M8 selects a privacy-safe route.
 */
export async function transcribeWithAI(audio: Uint8Array) {
  const config = requireNonZdrAudioSpike();
  const result = await transcribe({
    model: gateway.transcriptionModel(config.transcriptionModel),
    audio,
    maxRetries: 2,
    abortSignal: signal(DEFAULT_AUDIO_TIMEOUT_MS),
  });

  return {
    text: result.text,
    language: result.language,
    durationInSeconds: result.durationInSeconds,
    segments: result.segments,
    model: config.transcriptionModel,
    privacy: 'non-zdr-spike' as const,
  };
}

export async function generateSpeechWithAI(input: SpeechGenerationInput) {
  const config = requireNonZdrAudioSpike();
  const result = await generateSpeech({
    model: gateway.speechModel(config.speechModel),
    text: input.text,
    voice: input.voice ?? config.speechVoice,
    outputFormat: 'mp3',
    ...(input.language ? { language: input.language } : {}),
    ...(input.speed !== undefined ? { speed: input.speed } : {}),
    maxRetries: 2,
    abortSignal: signal(DEFAULT_AUDIO_TIMEOUT_MS),
  });

  return {
    bytes: result.audio.uint8Array,
    mediaType: result.audio.mediaType,
    format: result.audio.format,
    model: config.speechModel,
    privacy: 'non-zdr-spike' as const,
  };
}

export async function createRealtimeToken() {
  const config = requireNonZdrAudioSpike();
  const token = await gateway.experimental_realtime.getToken({
    model: config.realtimeModel,
  });

  return {
    token: token.token,
    url: token.url,
    model: config.realtimeModel,
    privacy: 'non-zdr-spike' as const,
  };
}

export function getConfiguredAIModels() {
  return getAIConfig();
}
