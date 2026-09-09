import { z } from 'zod';

import { embedTextWithAI } from '../../../src/ai/platform';
import {
  errorResponse,
  json,
  methodNotAllowed,
  parseJson,
  requestId,
  requireInternalAuthorization,
} from '../../../src/http';

const inputSchema = z.object({
  value: z.string().trim().min(1).max(16_000),
});

export default {
  async fetch(request: Request): Promise<Response> {
    const id = requestId(request);
    if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

    try {
      requireInternalAuthorization(request);
      const { value } = await parseJson(request, inputSchema);
      return json(await embedTextWithAI(value), 200, id);
    } catch (error) {
      return errorResponse(error, id, 'ai.embed');
    }
  },
};
