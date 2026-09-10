import { founderRealtimeSetupStatus } from '../../../src/founder-realtime-auth';
import { json, methodNotAllowed, requestId } from '../../../src/http';

export default {
  async fetch(request: Request): Promise<Response> {
    const id = requestId(request);
    if (request.method !== 'GET') return methodNotAllowed(['GET'], id);

    const status = founderRealtimeSetupStatus();
    return json(
      {
        available: status.available,
        missing: status.missing,
      },
      200,
      id,
    );
  },
};
