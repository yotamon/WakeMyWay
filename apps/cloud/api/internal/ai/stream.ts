import { z } from 'zod';

import { streamTextWithAI } from '../../../src/ai/platform';
import {
  errorResponse,
  methodNotAllowed,
  parseJson,
  requestId,
  requireInternalAuthorization,
} from '../../../src/http';

const inputSchema = z.object({
  task: z.enum(['fast', 'smart']).default('fast'),
  prompt: z.string().trim().min(1).max(12_000),
  system: z.string().trim().min(1).max(6_000).optional(),
});

export default {
  async fetch(request: Request): Promise<Response> {
    const id = requestId(request);
    if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

    try {
      requireInternalAuthorization(request);
      const input = await parseJson(request, inputSchema);
      const response = streamTextWithAI(input);
      const headers = new Headers(response.headers);
      headers.set('x-request-id', id);
      headers.set('cache-control', 'no-store');
      return new Response(response.body, { status: response.status, headers });
    } catch (error) {
      return errorResponse(error, id, 'ai.stream');
    }
  },
};
