import { createFounderWakeRealtimeClientSecret } from '../../../src/voice-spike/direct-openai';
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
      const secret = await createFounderWakeRealtimeClientSecret();
      return json(
        {
          ...secret,
          note: 'Founder-consented realtime wake dogfood. Speech enrichment only; never Alarm Kernel or Wake Runtime authority.',
        },
        200,
        id,
      );
    } catch (error) {
      return errorResponse(error, id, 'voice-dogfood.founder-wake-token');
    }
  },
};
