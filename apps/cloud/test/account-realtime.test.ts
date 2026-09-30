import { describe, expect, it, vi } from 'vitest';

import {
  handleAccountRealtimeProvisionRequest,
  handleAccountRealtimeTokenRequest,
} from '../src/account/realtime-handler';
import type { AccountWakeRealtimeClientSecret } from '../src/voice-spike/direct-openai';

const USER_ID = '2d53f744-d923-4a85-9ed6-7ea4aeece445';
const INSTALLATION_ID = '4d19d808-29f1-4ed6-8a74-31a14c9571ab';

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

describe('account Realtime provisioning route', () => {
  it('requires POST', async () => {
    const response = await handleAccountRealtimeProvisionRequest(
      new Request('https://wakemyway.test/api/v1/account/realtime-provision'),
    );
    expect(response.status).toBe(405);
  });

  it('requires a valid account before issuing a device credential', async () => {
    const verifier = vi.fn(async () => ({ userId: USER_ID, email: 'wake@example.test' }));
    const issueCredential = vi.fn(() => ({
      deviceToken: 'device-token',
      expiresAt: 1_900_000_000,
    }));
    const response = await handleAccountRealtimeProvisionRequest(
      new Request('https://wakemyway.test/api/v1/account/realtime-provision', {
        method: 'POST',
        headers: {
          authorization: 'Bearer signed-neon-jwt',
          'content-type': 'application/json',
        },
        body: JSON.stringify({ installationId: INSTALLATION_ID }),
      }),
      { verifier, issueCredential },
    );

    expect(response.status).toBe(200);
    expect(verifier).toHaveBeenCalledWith('signed-neon-jwt');
    expect(issueCredential).toHaveBeenCalledWith(USER_ID, INSTALLATION_ID);
    expect(await response.json()).toMatchObject({ deviceToken: 'device-token' });
  });
});

describe('account Realtime token route', () => {
  it('rejects missing device authorization', async () => {
    const response = await handleAccountRealtimeTokenRequest(
      new Request('https://wakemyway.test/api/v1/account/realtime-token', { method: 'POST' }),
    );
    expect(response.status).toBe(401);
  });

  it('verifies the scoped device credential before minting OpenAI Realtime', async () => {
    const verifyCredential = vi.fn(() => ({
      installationId: INSTALLATION_ID,
      accountPseudonym: 'a'.repeat(40),
      expiresAt: 1_900_000_000,
    }));
    const mintSecret = vi.fn(async ({ safetyIdentifier }: { safetyIdentifier: string }) => {
      expect(safetyIdentifier).toMatch(/^wmw_[a-f0-9]{60}$/);
      return secret;
    });
    const response = await handleAccountRealtimeTokenRequest(
      new Request('https://wakemyway.test/api/v1/account/realtime-token', {
        method: 'POST',
        headers: { authorization: 'Bearer scoped-device-token' },
      }),
      { verifyCredential, mintSecret },
    );

    expect(response.status).toBe(200);
    expect(verifyCredential).toHaveBeenCalledWith('scoped-device-token');
    expect(mintSecret).toHaveBeenCalledTimes(1);
    expect(await response.json()).toMatchObject({
      configurationId: 'direct-openai:webrtc-account-wake-v1',
      token: 'ephemeral-secret',
    });
  });
});
