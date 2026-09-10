import { requireFounderRealtimeAuthorization } from '../../../src/founder-realtime-auth';
import { errorResponse, json, methodNotAllowed, requestId } from '../../../src/http';
import { createFounderWakeRealtimeClientSecret } from '../../../src/voice-spike/direct-openai';

export default {
  async fetch(request: Request): Promise<Response> {
    const id = requestId(request);
    if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

    try {
      // Existing internal bearer auth remains valid for diagnostics, while the founder app uses a
      // scoped installation credential obtained through the explicit pairing flow.
      requireFounderRealtimeAuthorization(request);
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
