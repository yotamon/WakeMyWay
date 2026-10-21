import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { createLiveSession, liveRequestSchema, liveSessionConfig, OPENAI_LIVE_SESSIONS_URL } from '../src/voice-spike/gpt-live';
import { handleAccountLiveSessionRequest } from '../src/account/live-handler';
import { HttpError } from '../src/http';
import type { AccountRealtimeDeviceStore } from '../src/account/realtime-device-store';

const sdp = 'v=0\r\no=- 123 123 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\na=mid:0\r\n';
const request = { sdp, characterId: 'alfred' as const, voiceStyle: 'default' as const };
const credential = {
  credentialId: '859fc74d-dc3e-47b8-b739-10167a2ef931',
  installationId: '4d19d808-29f1-4ed6-8a74-31a14c9571ab',
  accountPseudonym: 'a'.repeat(40),
  expiresAt: 1_900_000_000,
};

beforeEach(() => {
  vi.stubEnv('WMW_GPT_LIVE_ENABLED', 'true');
  vi.stubEnv('WMW_GPT_LIVE_ALLOWED_ACCOUNT_PSEUDONYMS', 'a'.repeat(40));
});
afterEach(() => vi.unstubAllEnvs());

describe('GPT-Live session configuration', () => {
  it('keeps critical state out of the speech engine and disables recording', () => {
    const config = liveSessionConfig(request);
    expect(config).toMatchObject({
      model: 'gpt-live-1',
      store: false,
      delegation: { type: 'client' },
      audio: { output: { voice: 'vesper' } },
    });
    expect(config.instructions).toContain('WakeRuntime owns wake phases');
    expect(config.instructions).toContain('remain silent');
  });

  it('uses a custom voice ID object rather than a provider name for reviewed voice IDs', () => {
    vi.stubEnv('WMW_GPT_LIVE_VOICE_ALFRED', 'voice_approved_candidate');
    expect(liveSessionConfig(request).audio.output.voice).toEqual({ id: 'voice_approved_candidate' });
    vi.stubEnv('WMW_GPT_LIVE_VOICE_ALFRED', 'marin');
    expect(() => liveSessionConfig(request)).toThrow('misconfigured');
  });

  it('resolves known characters to approved voices only', () => {
    expect(liveSessionConfig({ ...request, characterId: 'luna' }).audio.output.voice).toBe('willow');
    expect(liveRequestSchema.safeParse({ ...request, characterId: 'unknown' }).success).toBe(false);
    expect(liveRequestSchema.safeParse({ ...request, voiceId: 'untrusted-voice' }).success).toBe(false);
  });

  it('rejects malformed, non-audio and oversized SDP', () => {
    expect(liveRequestSchema.safeParse({ ...request, sdp: 'v=0\r\nm=video 9' }).success).toBe(false);
    expect(liveRequestSchema.safeParse({ ...request, sdp: 'v=0' + 'x'.repeat(66000) }).success).toBe(false);
  });

  it('fails closed when the cost-gated experiment is disabled', async () => {
    vi.stubEnv('WMW_GPT_LIVE_ENABLED', 'false');
    const fetchImpl = vi.fn();
    await expect(createLiveSession(request, { apiKey: 'key', fetchImpl, safetyIdentifier: 'wmw_test' }))
      .rejects.toThrow('disabled');
    expect(fetchImpl).not.toHaveBeenCalled();
  });

  it('posts the verified SDP + voice config to Live, not Realtime', async () => {
    vi.stubEnv('WMW_GPT_LIVE_ENABLED', 'true');
    const fetchImpl = vi.fn(async (_url: RequestInfo | URL, _options?: RequestInit) => new Response(JSON.stringify({
      session: { id: 'live_test' },
      transport: { type: 'webrtc', sdp: 'v=0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\na=mid:0\r\n' },
    }), { status: 201 }));
    const result = await createLiveSession(request, {
      apiKey: 'test-key', fetchImpl, safetyIdentifier: 'wmw_test',
    });
    expect(result.session.id).toBe('live_test');
    const [url, options] = fetchImpl.mock.calls[0]!;
    expect(url).toBe(OPENAI_LIVE_SESSIONS_URL);
    const body = JSON.parse(String(options!.body));
    expect(body.session.audio.output.voice).toBe('vesper');
    expect(body.session.store).toBe(false);
    expect(body.transport.sdp).toBe(sdp);
    expect((options!.headers as Record<string, string>).authorization).toBe('Bearer test-key');
  });
});

