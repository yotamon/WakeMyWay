import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  handleMobileAuthExchange,
  handleMobileGoogleStart,
} from '../src/account/mobile-auth-handler';
import {
  encryptSessionToken,
  pkceChallenge,
  randomBase64Url,
  sha256Hex,
} from '../src/account/mobile-auth-crypto';
import type {
  MobileAuthHandoffStore,
  StoredMobileAuthHandoff,
} from '../src/account/mobile-auth-store';

const COOKIE_SECRET = 'wake-my-way-test-cookie-secret-at-least-32-chars';
const PUBLIC_BASE = 'https://wakemyway.example';

class MemoryHandoffStore implements MobileAuthHandoffStore {
  private readonly rows = new Map<string, {
    pkceChallenge: string;
    tokenEnvelope: string;
    expiresAt: Date;
  }>();

  async put(input: {
    codeHash: string;
    pkceChallenge: string;
    tokenEnvelope: string;
    expiresAt: Date;
    createdAt: Date;
  }): Promise<void> {
    this.rows.set(input.codeHash, {
      pkceChallenge: input.pkceChallenge,
      tokenEnvelope: input.tokenEnvelope,
      expiresAt: input.expiresAt,
    });
  }

  async consume(
    codeHash: string,
    challenge: string,
    now: Date,
  ): Promise<StoredMobileAuthHandoff | null> {
    const row = this.rows.get(codeHash);
    if (!row || row.pkceChallenge !== challenge || row.expiresAt <= now) return null;
    this.rows.delete(codeHash);
    return { tokenEnvelope: row.tokenEnvelope };
  }
}

beforeEach(() => {
  process.env.NEON_AUTH_BASE_URL = 'https://auth.example.test/neondb/auth';
  process.env.NEON_AUTH_COOKIE_SECRET = COOKIE_SECRET;
  process.env.ACCOUNT_PUBLIC_BASE_URL = PUBLIC_BASE;
});

afterEach(() => {
  vi.unstubAllGlobals();
  delete process.env.NEON_AUTH_BASE_URL;
  delete process.env.NEON_AUTH_COOKIE_SECRET;
  delete process.env.ACCOUNT_PUBLIC_BASE_URL;
});

describe('mobile Neon OAuth handoff', () => {
  it('starts Google OAuth through the official Neon proxy and binds browser PKCE state', async () => {
    let forwardedBody: Record<string, unknown> | undefined;
    vi.stubGlobal('fetch', vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
      forwardedBody = JSON.parse(String(init?.body)) as Record<string, unknown>;
      return new Response(
        JSON.stringify({
          url: 'https://accounts.google.com/o/oauth2/v2/auth?client_id=test',
          redirect: false,
        }),
        {
          status: 200,
          headers: {
            'content-type': 'application/json',
            'set-cookie': '__Secure-neon-auth.session_challenge=challenge; Path=/; HttpOnly; Secure; SameSite=Lax',
          },
        },
      );
    }));

    const challenge = pkceChallenge('v'.repeat(43));
    const response = await handleMobileGoogleStart(
      new Request(
        `${PUBLIC_BASE}/api/v1/account/auth/mobile/google/start?code_challenge=${challenge}`,
      ),
    );

    expect(response.status).toBe(302);
    expect(response.headers.get('location')).toContain('https://accounts.google.com/');
    expect(response.headers.getSetCookie().join('\n')).toContain('__Secure-wmw-mobile-pkce=');
    expect(forwardedBody).toMatchObject({
      provider: 'google',
      disableRedirect: true,
      callbackURL: `${PUBLIC_BASE}/api/v1/account/auth/mobile/callback`,
    });
  });

  it('exchanges a PKCE-bound handoff exactly once', async () => {
    const store = new MemoryHandoffStore();
    const verifier = 'v'.repeat(43);
    const code = randomBase64Url();
    const now = new Date('2026-09-27T00:00:00.000Z');
    await store.put({
      codeHash: sha256Hex(code),
      pkceChallenge: pkceChallenge(verifier),
      tokenEnvelope: encryptSessionToken('neon-session-token', COOKIE_SECRET),
      expiresAt: new Date(now.getTime() + 60_000),
      createdAt: now,
    });

    const request = () => new Request(
      `${PUBLIC_BASE}/api/v1/account/auth/mobile/exchange`,
      {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ code, verifier }),
      },
    );

    const first = await handleMobileAuthExchange(request(), { store, now: () => now });
    expect(first.status).toBe(200);
    await expect(first.json()).resolves.toEqual({ sessionToken: 'neon-session-token' });

    const replay = await handleMobileAuthExchange(request(), { store, now: () => now });
    expect(replay.status).toBe(401);
  });

  it('does not consume a handoff when the PKCE verifier is wrong', async () => {
    const store = new MemoryHandoffStore();
    const correctVerifier = 'c'.repeat(43);
    const code = randomBase64Url();
    const now = new Date('2026-09-27T00:00:00.000Z');
    await store.put({
      codeHash: sha256Hex(code),
      pkceChallenge: pkceChallenge(correctVerifier),
      tokenEnvelope: encryptSessionToken('neon-session-token', COOKIE_SECRET),
      expiresAt: new Date(now.getTime() + 60_000),
      createdAt: now,
    });

    const wrong = await handleMobileAuthExchange(
      new Request(`${PUBLIC_BASE}/api/v1/account/auth/mobile/exchange`, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ code, verifier: 'w'.repeat(43) }),
      }),
      { store, now: () => now },
    );
    expect(wrong.status).toBe(401);

    const correct = await handleMobileAuthExchange(
      new Request(`${PUBLIC_BASE}/api/v1/account/auth/mobile/exchange`, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ code, verifier: correctVerifier }),
      }),
      { store, now: () => now },
    );
    expect(correct.status).toBe(200);
  });
});
