import { createDirectOpenAiRealtimeClientSecret } from '../../../src/voice-spike/direct-openai';
import {
  errorResponse,
  json,
  methodNotAllowed,
  requestId,
  requireInternalAuthorization,
} from '../../../src/http';

export default {
  async fetch(request: Request): Promise<Response> {
    const id = requestId(request);
    if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

    try {
      requireInternalAuthorization(request);
      const secret = await createDirectOpenAiRealtimeClientSecret();
      return json(
        {
          ...secret,
          note: 'M8 synthetic spike credential. Never use this path as Alarm Kernel or Wake Runtime authority.',
        },
        200,
        id,
      );
    } catch (error) {
      return errorResponse(error, id, 'voice-spike.direct-openai-token');
    }
  },
};
