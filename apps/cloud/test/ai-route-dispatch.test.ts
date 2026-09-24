import { describe, expect, it } from 'vitest';

import aiRoute from '../api/internal/ai/[operation]';

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
});
