import { transcribeWithAI } from '../../../src/ai/platform';
import {
  errorResponse,
  HttpError,
  json,
  methodNotAllowed,
  requestId,
  requireInternalAuthorization,
} from '../../../src/http';

const MAX_AUDIO_BYTES = 10 * 1024 * 1024;

export default {
  async fetch(request: Request): Promise<Response> {
    const id = requestId(request);
    if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

    try {
      requireInternalAuthorization(request);

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
    } catch (error) {
      return errorResponse(error, id, 'ai.transcribe');
    }
  },
};
