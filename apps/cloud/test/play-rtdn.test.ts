import { Buffer } from 'node:buffer';

import { describe, expect, it, vi } from 'vitest';

import {
  purchaseTokenHash,
  type PlayPurchaseLifecycleRecord,
  type PlayPurchaseLifecycleStore,
} from '../src/commerce/play-lifecycle-store';
import { handlePlayRtdnRequest } from '../src/commerce/play-rtdn-handler';
import {
  decodePlayRtdnNotification,
  requirePlayRtdnConfig,
} from '../src/commerce/play-rtdn';

const environment: NodeJS.ProcessEnv = {
  WMW_PLAY_VERIFICATION_ENABLED: 'true',
  WMW_PLAY_PACKAGE_NAME: 'com.wakemyway.app',
  WMW_PLAY_SUBSCRIPTION_PRODUCT_IDS: 'wakemyway_pro',
  GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL: 'play-verifier@example.iam.gserviceaccount.com',
  GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY: 'not-used-by-injected-token',
  WMW_PLAY_RTDN_ENABLED: 'true',
  WMW_PLAY_RTDN_AUDIENCE: 'https://wakemyway.example/api/v1/commerce/play-rtdn',
  WMW_PLAY_RTDN_SERVICE_ACCOUNT_EMAIL: 'play-rtdn@example.iam.gserviceaccount.com',
};

class MemoryPlayLifecycleStore implements PlayPurchaseLifecycleStore {
  readonly purchases: PlayPurchaseLifecycleRecord[] = [];
  readonly messages = new Map<string, string>();

  async put(record: PlayPurchaseLifecycleRecord): Promise<void> {
    this.purchases.push(record);
  }

  async hasProcessedNotification(messageId: string): Promise<boolean> {
    return this.messages.has(messageId);
  }

  async markNotificationProcessed(messageId: string, processedAt: string): Promise<void> {
    this.messages.set(messageId, processedAt);
  }
}

function pushRequest(
  notification: unknown,
  messageId = 'message-1',
): Request {
  return new Request(environment.WMW_PLAY_RTDN_AUDIENCE!, {
    method: 'POST',
    headers: {
      authorization: 'Bearer test-token',
      'content-type': 'application/json',
    },
    body: JSON.stringify({
      message: {
        messageId,
        data: Buffer.from(JSON.stringify(notification), 'utf8').toString('base64'),
      },
      subscription: 'projects/wmw/subscriptions/google-play-rtdn',
    }),
  });
}

const allowAuthorization = async () => undefined;

