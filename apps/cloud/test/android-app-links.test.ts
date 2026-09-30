import { describe, expect, it } from 'vitest';

import {
  androidAssetLinksDocument,
  androidAssetLinksResponse,
  requireAndroidAppLinksConfig,
} from '../src/public/android-app-links';

describe('Android App Links association', () => {
  it('fails closed when production signing fingerprints are absent', async () => {
    expect(() => requireAndroidAppLinksConfig({})).toThrow();
    const response = androidAssetLinksResponse({});
    expect(response.status).toBe(503);
    expect(response.headers.get('cache-control')).toBe('no-store');
  });

  it('normalizes and publishes one or more SHA-256 signing fingerprints', async () => {
    const first = 'a'.repeat(64);
    const second = Array.from({ length: 32 }, (_, index) =>
      index.toString(16).padStart(2, '0'),
    ).join(':');

    const config = requireAndroidAppLinksConfig({
      WMW_ANDROID_APP_LINK_CERT_SHA256: `${first},${second}`,
    });
    expect(config.fingerprints).toHaveLength(2);
    expect(config.fingerprints[0]).toMatch(/^(?:[A-F0-9]{2}:){31}[A-F0-9]{2}$/);

    const document = androidAssetLinksDocument(config) as Array<Record<string, unknown>>;
    expect(document).toEqual([
      {
        relation: ['delegate_permission/common.handle_all_urls'],
        target: {
          namespace: 'android_app',
          package_name: 'com.wakemyway.app',
          sha256_cert_fingerprints: config.fingerprints,
        },
      },
    ]);

    const response = androidAssetLinksResponse({
      WMW_ANDROID_APP_LINK_CERT_SHA256: first,
    });
    expect(response.status).toBe(200);
    expect(response.headers.get('content-type')).toContain('application/json');
    await expect(response.json()).resolves.toEqual(expect.any(Array));
  });
});
