import { describe, expect, it } from 'vitest';

import { parseAIConfig, requireNonZdrAudioSpike } from '../src/ai/config';
import { privateGatewayRouting } from '../src/ai/platform';
import { isInternallyAuthorized, requestId, secureEqual } from '../src/http';

describe('AI configuration', () => {
  it('has deliberate cross-provider defaults and disables non-ZDR audio by default', () => {
    const config = parseAIConfig({});

    expect(config.fast.primary).toBe('openai/gpt-5.4-mini');
    expect(config.fast.fallbacks).toEqual([
      'anthropic/claude-haiku-4.5',
      'google/gemini-3.5-flash',
    ]);
    expect(config.smart.primary).toBe('anthropic/claude-sonnet-4.6');
    expect(config.realtimeModel).toBe('openai/gpt-realtime-2.1');
    expect(config.allowNonZdrAudioSpikes).toBe(false);
  });

  it('deduplicates fallbacks and never retries the primary as a fallback', () => {
    const config = parseAIConfig({
      WMW_AI_FAST_MODEL: 'openai/gpt-5.4-mini',
      WMW_AI_FAST_FALLBACKS:
        'openai/gpt-5.4-mini, anthropic/claude-haiku-4.5, anthropic/claude-haiku-4.5',
    });

    expect(config.fast.fallbacks).toEqual(['anthropic/claude-haiku-4.5']);
  });

  it('builds private Gateway routing with ZDR and no prompt training', () => {
    expect(
      privateGatewayRouting({
        primary: 'openai/gpt-5.4-mini',
        fallbacks: ['anthropic/claude-haiku-4.5'],
      }),
    ).toEqual({
      gateway: {
        zeroDataRetention: true,
        disallowPromptTraining: true,
        models: ['anthropic/claude-haiku-4.5'],
      },
    });
  });

  it('requires an explicit opt-in before non-ZDR audio/realtime spikes', () => {
    const disabled = parseAIConfig({ AI_GATEWAY_API_KEY: 'gateway-test-key' });
    expect(() => requireNonZdrAudioSpike(disabled)).toThrow(/disabled/i);

    const enabled = parseAIConfig({
      AI_GATEWAY_API_KEY: 'gateway-test-key',
      WMW_ENABLE_NON_ZDR_AUDIO_SPIKES: 'true',
    });
    expect(requireNonZdrAudioSpike(enabled).allowNonZdrAudioSpikes).toBe(true);
  });
});

describe('internal HTTP boundary', () => {
  const secret = '0123456789abcdef0123456789abcdef';

  it('uses bearer authentication and rejects wrong tokens', () => {
    const authorized = new Request('https://example.test', {
      headers: { authorization: `Bearer ${secret}` },
    });
    const rejected = new Request('https://example.test', {
      headers: { authorization: 'Bearer wrong' },
    });

    expect(isInternallyAuthorized(authorized, secret)).toBe(true);
    expect(isInternallyAuthorized(rejected, secret)).toBe(false);
    expect(secureEqual(secret, secret)).toBe(true);
  });

  it('accepts only bounded request IDs and otherwise creates one', () => {
    const supplied = new Request('https://example.test', {
      headers: { 'x-request-id': 'wake:123.test' },
    });
    const policyInvalidButHttpValid = new Request('https://example.test', {
      headers: { 'x-request-id': 'contains spaces' },
    });

    expect(requestId(supplied)).toBe('wake:123.test');
    expect(requestId(policyInvalidButHttpValid)).toMatch(/^[0-9a-f-]{36}$/);
  });
});