describe('Google Play RTDN lifecycle', () => {
  it('is disabled and fail-closed by default', () => {
    expect(() => requirePlayRtdnConfig({})).toThrowError('Play RTDN is not enabled.');
    expect(() =>
      requirePlayRtdnConfig({
        WMW_PLAY_RTDN_ENABLED: 'true',
        WMW_PLAY_RTDN_AUDIENCE: 'http://not-https.example',
        WMW_PLAY_RTDN_SERVICE_ACCOUNT_EMAIL: 'invalid@example.com',
      }),
    ).toThrow();
  });

  it('decodes bounded subscription notifications', () => {
    const push = {
      message: {
        messageId: 'decode-1',
        data: Buffer.from(
          JSON.stringify({
            version: '1.0',
            packageName: 'com.wakemyway.app',
            eventTimeMillis: '1790280000000',
            subscriptionNotification: {
              version: '1.0',
              notificationType: 2,
              purchaseToken: 'secret-purchase-token-123456',
            },
          }),
          'utf8',
        ).toString('base64'),
      },
    };

    const decoded = decodePlayRtdnNotification(push);
    expect(decoded.packageName).toBe('com.wakemyway.app');
    expect(decoded.subscriptionNotification?.notificationType).toBe(2);
  });

  it('accepts test notifications and dedupes by PubSub message id', async () => {
    const store = new MemoryPlayLifecycleStore();
    const request = () =>
      pushRequest({
        version: '1.0',
        packageName: 'com.wakemyway.app',
        eventTimeMillis: '1790280000000',
        testNotification: { version: '1.0' },
      }, 'test-message-1');

    const first = await handlePlayRtdnRequest(
      request(),
      {
        store,
        now: () => new Date('2026-09-24T20:30:00Z'),
        verifyAuthorization: allowAuthorization,
      },
      environment,
    );
    const second = await handlePlayRtdnRequest(
      request(),
      {
        store,
        now: () => new Date('2026-09-24T20:31:00Z'),
        verifyAuthorization: allowAuthorization,
      },
      environment,
    );

    expect(first.status).toBe(204);
    expect(second.status).toBe(204);
    expect(store.messages.size).toBe(1);
    expect(store.purchases).toEqual([]);
  });

  it('re-queries Google as source of truth, acknowledges, hashes token and dedupes retries', async () => {
    const store = new MemoryPlayLifecycleStore();
    const fetchMock = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(
        Response.json({
          subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE',
          acknowledgementState: 'ACKNOWLEDGEMENT_STATE_PENDING',
          linkedPurchaseToken: 'older-secret-linked-token-123456',
          lineItems: [
            {
              productId: 'wakemyway_pro',
              expiryTime: '2026-10-24T20:00:00Z',
            },
          ],
        }),
      )
      .mockResolvedValueOnce(new Response(null, { status: 200 }));

    const request = () =>
      pushRequest({
        version: '1.0',
        packageName: 'com.wakemyway.app',
        eventTimeMillis: '1790280000000',
        subscriptionNotification: {
          version: '1.0',
          notificationType: 2,
          purchaseToken: 'secret-purchase-token-123456',
        },
      }, 'subscription-message-1');

    const dependencies = {
      store,
      fetch: fetchMock,
      accessToken: async () => 'google-access-token',
      now: () => new Date('2026-09-24T20:30:00Z'),
      verifyAuthorization: allowAuthorization,
    };

    const first = await handlePlayRtdnRequest(request(), dependencies, environment);
    const duplicate = await handlePlayRtdnRequest(request(), dependencies, environment);

    expect(first.status).toBe(204);
    expect(duplicate.status).toBe(204);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(store.purchases).toEqual([
      {
        purchaseTokenHash: purchaseTokenHash('secret-purchase-token-123456'),
        productId: 'wakemyway_pro',
        entitlement: 'ACTIVE',
        acknowledged: true,
        linkedPurchaseTokenHash: purchaseTokenHash('older-secret-linked-token-123456'),
        expiryAt: '2026-10-24T20:00:00Z',
        verifiedAt: '2026-09-24T20:30:00.000Z',
      },
    ]);
    expect(store.messages.has('subscription-message-1')).toBe(true);
    expect(JSON.stringify(store.purchases)).not.toContain('secret-purchase-token-123456');
  });

  it('does not mark provider failures processed so PubSub can retry', async () => {
    const store = new MemoryPlayLifecycleStore();
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      new Response('temporary provider failure', { status: 503 }),
    );

    const response = await handlePlayRtdnRequest(
      pushRequest({
        version: '1.0',
        packageName: 'com.wakemyway.app',
        eventTimeMillis: '1790280000000',
        subscriptionNotification: {
          version: '1.0',
          notificationType: 2,
          purchaseToken: 'secret-purchase-token-123456',
        },
      }, 'retry-message-1'),
      {
        store,
        fetch: fetchMock,
        accessToken: async () => 'google-access-token',
        now: () => new Date('2026-09-24T20:30:00Z'),
        verifyAuthorization: allowAuthorization,
      },
      environment,
    );

    expect(response.status).toBe(503);
    expect(store.messages.has('retry-message-1')).toBe(false);
    expect(store.purchases).toEqual([]);
  });

  it('acknowledges pending-purchase cancellation without inventing entitlement', async () => {
    const store = new MemoryPlayLifecycleStore();
    const fetchMock = vi.fn<typeof fetch>();

    const response = await handlePlayRtdnRequest(
      pushRequest({
        version: '1.0',
        packageName: 'com.wakemyway.app',
        eventTimeMillis: '1790280000000',
        subscriptionNotification: {
          version: '1.0',
          notificationType: 20,
          purchaseToken: 'pending-purchase-token-123456',
        },
      }, 'pending-cancel-1'),
      {
        store,
        fetch: fetchMock,
        accessToken: async () => 'google-access-token',
        now: () => new Date('2026-09-24T20:30:00Z'),
        verifyAuthorization: allowAuthorization,
      },
      environment,
    );

    expect(response.status).toBe(204);
    expect(fetchMock).not.toHaveBeenCalled();
    expect(store.purchases).toEqual([]);
    expect(store.messages.has('pending-cancel-1')).toBe(true);
  });

  it('rejects notifications for another package without touching lifecycle state', async () => {
    const store = new MemoryPlayLifecycleStore();

    const response = await handlePlayRtdnRequest(
      pushRequest({
        version: '1.0',
        packageName: 'com.other.app',
        eventTimeMillis: '1790280000000',
        testNotification: { version: '1.0' },
      }, 'wrong-package-1'),
      {
        store,
        verifyAuthorization: allowAuthorization,
      },
      environment,
    );

    expect(response.status).toBe(400);
    expect(store.messages.size).toBe(0);
    expect(store.purchases).toEqual([]);
  });

  it('requires exactly one 1.0 subscription product before RTDN can infer product identity', async () => {
    const store = new MemoryPlayLifecycleStore();
    const multiProductEnvironment = {
      ...environment,
      WMW_PLAY_SUBSCRIPTION_PRODUCT_IDS: 'wakemyway_pro,wakemyway_extra',
    };

    const response = await handlePlayRtdnRequest(
      pushRequest({
        version: '1.0',
        packageName: 'com.wakemyway.app',
        eventTimeMillis: '1790280000000',
        subscriptionNotification: {
          version: '1.0',
          notificationType: 2,
          purchaseToken: 'secret-purchase-token-123456',
        },
      }, 'multi-product-1'),
      {
        store,
        verifyAuthorization: allowAuthorization,
      },
      multiProductEnvironment,
    );

    expect(response.status).toBe(503);
    expect(store.messages.size).toBe(0);
  });
});
