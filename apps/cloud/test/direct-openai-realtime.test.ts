import { describe, expect, it } from 'vitest';

import { HttpError } from '../src/http';
import {
  createDirectOpenAiRealtimeClientSecret,
  DIRECT_OPENAI_CONFIGURATION_ID,
  OPENAI_REALTIME_CALLS_URL,
  parseDirectOpenAiRealtimeConfig,
  requireDirectOpenAiRealtimeSpike,
} from '../src/voice-spike/direct-openai';

const enabledEnvironment: NodeJS.ProcessEnv = {
  OPENAI_API_KEY: 'server-only-openai-key',
  WMW_ENABLE_DIRECT_OPENAI_REALTIME_SPIKE: 'true',
  WMW_OPENAI_REALTIME_MODEL: 'gpt-realtime-2.1',
  WMW_OPENAI_REALTIME_VOICE: 'marin',
  WMW_OPENAI_SAFETY_IDENTIFIER: 'sha256:synthetic-operator-fixture',
};

describe('direct OpenAI realtime spike configuration', () => {
  it('is disabled and unconfigured by default', () => {
    const config = parseDirectOpenAiRealtimeConfig({});

    expect(config.configured).toBe(false);
    expect(config.enabled).toBe(false);
    expect(config.model).toBe('gpt-realtime-2.1');
    expect(config.voice).toBe('marin');
  });

  it('fails closed unless the explicit gate, server key, and safety identifier exist', () => {
    expect(() => requireDirectOpenAiRealtimeSpike(parseDirectOpenAiRealtimeConfig({}))).toThrow(
      HttpError,
    );

    expect(() =>
      requireDirectOpenAiRealtimeSpike(
        parseDirectOpenAiRealtimeConfig({
          OPENAI_API_KEY: 'key',
          WMW_ENABLE_DIRECT_OPENAI_REALTIME_SPIKE: 'true',
        }),
      ),
    ).toThrow(/safety identifier/i);
  });
});

describe('direct OpenAI realtime client-secret broker', () => {
  it('mints a bounded WebRTC ephemeral token without exposing the standard API key', async () => {
    let requestedUrl: string | null = null;
    let requestedInit: RequestInit | undefined;

    const result = await createDirectOpenAiRealtimeClientSecret({
      environment: enabledEnvironment,
      fetchImpl: async (input, init) => {
        requestedUrl = String(input);
        requestedInit = init;
        return Response.json({
          value: 'ephemeral-client-secret',
          expires_at: 2_000_000_000,
        });
      },
    });

    expect(requestedUrl).toBe('https://api.openai.com/v1/realtime/client_secrets');
    expect(requestedInit?.method).toBe('POST');

    const headers = new Headers(requestedInit?.headers);
    expect(headers.get('authorization')).toBe('Bearer server-only-openai-key');
    expect(headers.get('content-type')).toBe('application/json');
    expect(headers.get('openai-safety-identifier')).toBe('sha256:synthetic-operator-fixture');

    const body = JSON.parse(String(requestedInit?.body)) as {
      session: {
        type: string;
        model: string;
        audio: { output: { voice: string } };
      };
    };
    expect(body).toEqual({
      session: {
        type: 'realtime',
        model: 'gpt-realtime-2.1',
        audio: { output: { voice: 'marin' } },
      },
    });

    expect(result).toEqual({
      candidate: 'direct-openai',
      configurationId: DIRECT_OPENAI_CONFIGURATION_ID,
      connectionMode: 'webrtc-ephemeral',
      token: 'ephemeral-client-secret',
      expiresAt: 2_000_000_000,
      realtimeCallsUrl: OPENAI_REALTIME_CALLS_URL,
      model: 'gpt-realtime-2.1',
      voice: 'marin',
      privacyEligibility: 'synthetic-only',
      authority: 'speech-enrichment-only',
    });
    expect(JSON.stringify(result)).not.toContain('server-only-openai-key');
  });

  it('does not surface provider error bodies', async () => {
    await expect(
      createDirectOpenAiRealtimeClientSecret({
        environment: enabledEnvironment,
        fetchImpl: async () =>
          new Response('{"error":"sensitive-provider-detail"}', {
            status: 401,
            headers: { 'content-type': 'application/json' },
          }),
      }),
    ).rejects.toThrow('HTTP 401');

    try {
      await createDirectOpenAiRealtimeClientSecret({
        environment: enabledEnvironment,
        fetchImpl: async () =>
          new Response('{"error":"sensitive-provider-detail"}', { status: 401 }),
      });
    } catch (error) {
      expect(String(error)).not.toContain('sensitive-provider-detail');
    }
  });

  it('rejects malformed client-secret responses', async () => {
    await expect(
      createDirectOpenAiRealtimeClientSecret({
        environment: enabledEnvironment,
        fetchImpl: async () => Response.json({ expires_at: 2_000_000_000 }),
      }),
    ).rejects.toThrow(/malformed/i);
  });
});
