import { afterEach, describe, expect, it } from 'vitest';

import publicRoute from '../api/public/[page]';

afterEach(() => {
  delete process.env.WMW_PUBLIC_SUPPORT_EMAIL;
});

describe('consolidated public route', () => {
  it('handles both public rewrite URLs and internal dynamic destinations', async () => {
    process.env.WMW_PUBLIC_SUPPORT_EMAIL = 'help@example.com';

    for (const url of [
      'https://wakemyway.vercel.app/privacy',
      'https://wakemyway.vercel.app/api/public/privacy',
      'https://wakemyway.vercel.app/support',
      'https://wakemyway.vercel.app/api/public/support',
      'https://wakemyway.vercel.app/.well-known/assetlinks.json',
      'https://wakemyway.vercel.app/api/public/assetlinks',
      'https://wakemyway.vercel.app/auth/mobile',
      'https://wakemyway.vercel.app/api/public/mobile-auth',
    ]) {
      const response = await publicRoute.fetch(new Request(url));
      expect(response.status, url).toBe(200);
    }
  });

  it('rejects unsupported paths and non-GET methods', async () => {
    const missing = await publicRoute.fetch(
      new Request('https://wakemyway.vercel.app/api/public/unknown'),
    );
    expect(missing.status).toBe(404);

    const method = await publicRoute.fetch(
      new Request('https://wakemyway.vercel.app/auth/mobile', { method: 'POST' }),
    );
    expect(method.status).toBe(405);
    expect(method.headers.get('allow')).toBe('GET');
  });
});
