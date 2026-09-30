import { describe, expect, it, vi } from 'vitest';

import {
  accountRealtimeSafetyIdentifier,
  handleAccountRealtimeTokenRequest,
} from '../src/account/realtime-handler';
import type { AccountWakeRealtimeClientSecret } from '../src/voice-spike/direct-openai';

const USER_ID = '2d53f744-d923-4a85-9ed6-7ea4aeece445';

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

describe('account Realtime token route', () => {
  it('requires POST', async () => {
    const response = await handleAccountRealtimeTokenRequest(
      new Request('https://wakemyway.test/api/v1/account/realtime-token'),
    );
    expect(response.status).toBe(405);
  });

  it('verifies the account and mints a scoped Realtime secret', async () => {
    const verifier = vi.fn(async () => ({ userId: USER_ID, email: 'wake@example.test' }));
    const mintSecret = vi.fn(async ({ safetyIdentifier }: { safetyIdentifier: string }) => {
      expect(safetyIdentifier).toBe(accountRealtimeSafetyIdentifier(USER_ID));
      return secret;
    });
    const response = await handleAccountRealtimeTokenRequest(
      new Request('https://wakemyway.test/api/v1/account/realtime-token', {
        method: 'POST',
        headers: { authorization: 'Bearer signed-neon-jwt' },
      }),
      { verifier, mintSecret },
    );

    expect(response.status).toBe(200);
    expect(verifier).toHaveBeenCalledWith('signed-neon-jwt');
    expect(mintSecret).toHaveBeenCalledTimes(1);
    expect(await response.json()).toMatchObject({
      configurationId: 'direct-openai:webrtc-account-wake-v1',
      token: 'ephemeral-secret',
    });
  });

  it('derives a stable pseudonymous OpenAI safety identifier', () => {
    const value = accountRealtimeSafetyIdentifier(USER_ID);
    expect(value).toMatch(/^wmw_[a-f0-9]{60}$/);
    expect(value).not.toContain(USER_ID);
  });
});
