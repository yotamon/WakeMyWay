import { describe, expect, it } from 'vitest';

import {
  privacyPage,
  publicPageResponse,
  requirePublicSupportConfig,
  supportPage,
} from '../src/public/public-pages';

describe('public privacy and support pages', () => {
  it('fails closed without a real support contact', async () => {
    expect(() => requirePublicSupportConfig({})).toThrowError(
      'WakeMyWay public support contact is not configured.',
    );
    expect(() =>
      requirePublicSupportConfig({ WMW_PUBLIC_SUPPORT_EMAIL: 'not-an-email' }),
    ).toThrowError('WakeMyWay public support contact is invalid.');

    const response = publicPageResponse('privacy', {});
    expect(response.status).toBe(503);
    expect(response.headers.get('cache-control')).toBe('no-store');
    expect(await response.text()).not.toContain('support@');
  });

  it('publishes configured privacy copy without analytics or private-data collection claims', async () => {
    const environment = { WMW_PUBLIC_SUPPORT_EMAIL: 'help@example.com' };
    const response = publicPageResponse('privacy', environment);

    expect(response.status).toBe(200);
    expect(response.headers.get('content-type')).toContain('text/html');
    expect(response.headers.get('content-security-policy')).toContain("default-src 'none'");

    const body = await response.text();
    expect(body).toContain('WakeMyWay Privacy Policy');
    expect(body).toContain('help@example.com');
    expect(body).toContain('local-first alarm');
    expect(body).toContain('does not archive raw microphone audio');
    expect(body).toContain('Google Play subscriptions');
    expect(body).toContain('SHA-256 token digests');
    expect(body).toContain('not a medical device');
    expect(body).not.toContain('<script');
    expect(body).not.toContain('session replay');
  });

  it('publishes support guidance that asks only for privacy-safe diagnostics', async () => {
    const response = publicPageResponse('support', {
      WMW_PUBLIC_SUPPORT_EMAIL: 'help@example.com',
    });

    expect(response.status).toBe(200);
    const body = await response.text();
    expect(body).toContain('WakeMyWay Support');
    expect(body).toContain('reliability report');
    expect(body).toContain('do not send raw microphone recordings');
    expect(body).toContain('complete card number');
    expect(body).toContain('href="/privacy"');
  });

  it('escapes configured contact before rendering it into HTML', () => {
    const config = {
      supportEmail: 'help@example.com&quot;<script>alert(1)</script>',
    };

    expect(privacyPage(config)).not.toContain('<script>alert(1)</script>');
    expect(supportPage(config)).not.toContain('<script>alert(1)</script>');
  });
});
