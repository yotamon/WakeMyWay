import { describe, expect, it, vi } from 'vitest';

import { handlePlayVerificationRequest } from '../src/commerce/play-verification-handler';
import {
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

  it('maps a verified active matching subscription without exposing provider payload', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      Response.json({
        subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE',
        acknowledgementState: 'ACKNOWLEDGEMENT_STATE_PENDING',
        latestOrderId: 'GPA.private-order-id',
        lineItems: [
          {
            productId: 'wakemyway_pro',
            expiryTime: '2026-10-24T20:00:00Z',
          },
        ],
      }),
    );
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

    expect(result).toEqual({
      productId: 'wakemyway_pro',
      entitlement: 'ACTIVE',
      acknowledged: false,
    });
    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(String(url)).toContain('/applications/com.wakemyway.app/purchases/subscriptionsv2/tokens/');
    expect(String(url)).toContain('secret-purchase-token-123456');
    expect(init?.headers).toEqual({
      authorization: 'Bearer google-access-token',
      accept: 'application/json',
    });
    expect(JSON.stringify(result)).not.toContain('GPA.private-order-id');
    expect(JSON.stringify(result)).not.toContain('secret-purchase-token-123456');
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
  });

  it('handler returns only normalized verification data', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      Response.json({
        subscriptionState: 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD',
        acknowledgementState: 'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED',
        externalAccountIdentifiers: {
          obfuscatedExternalAccountId: 'private-account-id',
        },
        lineItems: [{ productId: 'wakemyway_pro' }],
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
