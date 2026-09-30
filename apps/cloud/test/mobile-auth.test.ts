import { describe, expect, it } from 'vitest';

import { mobileAuthFallbackResponse } from '../src/public/mobile-auth';

describe('mobile OAuth browser fallback', () => {
  it('keeps the handoff client-side and bridges legacy installs without network submission', async () => {
    const response = mobileAuthFallbackResponse();

    expect(response.status).toBe(200);
    expect(response.headers.get('cache-control')).toBe('no-store');
    expect(response.headers.get('referrer-policy')).toBe('no-referrer');

    const body = await response.text();
    expect(body).toContain('window.location.hash');
    expect(body).toContain('wakemyway://auth?handoff=');
    expect(body).not.toContain('fetch(');
    expect(body).not.toContain('XMLHttpRequest');
  });
});
