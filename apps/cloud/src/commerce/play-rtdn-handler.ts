import {
  PostgresPlayPurchaseLifecycleStore,
  persistVerifiedPlayPurchase,
  type PlayPurchaseLifecycleStore,
} from './play-lifecycle-store.js';
import {
  decodePlayRtdnNotification,
  playRtdnPushSchema,
  requirePlayRtdnConfig,
  verifyPlayRtdnAuthorization,
  type PlayRtdnAuthorizationVerifier,
} from './play-rtdn.js';
import {
  requirePlayVerificationConfig,
  verifyPlaySubscription,
  type PlayVerificationDependencies,
} from './play-verification.js';
import { HttpError, errorResponse, methodNotAllowed, parseJson, requestId } from '../http.js';

const MAX_PUSH_BYTES = 48 * 1024;
const PENDING_PURCHASE_CANCELED = 20;

export type PlayRtdnHandlerDependencies =
  Partial<PlayVerificationDependencies> & {
    store?: PlayPurchaseLifecycleStore;
    verifyAuthorization?: PlayRtdnAuthorizationVerifier;
  };

export async function handlePlayRtdnRequest(
  request: Request,
  suppliedDependencies: PlayRtdnHandlerDependencies = {},
  environment: NodeJS.ProcessEnv = process.env,
): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'POST') {
    return methodNotAllowed(['POST'], id);
  }

  try {
    const verificationConfig = requirePlayVerificationConfig(environment);
    const rtdnConfig = requirePlayRtdnConfig(environment);
    await (suppliedDependencies.verifyAuthorization ?? verifyPlayRtdnAuthorization)(
      request,
      rtdnConfig,
    );

    const push = await parseJson(request, playRtdnPushSchema, MAX_PUSH_BYTES);
    const notification = decodePlayRtdnNotification(push);
    if (notification.packageName !== verificationConfig.packageName) {
      throw new HttpError(400, 'RTDN package is not supported.');
    }

    const store = suppliedDependencies.store ?? new PostgresPlayPurchaseLifecycleStore();
    if (await store.hasProcessedNotification(push.message.messageId)) {
      return accepted(id);
    }

    const processedAt = (suppliedDependencies.now ?? (() => new Date()))().toISOString();

    if (notification.testNotification) {
      await store.markNotificationProcessed(push.message.messageId, processedAt);
      return accepted(id);
    }

    const subscription = notification.subscriptionNotification;
    if (!subscription) {
      // WakeMyWay 1.0 sells subscriptions only. Acknowledge unrelated authenticated Play
      // notifications without inventing commerce state.
      await store.markNotificationProcessed(push.message.messageId, processedAt);
      return accepted(id);
    }

    if (subscription.notificationType === PENDING_PURCHASE_CANCELED) {
      // PENDING never grants entitlement. No server lifecycle record is created for it.
      await store.markNotificationProcessed(push.message.messageId, processedAt);
      return accepted(id);
    }

    if (verificationConfig.allowedProductIds.size !== 1) {
      throw new HttpError(503, 'Play RTDN requires exactly one configured subscription product.');
    }
    const productId = [...verificationConfig.allowedProductIds][0];
    if (!productId) {
      throw new HttpError(503, 'Play RTDN subscription product is not configured.');
    }

    const result = await verifyPlaySubscription(
      {
        productId,
        purchaseToken: subscription.purchaseToken,
      },
      verificationConfig,
      suppliedDependencies,
    );

    await persistVerifiedPlayPurchase(
      store,
      subscription.purchaseToken,
      result,
      processedAt,
    );
    await store.markNotificationProcessed(push.message.messageId, processedAt);

    return accepted(id);
  } catch (error) {
    return errorResponse(error, id, 'play-rtdn');
  }
}

function accepted(id: string): Response {
  return new Response(null, {
    status: 204,
    headers: {
      'cache-control': 'no-store',
      'x-request-id': id,
    },
  });
}
