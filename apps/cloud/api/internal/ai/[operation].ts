import { z } from 'zod';

import {
  createRealtimeToken,
  embedTextWithAI,
  generateSpeechWithAI,
  generateTextWithAI,
  streamTextWithAI,
  transcribeWithAI,
} from '../../../src/ai/platform.js';
import {
  errorResponse,
  HttpError,
  json,
  methodNotAllowed,
  parseJson,
  requestId,
  requireInternalAuthorization,
} from '../../../src/http.js';

const textSchema = z.object({
  task: z.enum(['fast', 'smart']).default('fast'),
  prompt: z.string().trim().min(1).max(12_000),
  system: z.string().trim().min(1).max(6_000).optional(),
});

const embedSchema = z.object({
  value: z.string().trim().min(1).max(16_000),
});

const speechSchema = z.object({
  text: z.string().trim().min(1).max(4_000),
  voice: z.string().trim().min(1).max(128).optional(),
  language: z.string().trim().min(2).max(16).optional(),
  speed: z.number().min(0.5).max(1.5).optional(),
});

const MAX_AUDIO_BYTES = 10 * 1024 * 1024;
const OPERATIONS = new Set([
  'text',
  'stream',
  'embed',
  'speech',
  'transcribe',
  'realtime-token',
]);

export default {
  async fetch(request: Request): Promise<Response> {
    const id = requestId(request);
    if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

    const operation = operationFromRequest(request);
    if (!operation || !OPERATIONS.has(operation)) {
      return json({ error: 'AI operation was not found.', requestId: id }, 404, id);
    }

    try {
      requireInternalAuthorization(request);

      switch (operation) {
        case 'text':
          return handleText(request, id);
        case 'stream':
          return handleStream(request, id);
        case 'embed':
          return handleEmbed(request, id);
        case 'speech':
          return handleSpeech(request, id);
        case 'transcribe':
          return handleTranscribe(request, id);
        case 'realtime-token':
          return handleRealtimeToken(id);
        default:
          return json({ error: 'AI operation was not found.', requestId: id }, 404, id);
      }
    } catch (error) {
      return errorResponse(error, id, `ai.${operation}`);
    }
  },
};

function operationFromRequest(request: Request): string | undefined {
  const pathname = new URL(request.url).pathname.replace(/\/+$/, '');
  return pathname.split('/').filter(Boolean).at(-1);
}

async function handleText(request: Request, id: string): Promise<Response> {
  const input = await parseJson(request, textSchema);
  const result = await generateTextWithAI({
    task: input.task,
    prompt: input.prompt,
    ...(input.system !== undefined ? { system: input.system } : {}),
  });
  return json(result, 200, id);
}

async function handleStream(request: Request, id: string): Promise<Response> {
  const input = await parseJson(request, textSchema);
  const response = streamTextWithAI({
    task: input.task,
    prompt: input.prompt,
    ...(input.system !== undefined ? { system: input.system } : {}),
  });
  const headers = new Headers(response.headers);
  headers.set('x-request-id', id);
  headers.set('cache-control', 'no-store');
  return new Response(response.body, { status: response.status, headers });
}

async function handleEmbed(request: Request, id: string): Promise<Response> {
  const { value } = await parseJson(request, embedSchema);
  return json(await embedTextWithAI(value), 200, id);
}

async function handleSpeech(request: Request, id: string): Promise<Response> {
  const input = await parseJson(request, speechSchema);
  const speech = await generateSpeechWithAI({
    text: input.text,
    ...(input.voice !== undefined ? { voice: input.voice } : {}),
    ...(input.language !== undefined ? { language: input.language } : {}),
    ...(input.speed !== undefined ? { speed: input.speed } : {}),
  });

  const body = new ArrayBuffer(speech.bytes.byteLength);
  new Uint8Array(body).set(speech.bytes);

  return new Response(body, {
    status: 200,
    headers: {
      'content-type': speech.mediaType || 'audio/mpeg',
      'cache-control': 'private, no-store',
      'content-disposition': `inline; filename="wmw-speech.${speech.format || 'mp3'}"`,
      'x-request-id': id,
      'x-wmw-ai-model': speech.model,
    },
  });
}

async function handleTranscribe(request: Request, id: string): Promise<Response> {
  const contentType = request.headers.get('content-type')?.toLowerCase() ?? '';
  if (!contentType.startsWith('audio/')) {
    throw new HttpError(415, 'Content-Type must be audio/*.');
  }

  const declaredLength = Number(request.headers.get('content-length') ?? 0);
  if (Number.isFinite(declaredLength) && declaredLength > MAX_AUDIO_BYTES) {
    throw new HttpError(413, 'Audio payload is too large.');
  }

  const buffer = await request.arrayBuffer();
  if (buffer.byteLength === 0) throw new HttpError(400, 'Audio payload is empty.');
  if (buffer.byteLength > MAX_AUDIO_BYTES) throw new HttpError(413, 'Audio payload is too large.');

  const result = await transcribeWithAI(new Uint8Array(buffer));
  return json(result, 200, id);
}

async function handleRealtimeToken(id: string): Promise<Response> {
  const session = await createRealtimeToken();
  return json(
    {
      ...session,
      authority: 'speech-enrichment-only',
      note: 'M8 spike credential. Never use this token path as alarm or Wake Runtime authority.',
    },
    200,
    id,
  );
}
