import { createRealtimeToken } from '../../../src/ai/platform.js';
import {
  errorResponse,
  json,
  methodNotAllowed,
  requestId,
  requireInternalAuthorization,
} from '../../../src/http.js';

export default {
  async fetch(request: Request): Promise<Response> {
    const id = requestId(request);
    if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

    try {
      requireInternalAuthorization(request);
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
    } catch (error) {
      return errorResponse(error, id, 'ai.realtime-token');
    }
  },
};
