import { describe, expect, it } from 'vitest';

import { HttpError } from '../src/http';
import {
  FOUNDER_PAIRING_CODE_MIN_LENGTH,
  founderRealtimeSetupStatus,
  pairFounderInstallation,
  requireFounderRealtimeAuthorization,
  verifyFounderDeviceToken,
} from '../src/founder-realtime-auth';

const readyEnvironment: NodeJS.ProcessEnv = {
  OPENAI_API_KEY: 'server-only-openai-key',
  WMW_ENABLE_FOUNDER_REALTIME_DOGFOOD: 'true',
  WMW_OPENAI_SAFETY_IDENTIFIER: 'sha256:founder-device-fixture',
  WMW_INTERNAL_API_KEY: 's'.repeat(64),
  WMW_FOUNDER_PAIRING_CODE: 'WakeMyWay-Founder-Connect-2026',
};

const installationId = '4d19d808-29f1-4ed6-8a74-31a14c9571ab';

describe('founder Realtime server readiness', () => {
  it('reports exactly which safe server prerequisites are missing without exposing values', () => {
    const status = founderRealtimeSetupStatus({});

    expect(status.available).toBe(false);
    expect(status.missing).toEqual([
      'OpenAI API key',
      'founder Realtime gate',
      'OpenAI safety identifier',
      'server signing key',
      'founder access code',
    ]);
  });

  it('is available only when the complete founder configuration exists', () => {
    expect(founderRealtimeSetupStatus(readyEnvironment)).toEqual({ available: true, missing: [] });
  });

  it('does not treat a short founder secret as configured', () => {
    const shortEnvironment = {
      ...readyEnvironment,
      WMW_FOUNDER_PAIRING_CODE: 'x'.repeat(FOUNDER_PAIRING_CODE_MIN_LENGTH - 1),
    };

    expect(founderRealtimeSetupStatus(shortEnvironment)).toEqual({
      available: false,
      missing: ['founder access code'],
    });
  });
});

describe('founder installation pairing', () => {
  it('exchanges the access code for a scoped expiring installation credential', () => {
    const paired = pairFounderInstallation(
      { code: readyEnvironment.WMW_FOUNDER_PAIRING_CODE, installationId },
      { environment: readyEnvironment, nowSeconds: 1_800_000_000 },
    );

    expect(paired.deviceToken).not.toContain(readyEnvironment.WMW_FOUNDER_PAIRING_CODE!);
    expect(paired.deviceToken).not.toContain(readyEnvironment.WMW_INTERNAL_API_KEY!);
    expect(paired.expiresAt).toBeGreaterThan(1_800_000_000);

    const verified = verifyFounderDeviceToken(paired.deviceToken, {
      environment: readyEnvironment,
      nowSeconds: 1_800_000_001,
    });
    expect(verified.sub).toBe(installationId);
    expect(verified.scope).toBe('founder-realtime-wake');
  });

  it('rejects a wrong founder access code', () => {
    expect(() =>
      pairFounderInstallation(
        { code: 'definitely-not-the-founder-code', installationId },
        { environment: readyEnvironment, nowSeconds: 1_800_000_000 },
      ),
    ).toThrow(HttpError);
  });

  it('rejects pairing requests below the minimum founder secret length', () => {
    expect(() =>
      pairFounderInstallation(
        { code: 'x'.repeat(FOUNDER_PAIRING_CODE_MIN_LENGTH - 1), installationId },
        { environment: readyEnvironment, nowSeconds: 1_800_000_000 },
      ),
    ).toThrow(HttpError);
  });

  it('rejects tampered or expired credentials', () => {
    const paired = pairFounderInstallation(
      { code: readyEnvironment.WMW_FOUNDER_PAIRING_CODE, installationId },
      { environment: readyEnvironment, nowSeconds: 1_800_000_000 },
    );

    expect(() =>
      verifyFounderDeviceToken(`${paired.deviceToken}x`, {
        environment: readyEnvironment,
        nowSeconds: 1_800_000_001,
      }),
    ).toThrow(HttpError);

    expect(() =>
      verifyFounderDeviceToken(paired.deviceToken, {
        environment: readyEnvironment,
        nowSeconds: paired.expiresAt + 1,
      }),
    ).toThrow(/expired/i);
  });

  it('accepts both a paired installation credential and the existing internal admin key', () => {
    const paired = pairFounderInstallation(
      { code: readyEnvironment.WMW_FOUNDER_PAIRING_CODE, installationId },
      { environment: readyEnvironment, nowSeconds: Math.floor(Date.now() / 1000) },
    );

    const founderRequest = new Request('https://example.test', {
      headers: { authorization: `Bearer ${paired.deviceToken}` },
    });
    expect(() => requireFounderRealtimeAuthorization(founderRequest, readyEnvironment)).not.toThrow();

    const adminRequest = new Request('https://example.test', {
      headers: { authorization: `Bearer ${readyEnvironment.WMW_INTERNAL_API_KEY}` },
    });
    expect(() => requireFounderRealtimeAuthorization(adminRequest, readyEnvironment)).not.toThrow();
  });
});
