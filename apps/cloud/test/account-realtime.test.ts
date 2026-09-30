import { describe, expect, it, vi } from 'vitest';

import {
  handleAccountRealtimeProvisionRequest,
  handleAccountRealtimeTokenRequest,
} from '../src/account/realtime-handler';
import type { AccountRealtimeDeviceStore } from '../src/account/realtime-device-store';
import type { AccountWakeRealtimeClientSecret } from '../src/voice-spike/direct-openai';

const USER_ID = '2d53f744-d923-4a85-9ed6-7ea4aeece445';
const INSTALLATION_ID = '4d19d808-29f1-4ed6-8a74-31a14c9571ab';
const CREDENTIAL_ID = '859fc74d-dc3e-47b8-b739-10167a2ef931';

const secret: AccountWakeRealtimeClientSecret = {
  candidate: 'direct-openai',
  connectionMode: 'webrtc-ephemeral',
  token: 'ephemeral-secret',
  expiresAt: 1_800_000_000,
  realtimeCallsUrl: 'https://api.openai.com/v1/realtime/calls',
  model: 'gpt-realtime-2.1',
  voice: 'marin',
  authority: 'speech-enrichment-only',
  configurationId: 'direct-openai:webrtc-account-wake-v1',
  privacyEligibility: 'authenticated-account-default-api-retention',
};

function deviceStore(): AccountRealtimeDeviceStore & {
  activate: ReturnType<typeof vi.fn>;
  authorize: ReturnType<typeof vi.fn>;
  revoke: ReturnType<typeof vi.fn>;
} {
  return {
    activate: vi.fn(async () => undefined),
    authorize: vi.fn(async () => undefined),
    revoke: vi.fn(async () => undefined),
  };
}

describe('account Realtime provisioning route', () => {
  it('requires POST or DELETE', async () => {
    const response = await handleAccountRealtimeProvisionRequest(
      new Request('https://wakemyway.test/api/v1/account/realtime-provision'),
    );
    expect(response.status).toBe(405);
  });

  it('persists server authorization before returning a device credential', async () => {
    const verifier = vi.fn(async () => ({ userId: USER_ID, email: 'wake@example.test' }));
    const issueCredential = vi.fn(() => ({
      deviceToken: 'device-token',
      credentialId: CREDENTIAL_ID,
      expiresAt: 1_900_000_000,
    }));
    const store = deviceStore();
    const response = await handleAccountRealtimeProvisionRequest(
      new Request('https://wakemyway.test/api/v1/account/realtime-provision', {
        method: 'POST',
        headers: {
          authorization: 'Bearer signed-neon-jwt',
          'content-type': 'application/json',
        },
        body: JSON.stringify({ installationId: INSTALLATION_ID }),
      }),
      { verifier, issueCredential, deviceStore: store },
    );

    expect(response.status).toBe(200);
    expect(verifier).toHaveBeenCalledWith('signed-neon-jwt');
    expect(issueCredential).toHaveBeenCalledWith(USER_ID, INSTALLATION_ID);
    expect(store.activate).toHaveBeenCalledWith(expect.objectContaining({
      credentialId: CREDENTIAL_ID,
      accountId: USER_ID,
      installationId: INSTALLATION_ID,
      expiresAt: 1_900_000_000,
    }));
    expect(await response.json()).toEqual(expect.objectContaining({
      deviceToken: 'device-token',
      expiresAt: 1_900_000_000,
    }));
  });

  it('revokes the current installation with authenticated DELETE', async () => {
    const verifier = vi.fn(async () => ({ userId: USER_ID, email: 'wake@example.test' }));
    const store = deviceStore();
    const response = await handleAccountRealtimeProvisionRequest(
      new Request('https://wakemyway.test/api/v1/account/realtime-provision', {
        method: 'DELETE',
        headers: {
          authorization: 'Bearer signed-neon-jwt',
          'content-type': 'application/json',
        },
        body: JSON.stringify({ installationId: INSTALLATION_ID }),
      }),
      { verifier, deviceStore: store },
    );

    expect(response.status).toBe(200);
    expect(store.revoke).toHaveBeenCalledWith(USER_ID, INSTALLATION_ID);
    expect(await response.json()).toEqual(expect.objectContaining({ revoked: true }));
  });
});

describe('account Realtime token route', () => {
  it('rejects missing device authorization', async () => {
    const response = await handleAccountRealtimeTokenRequest(
      new Request('https://wakemyway.test/api/v1/account/realtime-token', { method: 'POST' }),
    );
    expect(response.status).toBe(401);
  });

  it('requires active server authorization before minting OpenAI Realtime', async () => {
    const credential = {
      credentialId: CREDENTIAL_ID,
      installationId: INSTALLATION_ID,
      accountPseudonym: 'a'.repeat(40),
      expiresAt: 1_900_000_000,
    };
    const verifyCredential = vi.fn(() => credential);
    const store = deviceStore();
    const mintSecret = vi.fn(async ({ safetyIdentifier }: { safetyIdentifier: string }) => {
      expect(safetyIdentifier).toMatch(/^wmw_[a-f0-9]{60}$/);
      return secret;
    });
    const response = await handleAccountRealtimeTokenRequest(
      new Request('https://wakemyway.test/api/v1/account/realtime-token', {
        method: 'POST',
        headers: { authorization: 'Bearer scoped-device-token' },
      }),
      { verifyCredential, mintSecret, deviceStore: store },
    );

    expect(response.status).toBe(200);
    expect(verifyCredential).toHaveBeenCalledWith('scoped-device-token');
    expect(store.authorize).toHaveBeenCalledWith(credential);
    expect(store.authorize.mock.invocationCallOrder[0]!).toBeLessThan(
      mintSecret.mock.invocationCallOrder[0]!,
    );
    expect(await response.json()).toMatchObject({
      configurationId: 'direct-openai:webrtc-account-wake-v1',
      token: 'ephemeral-secret',
    });
  });

  it('does not mint when the server authorization record is revoked or missing', async () => {
    const store = deviceStore();
    store.authorize.mockRejectedValueOnce(new Error('revoked'));
    const mintSecret = vi.fn(async () => secret);

    const response = await handleAccountRealtimeTokenRequest(
      new Request('https://wakemyway.test/api/v1/account/realtime-token', {
        method: 'POST',
        headers: { authorization: 'Bearer scoped-device-token' },
      }),
      {
        verifyCredential: () => ({
          credentialId: CREDENTIAL_ID,
          installationId: INSTALLATION_ID,
          accountPseudonym: 'a'.repeat(40),
          expiresAt: 1_900_000_000,
        }),
        mintSecret,
        deviceStore: store,
      },
    );

    expect(response.status).toBeGreaterThanOrEqual(400);
    expect(mintSecret).not.toHaveBeenCalled();
  });
});
