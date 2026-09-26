import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import {
  handleMobileGoogleExchange,
  mobilePkceChallenge,
  openMobileHandoff,
  sealMobileHandoff,
} from '../src/account/mobile-oauth';

const USER_ID = '0a3fade4-92f7-4c08-aaee-d9a8e87b84cb';
const SESSION_TOKEN = 'session-token-that-is-long-enough-for-the-test';
const VERIFIER = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~abc';

beforeEach(() => {
  process.env.NEON_AUTH_COOKIE_SECRET =
    'test-cookie-secret-that-is-at-least-thirty-two-characters-long';
});

afterEach(() => {
  delete process.env.NEON_AUTH_COOKIE_SECRET;
});

describe('mobile Neon OAuth handoff', () => {
  it('round-trips an encrypted handoff without exposing the session token', () => {
    const challenge = mobilePkceChallenge(VERIFIER);
    const sealed = sealMobileHandoff({
      v: 1,
      sessionToken: SESSION_TOKEN,
      userId: USER_ID,
      challenge,
      exp: Date.now() + 60_000,
    });

    expect(sealed).not.toContain(SESSION_TOKEN);
    expect(openMobileHandoff(sealed)).toMatchObject({
      v: 1,
      sessionToken: SESSION_TOKEN,
      userId: USER_ID,
      challenge,
    });
  });

  it('rejects a tampered handoff', () => {
    const sealed = sealMobileHandoff({
      v: 1,
      sessionToken: SESSION_TOKEN,
      userId: USER_ID,
      challenge: mobilePkceChallenge(VERIFIER),
      exp: Date.now() + 60_000,
    });
    const replacement = sealed.endsWith('A') ? 'B' : 'A';
    const tampered = sealed.slice(0, -1) + replacement;

    expect(() => openMobileHandoff(tampered)).toThrow('invalid');
  });

  it('exchanges a valid PKCE-bound handoff for the opaque Neon session token', async () => {
    const handoff = sealMobileHandoff({
      v: 1,
      sessionToken: SESSION_TOKEN,
      userId: USER_ID,
      challenge: mobilePkceChallenge(VERIFIER),
      exp: Date.now() + 60_000,
    });
    const request = new Request(
      'https://wakemyway.vercel.app/api/v1/account/mobile-google-exchange',
      {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ handoff, verifier: VERIFIER }),
      },
    );

    const response = await handleMobileGoogleExchange(request);

    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toMatchObject({
      sessionToken: SESSION_TOKEN,
      userId: USER_ID,
    });
  });

  it('rejects the handoff when the PKCE verifier does not match', async () => {
    const handoff = sealMobileHandoff({
      v: 1,
      sessionToken: SESSION_TOKEN,
      userId: USER_ID,
      challenge: mobilePkceChallenge(VERIFIER),
      exp: Date.now() + 60_000,
    });
    const request = new Request(
      'https://wakemyway.vercel.app/api/v1/account/mobile-google-exchange',
      {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({
          handoff,
          verifier: 'wrong-verifier-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ012345',
        }),
      },
    );

    const response = await handleMobileGoogleExchange(request);

    expect(response.status).toBe(401);
  });

  it('rejects an expired handoff', async () => {
    const handoff = sealMobileHandoff({
      v: 1,
      sessionToken: SESSION_TOKEN,
      userId: USER_ID,
      challenge: mobilePkceChallenge(VERIFIER),
      exp: Date.now() - 1,
    });
    const request = new Request(
      'https://wakemyway.vercel.app/api/v1/account/mobile-google-exchange',
      {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ handoff, verifier: VERIFIER }),
      },
    );

    const response = await handleMobileGoogleExchange(request);

    expect(response.status).toBe(401);
  });
});
