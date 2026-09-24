import { describe, expect, it, vi } from 'vitest';

import {
  purchaseTokenHash,
  type PlayPurchaseLifecycleRecord,
  type PlayPurchaseLifecycleStore,
} from '../src/commerce/play-lifecycle-store';
import { handlePlayVerificationRequest } from '../src/commerce/play-verification-handler';
import {
  grantsPaidAccess,
  mapSubscriptionState,
  requirePlayVerificationConfig,
  verifyPlaySubscription,
} from '../src/commerce/play-verification';

const environment: NodeJS.ProcessEnv = {
  WMW_PLAY_VERIFICATION_ENABLED: 'true',
  WMW_PLAY_PACKAGE_NAME: 'com.wakemyway.app',
  WMW_PLAY_SUBSCRIPTION_PRODUCT_IDS: 'wakemyway_pro',
  GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL: 'play-verifier@example.iam.gserviceaccount.com',
  GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY: 'not-used-by-injected-token',
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

describe('Google Play subscription verification', () => {
  it('is disabled by default', () => {
    expect(() => requirePlayVerificationConfig({})).toThrowError(
      'Play purchase verification is not enabled.',
    );
  });

  it('rejects products outside the server allowlist before calling Google', async () => {
    const fetchMock = vi.fn<typeof fetch>();
    const config = requirePlayVerificationConfig(environment);

    await expect(
      verifyPlaySubscription(
        {
          productId: 'not_wakemyway',
          purchaseToken: 'purchase-token-with-enough-length',
        },
        config,
        {
          fetch: fetchMock,
          accessToken: async () => 'google-access-token',
        },
      ),
    ).rejects.toMatchObject({
      status: 400,
      message: 'Subscription product is not supported.',
    });
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('verifies and acknowledges active matching subscriptions server-side', async () => {
    const fetchMock = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(
        Response.json({
          subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE',
          acknowledgementState: 'ACKNOWLEDGEMENT_STATE_PENDING',
          latestOrderId: 'GPA.private-order-id',
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
    const config = requirePlayVerificationConfig(environment);

    const result = await verifyPlaySubscription(
      {
        productId: 'wakemyway_pro',
        purchaseToken: 'secret-purchase-token-123456',
      },
      config,
      {
        fetch: fetchMock,
        accessToken: async () => 'google-access-token',
      },
    );

    expect(result).toMatchObject({
      productId: 'wakemyway_pro',
      entitlement: 'ACTIVE',
      acknowledged: true,
      expiryAt: '2026-10-24T20:00:00Z',
      linkedPurchaseToken: 'older-secret-linked-token-123456',
    });

    const [verifyUrl, verifyInit] = fetchMock.mock.calls[0] ?? [];
    expect(String(verifyUrl)).toContain(
      '/applications/com.wakemyway.app/purchases/subscriptionsv2/tokens/',
    );
    expect(verifyInit?.method).toBe('GET');

    const [ackUrl, ackInit] = fetchMock.mock.calls[1] ?? [];
    expect(String(ackUrl)).toContain(
      '/purchases/subscriptions/wakemyway_pro/tokens/secret-purchase-token-123456:acknowledge',
    );
    expect(ackInit?.method).toBe('POST');

    expect(JSON.stringify(result)).not.toContain('GPA.private-order-id');
  });

  it('keeps valid entitlement but reports acknowledgement false when acknowledgement must retry', async () => {
    const fetchMock = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(
        Response.json({
          subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE',
          acknowledgementState: 'ACKNOWLEDGEMENT_STATE_PENDING',
          lineItems: [{ productId: 'wakemyway_pro' }],
        }),
      )
      .mockResolvedValueOnce(new Response('temporary', { status: 503 }));

    const result = await verifyPlaySubscription(
      {
        productId: 'wakemyway_pro',
        purchaseToken: 'secret-purchase-token-123456',
      },
      requirePlayVerificationConfig(environment),
      {
        fetch: fetchMock,
        accessToken: async () => 'google-access-token',
      },
    );

    expect(result.entitlement).toBe('ACTIVE');
    expect(result.acknowledged).toBe(false);
  });

  it('does not try to acknowledge non-entitled lifecycle states', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      Response.json({
        subscriptionState: 'SUBSCRIPTION_STATE_ON_HOLD',
        acknowledgementState: 'ACKNOWLEDGEMENT_STATE_PENDING',
        lineItems: [{ productId: 'wakemyway_pro' }],
      }),
    );

    const result = await verifyPlaySubscription(
      {
        productId: 'wakemyway_pro',
        purchaseToken: 'secret-purchase-token-123456',
      },
      requirePlayVerificationConfig(environment),
      {
        fetch: fetchMock,
        accessToken: async () => 'google-access-token',
      },
    );

    expect(result.entitlement).toBe('ON_HOLD');
    expect(result.acknowledged).toBe(false);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('rejects a valid token whose subscription line item belongs to another product', async () => {
    const config = requirePlayVerificationConfig(environment);
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      Response.json({
        subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE',
        lineItems: [{ productId: 'some_other_product' }],
      }),
    );

    await expect(
      verifyPlaySubscription(
        {
          productId: 'wakemyway_pro',
          purchaseToken: 'secret-purchase-token-123456',
        },
        config,
        {
          fetch: fetchMock,
          accessToken: async () => 'google-access-token',
        },
      ),
    ).rejects.toMatchObject({
      status: 401,
      message: 'Purchase could not be verified.',
    });
  });

  it('fails closed when Google is unavailable', async () => {
    const config = requirePlayVerificationConfig(environment);
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      new Response('provider detail that must not escape', { status: 503 }),
    );

    await expect(
      verifyPlaySubscription(
        {
          productId: 'wakemyway_pro',
          purchaseToken: 'secret-purchase-token-123456',
        },
        config,
        {
          fetch: fetchMock,
          accessToken: async () => 'google-access-token',
        },
      ),
    ).rejects.toMatchObject({
      status: 503,
      message: 'Play purchase verification is temporarily unavailable.',
    });
  });

  it('normalizes subscription lifecycle without inventing entitlement', () => {
    expect(mapSubscriptionState('SUBSCRIPTION_STATE_ACTIVE')).toBe('ACTIVE');
    expect(mapSubscriptionState('SUBSCRIPTION_STATE_CANCELED')).toBe('CANCELLED_ENTITLED');
    expect(mapSubscriptionState('SUBSCRIPTION_STATE_IN_GRACE_PERIOD')).toBe('GRACE_PERIOD');
    expect(mapSubscriptionState('SUBSCRIPTION_STATE_ON_HOLD')).toBe('ON_HOLD');
    expect(mapSubscriptionState('SUBSCRIPTION_STATE_PAUSED')).toBe('PAUSED');
    expect(mapSubscriptionState('SUBSCRIPTION_STATE_EXPIRED')).toBe('EXPIRED');
    expect(mapSubscriptionState('SUBSCRIPTION_STATE_PENDING')).toBe('UNKNOWN');
    expect(mapSubscriptionState('SOMETHING_FUTURE')).toBe('UNKNOWN');

    expect(grantsPaidAccess('ACTIVE')).toBe(true);
    expect(grantsPaidAccess('CANCELLED_ENTITLED')).toBe(true);
    expect(grantsPaidAccess('GRACE_PERIOD')).toBe(true);
    expect(grantsPaidAccess('ON_HOLD')).toBe(false);
    expect(grantsPaidAccess('EXPIRED')).toBe(false);
  });

  it('handler returns normalized data and stores only token hashes', async () => {
    const store = new MemoryPlayLifecycleStore();
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      Response.json({
        subscriptionState: 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD',
        acknowledgementState: 'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED',
        linkedPurchaseToken: 'older-secret-linked-token-123456',
        externalAccountIdentifiers: {
          obfuscatedExternalAccountId: 'private-account-id',
        },
        lineItems: [
          {
            productId: 'wakemyway_pro',
            expiryTime: '2026-10-24T20:00:00Z',
          },
        ],
      }),
    );
    const request = new Request('https://example.test/api/v1/commerce/play-verify', {
      method: 'POST',
      body: JSON.stringify({
        productId: 'wakemyway_pro',
        purchaseToken: 'secret-purchase-token-123456',
      }),
    });

    const response = await handlePlayVerificationRequest(
      request,
      {
        fetch: fetchMock,
        accessToken: async () => 'google-access-token',
        now: () => new Date('2026-09-24T20:00:00Z'),
        store,
      },
      environment,
    );

    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body).toMatchObject({
      productId: 'wakemyway_pro',
      entitlement: 'GRACE_PERIOD',
      acknowledged: true,
    });
    expect(JSON.stringify(body)).not.toContain('private-account-id');
    expect(JSON.stringify(body)).not.toContain('secret-purchase-token-123456');
    expect(JSON.stringify(body)).not.toContain('older-secret-linked-token-123456');

    expect(store.purchases).toEqual([
      {
        purchaseTokenHash: purchaseTokenHash('secret-purchase-token-123456'),
        productId: 'wakemyway_pro',
        entitlement: 'GRACE_PERIOD',
        acknowledged: true,
        linkedPurchaseTokenHash: purchaseTokenHash('older-secret-linked-token-123456'),
        expiryAt: '2026-10-24T20:00:00Z',
        verifiedAt: '2026-09-24T20:00:00.000Z',
      },
    ]);
    expect(JSON.stringify(store.purchases)).not.toContain('secret-purchase-token-123456');
    expect(JSON.stringify(store.purchases)).not.toContain('older-secret-linked-token-123456');
  });

  it('handler stays unavailable until production verification is explicitly enabled', async () => {
    const request = new Request('https://example.test/api/v1/commerce/play-verify', {
      method: 'POST',
      body: JSON.stringify({
        productId: 'wakemyway_pro',
        purchaseToken: 'secret-purchase-token-123456',
      }),
    });

    const response = await handlePlayVerificationRequest(request, {}, {});
    expect(response.status).toBe(503);
    expect(await response.json()).toMatchObject({
      error: 'Play purchase verification is not enabled.',
    });
  });

  it('rejects non-POST methods without touching provider state', async () => {
    const response = await handlePlayVerificationRequest(
      new Request('https://example.test/api/v1/commerce/play-verify'),
      {},
      environment,
    );

    expect(response.status).toBe(405);
    expect(response.headers.get('allow')).toBe('POST');
  });
});
