import { z } from 'zod';
import { HttpError } from '../http.js';

/**
 * GPT-Live is NOT Realtime. It requires server-side SDP exchange, and creates
 * an entirely different session lifecycle. Do not reuse /v1/realtime/calls.
 * This is intentionally feature-gated until the Android audio/event adapter
 * has passed physical-device tests.
 */
export const OPENAI_LIVE_SESSIONS_URL = 'https://api.openai.com/v1/live/sessions';
export const GPT_LIVE_MODEL = 'gpt-live-1';
export const LIVE_CONFIGURATION_ID = 'openai:gpt-live-1:webrtc-client-v1';

const offerSchema = z.string().min(40).max(65536)
  .refine((sdp) => sdp.startsWith('v=0') && /\bm=audio\s/.test(sdp), 'Invalid WebRTC audio offer');

export const liveRequestSchema = z.object({
  sdp: offerSchema,
  characterId: z.enum(['alfred', 'sam', 'luna', 'rocco']).default('alfred'),
  voiceStyle: z.enum(['default', 'minimal', 'motivational']).default('default'),
  // Presentation only. These may NEVER amend the authoritative WakeRuntime policy.
  energy: z.enum(['calm', 'balanced', 'energetic']).default('balanced'),
  directness: z.enum(['gentle', 'balanced', 'firm']).default('balanced'),
  conversationAmount: z.enum(['minimal', 'natural', 'chatty']).default('natural'),
  humor: z.enum(['off', 'subtle', 'playful']).default('subtle'),
  locale: z.enum(['en', 'de', 'he']).default('en'),
}).strict();

/** Safe allowlisted voice identifiers; no user-supplied raw OpenAI voice IDs. */
export const BUILTIN_CHARACTER_VOICES = {
  alfred: 'vesper',
  sam: 'meridian',
  luna: 'willow',
  rocco: 'ripple',
} as const;

/**
 * An operator may activate one previously reviewed prompt-created Voice ID per
 * character. Clients never submit provider voice IDs. The ID is resolved once
 * at session creation and therefore cannot drift mid-wake.
 */
export function configuredVoiceFor(characterId: keyof typeof BUILTIN_CHARACTER_VOICES): string {
  const key = `WMW_GPT_LIVE_VOICE_${characterId.toUpperCase()}`;
  const configured = process.env[key]?.trim();
  if (!configured) return BUILTIN_CHARACTER_VOICES[characterId];
  if (!/^[A-Za-z0-9_-]{1,128}$/.test(configured)) {
    throw new HttpError(503, 'An approved character voice is misconfigured.');
  }
  return configured;
}

const characterIdentity = {
  alfred: 'Alfred: familiar British presence, composed, warm but understated, dry and gently witty. Avoid theatrical butler acting.',
  sam: 'Sam: a warm, steady, relaxed friend. Conversational and understanding; never chirpy, performative or overprotective.',
  luna: 'Luna: quiet, grounded, reassuring and concise. A soft voice, but always audible; no whisper-only output.',
  rocco: 'Rocco: lightly sarcastic, playful, direct and familiar. Humour is about the situation, never the user. Never shout.',
} as const;

const baseInstructions = [
  'You are WakeMyWay\'s optional morning conversational companion.',
  'One stable voice and identity throughout this session. Speak naturally, not like an AI assistant or narrator.',
  'The Android Alarm Kernel owns alarm playback and stop/snooze. The deterministic WakeRuntime owns wake phases, physical requests, and completion.',
  'You have NO authority to claim the user stood, moved, snoozed or completed the wake. Never announce success before verified application state.',
  'Keep replies brief and easy to follow for a sleepy person. Do not turn every reply into another action or question.',
  'Treat silence as silence. Do not repeatedly demand spoken proof. Pause and listen.',
  'Let the user interrupt. Acknowledge complaints and jokes conversationally, without motivational slogans.',
  'Do not give unsolicited physical instructions or invent private context. No shaming, threats, shouting, or unsafe physical tasks.',
  'When an action or verified state is needed, delegate to the client application. Never execute an action yourself.',
  'At startup remain silent until the application sends an authorized wake directive. Avoid background chatter.',
].join('\n');

export function liveSessionConfig(input: z.infer<typeof liveRequestSchema>) {
  const modifier = input.voiceStyle === 'minimal'
    ? 'Use very few words; do not become abrupt or robotic.'
    : input.voiceStyle === 'motivational'
      ? 'Be a little more energetic while remaining humane; never become a cheerleader.'
      : 'Keep a relaxed conversational pace.';
  const expression = [
    `Speak in ${{ en: 'English', de: 'German', he: 'Hebrew' }[input.locale]}.`,
    `Vocal energy: ${input.energy}. Keep natural, audible bedroom-level volume.`,
    `Verbal directness: ${input.directness}, without shaming or coercion.`,
    `Conversation amount: ${input.conversationAmount}; never force a reply.`,
    `Humor: ${input.humor}; no jokes when the user is distressed.`,
  ].join(' ');
  return {
    model: GPT_LIVE_MODEL,
    store: false,
    audio: { output: { voice: configuredVoiceFor(input.characterId) } },
    delegation: { type: 'client' as const },
    instructions: [baseInstructions, characterIdentity[input.characterId], modifier, expression].join('\n\n'),
  };
}

export interface LiveSessionAnswer {
  session: { id: string };
  transport: { type: 'webrtc'; sdp: string };
}

export async function createLiveSession(
  input: z.infer<typeof liveRequestSchema>,
  options: { apiKey?: string; fetchImpl?: typeof fetch; safetyIdentifier: string } ,
): Promise<LiveSessionAnswer> {
  if (process.env.WMW_GPT_LIVE_ENABLED !== 'true') {
    throw new HttpError(503, 'GPT-Live experiment is disabled.');
  }
  const apiKey = options.apiKey ?? process.env.OPENAI_API_KEY;
  if (!apiKey) throw new HttpError(503, 'GPT-Live is not configured.');

  const response = await (options.fetchImpl ?? fetch)(OPENAI_LIVE_SESSIONS_URL, {
    method: 'POST',
    headers: {
      authorization: `Bearer ${apiKey}`,
      'content-type': 'application/json',
      'openai-safety-identifier': options.safetyIdentifier,
    },
    body: JSON.stringify({ session: liveSessionConfig(input), transport: { type: 'webrtc', sdp: input.sdp } }),
    signal: AbortSignal.timeout(15_000),
  });
  if (!response.ok) {
    // Do not log SDP, provider body, personal content or credentials.
    console.error('[wmw-gpt-live]', { operation: 'session', providerStatus: response.status });
    if (response.status === 429) throw new HttpError(503, 'Voice capacity is temporarily unavailable.');
    throw new HttpError(503, 'GPT-Live session could not be created.');
  }
  const schema = z.object({
    session: z.object({ id: z.string().min(4).max(256) }),
    transport: z.object({ type: z.literal('webrtc'), sdp: z.string().min(40).max(65536) }),
  });
  const parsed = schema.safeParse(await response.json());
  if (!parsed.success) throw new HttpError(502, 'Invalid GPT-Live SDP response.');
  return {
    session: { id: parsed.data.session.id },
    transport: { type: 'webrtc', sdp: parsed.data.transport.sdp },
  };
}
