import { describe, expect, it } from 'vitest';

import {
  accountRealtimeSafetyIdentifier,
  issueAccountRealtimeCredential,
  verifyAccountRealtimeCredential,
} from '../src/account/account-realtime-auth';

const USER_ID = '2d53f744-d923-4a85-9ed6-7ea4aeece445';
const INSTALLATION_ID = '4d19d808-29f1-4ed6-8a74-31a14c9571ab';
const environment: NodeJS.ProcessEnv = {
  WMW_REALTIME_TOKEN_SIGNING_KEY: 'r'.repeat(64),
};

describe('account Realtime device credentials', () => {
  it('issues a scoped credential without embedding account identity', () => {
    const issued = issueAccountRealtimeCredential(USER_ID, INSTALLATION_ID, {
      environment,
      nowSeconds: 1_800_000_000,
    });
    expect(issued.deviceToken).not.toContain(USER_ID);
    expect(issued.expiresAt).toBeGreaterThan(1_800_000_000);

    const verified = verifyAccountRealtimeCredential(issued.deviceToken, {
      environment,
      nowSeconds: 1_800_000_001,
    });
    expect(verified.installationId).toBe(INSTALLATION_ID);
    expect(verified.accountPseudonym).toMatch(/^[a-f0-9]{40}$/);
    expect(accountRealtimeSafetyIdentifier(verified)).toMatch(/^wmw_[a-f0-9]{60}$/);
  });

  it('rejects tampered and expired credentials', () => {
    const issued = issueAccountRealtimeCredential(USER_ID, INSTALLATION_ID, {
      environment,
      nowSeconds: 1_800_000_000,
    });
    expect(() => verifyAccountRealtimeCredential(`${issued.deviceToken}x`, {
      environment,
      nowSeconds: 1_800_000_001,
    })).toThrow();
    expect(() => verifyAccountRealtimeCredential(issued.deviceToken, {
      environment,
      nowSeconds: issued.expiresAt + 1,
    })).toThrow();
  });

  it('accepts the legacy server signing key only as deployment-compatible fallback', () => {
    const legacyEnvironment: NodeJS.ProcessEnv = {
      WMW_FOUNDER_TOKEN_SIGNING_KEY: 's'.repeat(64),
    };
    const issued = issueAccountRealtimeCredential(USER_ID, INSTALLATION_ID, {
      environment: legacyEnvironment,
      nowSeconds: 1_800_000_000,
    });
    expect(() => verifyAccountRealtimeCredential(issued.deviceToken, {
      environment: legacyEnvironment,
      nowSeconds: 1_800_000_001,
    })).not.toThrow();
  });
});
