import {
  playVerificationRequestSchema,
  requirePlayVerificationConfig,
  verifyPlaySubscription,
  type PlayVerificationDependencies,
} from './play-verification.js';
import { errorResponse, json, methodNotAllowed, parseJson, requestId } from '../http.js';

const MAX_VERIFY_BYTES = 8 * 1024;

export async function handlePlayVerificationRequest(
  request: Request,
  suppliedDependencies: Partial<PlayVerificationDependencies> = {},
  environment: NodeJS.ProcessEnv = process.env,
): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'POST') {
    return methodNotAllowed(['POST'], id);
  }

  try {
    const config = requirePlayVerificationConfig(environment);
    const input = await parseJson(request, playVerificationRequestSchema, MAX_VERIFY_BYTES);
    const result = await verifyPlaySubscription(input, config, suppliedDependencies);

    return json(
      {
        productId: result.productId,
        entitlement: result.entitlement,
        acknowledged: result.acknowledged,
      },
      200,
      id,
    );
  } catch (error) {
    return errorResponse(error, id, 'play-subscription-verify');
  }
}
