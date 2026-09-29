import {
  handleAuthProxyRequest,
  NEON_AUTH_SESSION_CHALLENGE_COOKIE_NAME,
  NEON_AUTH_SESSION_COOKIE_NAME,
} from '@neondatabase/auth/server';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@neondatabase/auth/server', async importOriginal => {
  const actual = await importOriginal<typeof import('@neondatabase/auth/server')>();
  return {
    ...actual,
    handleAuthProxyRequest: vi.fn(),
  };
});

import {
  handleMobileGoogleComplete,
  handleMobileGoogleExchange,
  mobilePkceChallenge,
  openMobileHandoff,
  sealMobileHandoff,
} from '../src/account/mobile-oauth';

const USER_ID = '0a3fade4-92f7-4c08-aaee-d9a8e87b84cb';
const SESSION_TOKEN = 'signed-session-cookie-value-that-is-long-enough';
const SESSION_VERIFIER = 'server-verifier_1234567890';
const CHALLENGE_COOKIE =
  `${NEON_AUTH_SESSION_CHALLENGE_COOKIE_NAME}=signed-browser-challenge`;
const VERIFIER = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~abc';

beforeEach(() => {
  process.env.NEON_AUTH_BASE_URL = 'https://example.neonauth.test/neondb/auth';
  process.env.NEON_AUTH_COOKIE_SECRET =
    'test-cookie-secret-that-is-at-least-thirty-two-characters-long';
  vi.mocked(handleAuthProxyRequest).mockReset();
});

afterEach(() => {
  delete process.env.NEON_AUTH_BASE_URL;
  delete process.env.NEON_AUTH_COOKIE_SECRET;
});

describe('mobile Neon OAuth handoff', () => {
  it('packages the Neon verifier and challenge cookie without consuming them in the browser callback', async () => {
    const challenge = mobilePkceChallenge(VERIFIER);
    const request = new Request(
      `https://wakemyway.vercel.app/api/v1/account/mobile-google-complete?` +
        `challenge=${challenge}&neon_auth_session_verifier=${SESSION_VERIFIER}`,
      {
        headers: {
          cookie: `${CHALLENGE_COOKIE}; unrelated=value`,
        },
      },
    );

    const response = await handleMobileGoogleComplete(request);

    expect(response.status).toBe(302);
    const location = response.headers.get('location');
    expect(location).toMatch(/^wakemyway:\/\/auth\?handoff=/);

    const handoff = new URL(location!).searchParams.get('handoff');
    expect(handoff).toBeTruthy();
    expect(openMobileHandoff(handoff!)).toMatchObject({
      v: 2,
      sessionVerifier: SESSION_VERIFIER,
      challengeCookie: CHALLENGE_COOKIE,
      challenge,
    });
    expect(vi.mocked(handleAuthProxyRequest)).not.toHaveBeenCalled();
  });

  it('rejects a callback without the Neon session verifier', async () => {
    const challenge = mobilePkceChallenge(VERIFIER);
    const request = new Request(
      `https://wakemyway.vercel.app/api/v1/account/mobile-google-complete?challenge=${challenge}`,
      { headers: { cookie: CHALLENGE_COOKIE } },
    );

    const response = await handleMobileGoogleComplete(request);

    expect(response.status).toBe(400);
    await expect(response.text()).resolves.toContain(
      'Neon Auth did not return a valid session verifier.',
    );
  });

  it('rejects a callback without the original browser challenge cookie', async () => {
    const challenge = mobilePkceChallenge(VERIFIER);
    const request = new Request(
      `https://wakemyway.vercel.app/api/v1/account/mobile-google-complete?` +
        `challenge=${challenge}&neon_auth_session_verifier=${SESSION_VERIFIER}`,
    );

    const response = await handleMobileGoogleComplete(request);

    expect(response.status).toBe(400);
    await expect(response.text()).resolves.toContain(
      'challenge cookie was not returned',
    );
  });

  it('round-trips an encrypted handoff without exposing Neon verifier material', () => {
    const sealed = sealMobileHandoff({
      v: 2,
      sessionVerifier: SESSION_VERIFIER,
      challengeCookie: CHALLENGE_COOKIE,
      challenge: mobilePkceChallenge(VERIFIER),
      exp: Date.now() + 60_000,
    });

    expect(sealed).not.toContain(SESSION_VERIFIER);
    expect(sealed).not.toContain('signed-browser-challenge');
    expect(openMobileHandoff(sealed)).toMatchObject({
      v: 2,
      sessionVerifier: SESSION_VERIFIER,
      challengeCookie: CHALLENGE_COOKIE,
    });
  });

  it('rejects a tampered handoff', () => {
    const sealed = sealMobileHandoff({
      v: 2,
      sessionVerifier: SESSION_VERIFIER,
      challengeCookie: CHALLENGE_COOKIE,
      challenge: mobilePkceChallenge(VERIFIER),
      exp: Date.now() + 60_000,
    });
    const replacement = sealed.endsWith('A') ? 'B' : 'A';
    const tampered = sealed.slice(0, -1) + replacement;

    expect(() => openMobileHandoff(tampered)).toThrow('invalid');
  });

  it('exchanges a valid PKCE-bound handoff for a managed Neon session cookie', async () => {
    const handoff = sealMobileHandoff({
      v: 2,
      sessionVerifier: SESSION_VERIFIER,
      challengeCookie: CHALLENGE_COOKIE,
      challenge: mobilePkceChallenge(VERIFIER),
      exp: Date.now() + 60_000,
    });

    vi.mocked(handleAuthProxyRequest)
      .mockResolvedValueOnce(
        new Response('{}', {
          status: 200,
          headers: {
            'set-cookie':
              `${NEON_AUTH_SESSION_COOKIE_NAME}=${SESSION_TOKEN}; Path=/; HttpOnly; Secure; SameSite=Lax`,
          },
        }),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            session: { id: 'session-id' },
            user: { id: USER_ID, email: 'yotamon@example.test' },
          }),
          {
            status: 200,
            headers: { 'content-type': 'application/json' },
          },
        ),
      );

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
      sessionCookie: `${NEON_AUTH_SESSION_COOKIE_NAME}=${SESSION_TOKEN}`,
      userId: USER_ID,
    });

    expect(handleAuthProxyRequest).toHaveBeenCalledTimes(2);
    const exchangeCall = vi.mocked(handleAuthProxyRequest).mock.calls[0]![0];
    expect(exchangeCall.path).toBe('get-session');
    expect(exchangeCall.request.url).toContain(
      `neon_auth_session_verifier=${SESSION_VERIFIER}`,
    );
    expect(exchangeCall.request.headers.get('cookie')).toBe(CHALLENGE_COOKIE);

    const verifyCall = vi.mocked(handleAuthProxyRequest).mock.calls[1]![0];
    expect(verifyCall.request.headers.get('cookie')).toBe(
      `${NEON_AUTH_SESSION_COOKIE_NAME}=${SESSION_TOKEN}`,
    );
  });

  it('rejects the handoff when the PKCE verifier does not match', async () => {
    const handoff = sealMobileHandoff({
      v: 2,
      sessionVerifier: SESSION_VERIFIER,
      challengeCookie: CHALLENGE_COOKIE,
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
    expect(handleAuthProxyRequest).not.toHaveBeenCalled();
  });

  it('rejects an expired handoff before contacting Neon', async () => {
    const handoff = sealMobileHandoff({
      v: 2,
      sessionVerifier: SESSION_VERIFIER,
      challengeCookie: CHALLENGE_COOKIE,
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
    expect(handleAuthProxyRequest).not.toHaveBeenCalled();
  });
});
