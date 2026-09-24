import { afterEach, describe, expect, it } from 'vitest';

import aiRoute from '../api/internal/ai/[operation]';

const OPERATIONS = ['text', 'stream', 'embed', 'speech', 'transcribe', 'realtime-token'] as const;

describe('consolidated internal AI route', () => {
  it('keeps unsupported operations out of the shared function', async () => {
    const response = await aiRoute.fetch(
      new Request('https://example.test/api/internal/ai/not-real', {
        method: 'POST',
      }),
    );

    expect(response.status).toBe(404);
    expect(await response.json()).toMatchObject({
      error: 'AI operation was not found.',
    });
  });

  it('keeps the existing POST-only contract', async () => {
    const response = await aiRoute.fetch(
      new Request('https://example.test/api/internal/ai/text', {
        method: 'GET',
      }),
    );

    expect(response.status).toBe(405);
    expect(response.headers.get('allow')).toBe('POST');
  });

  describe('internal authorization', () => {
    const configuredKey = 'internal-key-0123456789abcdef0123456789abcdef';

    afterEach(() => {
      delete process.env.WMW_INTERNAL_API_KEY;
    });

    it('rejects every operation without internal credentials', async () => {
      process.env.WMW_INTERNAL_API_KEY = configuredKey;

      for (const operation of OPERATIONS) {
        const response = await aiRoute.fetch(
          new Request(`https://example.test/api/internal/ai/${operation}`, {
            method: 'POST',
          }),
        );

        expect(response.status, operation).toBe(401);
      }
    });

    it('rejects a wrong internal credential', async () => {
      process.env.WMW_INTERNAL_API_KEY = configuredKey;

      const response = await aiRoute.fetch(
        new Request('https://example.test/api/internal/ai/text', {
          method: 'POST',
          headers: { authorization: 'Bearer wrong-key' },
          body: JSON.stringify({ prompt: 'hello' }),
        }),
      );

      expect(response.status).toBe(401);
    });

    it('fails closed with 503 when internal authentication is not configured', async () => {
      const response = await aiRoute.fetch(
        new Request('https://example.test/api/internal/ai/text', {
          method: 'POST',
        }),
      );

      expect(response.status).toBe(503);
      expect(await response.json()).toMatchObject({
        error: 'Internal API authentication is not configured.',
      });
    });
  });
});
