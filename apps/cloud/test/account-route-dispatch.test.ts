import { describe, expect, it } from 'vitest';

import accountRoute from '../api/v1/account/[operation]';

describe('consolidated account route', () => {
  it('keeps unknown account operations out of the shared function', async () => {
    const response = await accountRoute.fetch(
      new Request('https://wakemyway.vercel.app/api/v1/account/not-real'),
    );

    expect(response.status).toBe(404);
    await expect(response.json()).resolves.toMatchObject({
      error: 'Account operation was not found.',
    });
  });

  it('preserves authentication on the account me endpoint', async () => {
    const response = await accountRoute.fetch(
      new Request('https://wakemyway.vercel.app/api/v1/account/me'),
    );

    expect(response.status).toBe(401);
  });

  it('preserves authentication on the backup endpoint', async () => {
    const response = await accountRoute.fetch(
      new Request('https://wakemyway.vercel.app/api/v1/account/backup'),
    );

    expect(response.status).toBe(401);
  });

  it('rejects a malformed mobile OAuth start challenge before contacting Neon', async () => {
    const response = await accountRoute.fetch(
      new Request(
        'https://wakemyway.vercel.app/api/v1/account/mobile-google-start?challenge=bad',
      ),
    );

    expect(response.status).toBe(400);
  });

  it('keeps the mobile handoff exchange POST-only', async () => {
    const response = await accountRoute.fetch(
      new Request(
        'https://wakemyway.vercel.app/api/v1/account/mobile-google-exchange',
      ),
    );

    expect(response.status).toBe(405);
    expect(response.headers.get('allow')).toBe('POST');
  });
});
