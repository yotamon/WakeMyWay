import {
  playVerificationRequestSchema,
  requirePlayVerificationConfig,
  verifyPlaySubscription,
  type PlayVerificationDependencies,
} from './play-verification.js';
import {
  PostgresPlayPurchaseLifecycleStore,
  purchaseTokenHash,
  type PlayPurchaseLifecycleStore,
} from './play-lifecycle-store.js';
import { errorResponse, json, methodNotAllowed, parseJson, requestId } from '../http.js';

const MAX_VERIFY_BYTES = 8 * 1024;

export type PlayVerificationHandlerDependencies =
  Partial<PlayVerificationDependencies> & {
    store?: PlayPurchaseLifecycleStore;
  };

export async function handlePlayVerificationRequest(
  request: Request,
  suppliedDependencies: PlayVerificationHandlerDependencies = {},
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
    const verifiedAt = (suppliedDependencies.now ?? (() => new Date()))().toISOString();
    const store = suppliedDependencies.store ?? new PostgresPlayPurchaseLifecycleStore();
    await store.put({
      purchaseTokenHash: purchaseTokenHash(input.purchaseToken),
      productId: result.productId,
      entitlement: result.entitlement,
      acknowledged: result.acknowledged,
      ...(result.linkedPurchaseToken
        ? { linkedPurchaseTokenHash: purchaseTokenHash(result.linkedPurchaseToken) }
        : {}),
      ...(result.expiryAt ? { expiryAt: result.expiryAt } : {}),
      verifiedAt,
    });

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
