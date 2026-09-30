import { describe, expect, it } from 'vitest';

import {
  ACCOUNT_REALTIME_DEVICE_TTL_SECONDS,
  accountRealtimeSafetyIdentifier,
  issueAccountRealtimeCredential,
  verifyAccountRealtimeCredential,
} from '../src/account/account-realtime-auth';

const USER_ID = '2d53f744-d923-4a85-9ed6-7ea4aeece445';
const INSTALLATION_ID = '4d19d808-29f1-4ed6-8a74-31a14c9571ab';
const CREDENTIAL_ID = '859fc74d-dc3e-47b8-b739-10167a2ef931';
const environment: NodeJS.ProcessEnv = {
  WMW_REALTIME_TOKEN_SIGNING_KEY: 'r'.repeat(64),
};

describe('account Realtime device credentials', () => {
  it('issues a short-lived scoped credential without embedding account identity', () => {
    const issued = issueAccountRealtimeCredential(USER_ID, INSTALLATION_ID, {
      environment,
      nowSeconds: 1_800_000_000,
      credentialId: CREDENTIAL_ID,
    });
    expect(issued.deviceToken).not.toContain(USER_ID);
    expect(issued.credentialId).toBe(CREDENTIAL_ID);
    expect(issued.expiresAt).toBe(1_800_000_000 + ACCOUNT_REALTIME_DEVICE_TTL_SECONDS);

    const verified = verifyAccountRealtimeCredential(issued.deviceToken, {
      environment,
      nowSeconds: 1_800_000_001,
    });
    expect(verified.credentialId).toBe(CREDENTIAL_ID);
    expect(verified.installationId).toBe(INSTALLATION_ID);
    expect(verified.accountPseudonym).toMatch(/^[a-f0-9]{40}$/);
    expect(accountRealtimeSafetyIdentifier(verified)).toMatch(/^wmw_[a-f0-9]{60}$/);
  });

  it('rejects tampered and expired credentials', () => {
    const issued = issueAccountRealtimeCredential(USER_ID, INSTALLATION_ID, {
      environment,
      nowSeconds: 1_800_000_000,
      credentialId: CREDENTIAL_ID,
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

  it('rejects credentials whose signed lifetime exceeds the current policy', () => {
    const issued = issueAccountRealtimeCredential(USER_ID, INSTALLATION_ID, {
      environment,
      nowSeconds: 1_800_000_000,
      credentialId: CREDENTIAL_ID,
    });
    const encoded = issued.deviceToken.split('.')[0]!;
    const payload = JSON.parse(Buffer.from(encoded, 'base64url').toString('utf8')) as Record<string, unknown>;
    payload.exp = Number(payload.exp) + 1;
    const modified = Buffer.from(JSON.stringify(payload), 'utf8').toString('base64url');

    expect(() => verifyAccountRealtimeCredential(`${modified}.${issued.deviceToken.split('.')[1]}`, {
      environment,
      nowSeconds: 1_800_000_001,
    })).toThrow();
  });

  it('accepts the legacy server signing key only as deployment-compatible fallback', () => {
    const legacyEnvironment: NodeJS.ProcessEnv = {
      WMW_FOUNDER_TOKEN_SIGNING_KEY: 's'.repeat(64),
    };
    const issued = issueAccountRealtimeCredential(USER_ID, INSTALLATION_ID, {
      environment: legacyEnvironment,
      nowSeconds: 1_800_000_000,
      credentialId: CREDENTIAL_ID,
    });
    expect(() => verifyAccountRealtimeCredential(issued.deviceToken, {
      environment: legacyEnvironment,
      nowSeconds: 1_800_000_001,
    })).not.toThrow();
  });
});
