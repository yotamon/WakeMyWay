import { z } from 'zod';

import { generateSpeechWithAI } from '../../../src/ai/platform';
import {
  errorResponse,
  methodNotAllowed,
  parseJson,
  requestId,
  requireInternalAuthorization,
} from '../../../src/http';

const inputSchema = z.object({
  text: z.string().trim().min(1).max(4_000),
  voice: z.string().trim().min(1).max(128).optional(),
  language: z.string().trim().min(2).max(16).optional(),
  speed: z.number().min(0.5).max(1.5).optional(),
});

export default {
  async fetch(request: Request): Promise<Response> {
    const id = requestId(request);
    if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

    try {
      requireInternalAuthorization(request);
      const input = await parseJson(request, inputSchema);
      const speech = await generateSpeechWithAI({
        text: input.text,
        ...(input.voice !== undefined ? { voice: input.voice } : {}),
        ...(input.language !== undefined ? { language: input.language } : {}),
        ...(input.speed !== undefined ? { speed: input.speed } : {}),
      });

      // DOM BodyInit is intentionally backed by a concrete ArrayBuffer, not an
      // ArrayBufferLike view that could be SharedArrayBuffer under newer TS libs.
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
    } catch (error) {
      return errorResponse(error, id, 'ai.speech');
    }
  },
};