describe('account-authenticated Live SDP route', () => {
  const store: AccountRealtimeDeviceStore = {
    activate: vi.fn(async () => undefined),
    revoke: vi.fn(async () => undefined),
    authorize: vi.fn(async () => undefined),
  };

  it('rejects unauthenticated and unsupported methods before any OpenAI call', async () => {
    const endpoint = 'https://wakemyway.test/api/v1/account/live-session';
    expect((await handleAccountLiveSessionRequest(new Request(endpoint, { method: 'PUT' }))).status).toBe(405);
    expect((await handleAccountLiveSessionRequest(new Request(endpoint))).status).toBe(401);
    expect((await handleAccountLiveSessionRequest(new Request(endpoint, { method: 'POST' }))).status).toBe(401);
  });

  it('rejects non-allowlisted accounts without a paid admission', async () => {
    vi.stubEnv('WMW_GPT_LIVE_ALLOWED_ACCOUNT_PSEUDONYMS', '');
    const reserve = vi.fn(async () => undefined);
    const mint = vi.fn();
    const response = await handleAccountLiveSessionRequest(
      new Request('https://wakemyway.test/api/v1/account/live-session', {
        method: 'POST',
        headers: { authorization: 'Bearer scoped-device-token', 'content-type': 'application/json' },
        body: JSON.stringify(request),
      }),
      { verifyCredential: () => credential, deviceStore: store,
        pilotAccess: { isAllowed: async () => false },
        admissionStore: { reserve }, mintLiveSession: mint },
    );
    expect(response.status).toBe(403);
    expect(reserve).not.toHaveBeenCalled();
    expect(mint).not.toHaveBeenCalled();
  });

  it('allows a single DB-approved pilot even if global Vercel voice access is off', async () => {
    vi.stubEnv('WMW_GPT_LIVE_ENABLED', 'false');
    vi.stubEnv('WMW_GPT_LIVE_ALLOWED_ACCOUNT_PSEUDONYMS', '');
    const authorize = vi.fn(async () => undefined);
    const pilotCheck = vi.fn(async () => true);
    const reserve = vi.fn(async () => undefined);
    const mint = vi.fn(async (_input: unknown, options: { pilotApproved?: boolean }) => {
      expect(options.pilotApproved).toBe(true);
      return { session: { id: 'live_test' },
        transport: { type: 'webrtc' as const,
          sdp: 'v=0\\r\\nm=audio 9 UDP/TLS/RTP/SAVPF 111\\r\\na=mid:0\\r\\n' } };
    });
    const response = await handleAccountLiveSessionRequest(
      new Request('https://wakemyway.test/api/v1/account/live-session', {
        method: 'POST',
        headers: { authorization: 'Bearer scoped-device-token', 'content-type': 'application/json' },
        body: JSON.stringify(request),
      }),
      { verifyCredential: () => credential, deviceStore: { ...store, authorize },
        pilotAccess: { isAllowed: pilotCheck },
        admissionStore: { reserve }, mintLiveSession: mint },
    );
    expect(response.status).toBe(201);
    expect(pilotCheck).toHaveBeenCalledWith(credential.credentialId);
    expect(authorize.mock.invocationCallOrder[0]!).toBeLessThan(pilotCheck.mock.invocationCallOrder[0]!);
    expect(reserve).toHaveBeenCalledOnce();
    expect(mint).toHaveBeenCalledOnce();
  });

  it('does not reserve a paid admission when the experiment is disabled', async () => {
    vi.stubEnv('WMW_GPT_LIVE_ENABLED', 'false');
    const reserve = vi.fn(async () => undefined);
    const mint = vi.fn();
    const response = await handleAccountLiveSessionRequest(
      new Request('https://wakemyway.test/api/v1/account/live-session', {
        method: 'POST',
        headers: { authorization: 'Bearer scoped-device-token', 'content-type': 'application/json' },
        body: JSON.stringify(request),
      }),
      { verifyCredential: () => credential, deviceStore: store,
        pilotAccess: { isAllowed: async () => false },
        admissionStore: { reserve }, mintLiveSession: mint },
    );
    expect(response.status).toBe(403);
    expect(reserve).not.toHaveBeenCalled();
    expect(mint).not.toHaveBeenCalled();
  });

  it('preflights allowed accounts without reserving any paid Live admission', async () => {
    const reserve = vi.fn();
    const mint = vi.fn();
    const response = await handleAccountLiveSessionRequest(
      new Request('https://wakemyway.test/api/v1/account/live-session', {
        method: 'GET',
        headers: { authorization: 'Bearer scoped-device-token' },
      }),
      { verifyCredential: () => credential, deviceStore: store,
        admissionStore: { reserve }, mintLiveSession: mint },
    );
    expect(response.status).toBe(200);
    expect(await response.json()).toMatchObject({ available: true });
    expect(reserve).not.toHaveBeenCalled();
    expect(mint).not.toHaveBeenCalled();
  });

  it('rejects disabled Live during preflight without using paid admission', async () => {
    vi.stubEnv('WMW_GPT_LIVE_ENABLED', 'false');
    const reserve = vi.fn();
    const response = await handleAccountLiveSessionRequest(
      new Request('https://wakemyway.test/api/v1/account/live-session', {
        method: 'GET',
        headers: { authorization: 'Bearer scoped-device-token' },
      }),
      { verifyCredential: () => credential, deviceStore: store,
        pilotAccess: { isAllowed: async () => false },
        admissionStore: { reserve } },
    );
    expect(response.status).toBe(403);
    expect(reserve).not.toHaveBeenCalled();
  });

  it('authorizes the stored device credential before calling OpenAI', async () => {
    const authorize = vi.fn(async () => undefined);
    const mint = vi.fn(async () => ({ session: { id: 'live_test' },
      transport: { type: 'webrtc' as const, sdp: 'v=0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\na=mid:0\r\n' } }));
    const response = await handleAccountLiveSessionRequest(
      new Request('https://wakemyway.test/api/v1/account/live-session', {
        method: 'POST',
        headers: { authorization: 'Bearer scoped-device-token', 'content-type': 'application/json' },
        body: JSON.stringify(request),
      }),
      { verifyCredential: () => credential, deviceStore: { ...store, authorize }, admissionStore: { reserve: vi.fn(async () => undefined) }, mintLiveSession: mint },
    );
    expect(response.status).toBe(201);
    expect(authorize).toHaveBeenCalledWith(credential);
    expect(authorize.mock.invocationCallOrder[0]!).toBeLessThan(mint.mock.invocationCallOrder[0]!);
    expect(await response.json()).toMatchObject({
      configurationId: 'openai:gpt-live-1:webrtc-client-v1',
      session: { id: 'live_test' },
    });
  });

  it('rejects quota exhaustion without contacting OpenAI', async () => {
    const mint = vi.fn();
    const response = await handleAccountLiveSessionRequest(
      new Request('https://wakemyway.test/api/v1/account/live-session', {
        method: 'POST',
        headers: { authorization: 'Bearer scoped-device-token', 'content-type': 'application/json' },
        body: JSON.stringify(request),
      }),
      { verifyCredential: () => credential, deviceStore: store,
        admissionStore: { reserve: async () => { throw new HttpError(429, 'quota reached'); } },
        mintLiveSession: mint },
    );
    expect(response.status).toBe(429);
    expect(mint).not.toHaveBeenCalled();
  });

  it('does not allocate billable sessions when a credential was revoked', async () => {
    const mint = vi.fn();
    const response = await handleAccountLiveSessionRequest(
      new Request('https://wakemyway.test/api/v1/account/live-session', {
        method: 'POST',
        headers: { authorization: 'Bearer bad-token', 'content-type': 'application/json' },
        body: JSON.stringify(request),
      }),
      { verifyCredential: () => credential, deviceStore: { ...store, authorize: async () => { throw new Error('revoked'); } }, admissionStore: { reserve: vi.fn(async () => undefined) }, mintLiveSession: mint },
    );
    expect(response.status).toBeGreaterThanOrEqual(400);
    expect(mint).not.toHaveBeenCalled();
  });
});
