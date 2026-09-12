import { z } from 'zod';

import {
  FOUNDER_PAIRING_CODE_MIN_LENGTH,
  pairFounderInstallation,
} from '../../../src/founder-realtime-auth';
import { errorResponse, json, methodNotAllowed, parseJson, requestId } from '../../../src/http';

const bodySchema = z.object({
  code: z.string().trim().min(FOUNDER_PAIRING_CODE_MIN_LENGTH).max(128),
  installationId: z.string().uuid(),
});

export default {
  async fetch(request: Request): Promise<Response> {
    const id = requestId(request);
    if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

    try {
      const body = await parseJson(request, bodySchema, 4 * 1024);
      const paired = pairFounderInstallation(body);
      return json(
        {
          deviceToken: paired.deviceToken,
          expiresAt: paired.expiresAt,
          brokerPath: '/api/internal/voice-spike/founder-wake-token',
        },
        200,
        id,
      );
    } catch (error) {
      return errorResponse(error, id, 'founder-realtime.pair');
    }
  },
};
