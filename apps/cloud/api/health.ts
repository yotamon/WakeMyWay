import { publicAIConfig } from '../src/ai/config';
import { json, methodNotAllowed, requestId } from '../src/http';

export default {
  async fetch(request: Request): Promise<Response> {
    const id = requestId(request);
    if (request.method !== 'GET') return methodNotAllowed(['GET'], id);

    return json(
      {
        ok: true,
        service: 'wake-my-way-cloud',
        authority: 'non-critical-enrichment-only',
        realtimeStatus: 'spike-only-not-wake-authority',
        ai: publicAIConfig(),
      },
      200,
      id,
    );
  },
};
