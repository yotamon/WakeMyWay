import { z } from 'zod';

import { generateTextWithAI } from '../../../src/ai/platform';
import {
  errorResponse,
  json,
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
      const result = await generateTextWithAI({
        task: input.task,
        prompt: input.prompt,
        ...(input.system !== undefined ? { system: input.system } : {}),
      });
      return json(result, 200, id);
    } catch (error) {
      return errorResponse(error, id, 'ai.text');
    }
  },
};
