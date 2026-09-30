import { describe, expect, it } from 'vitest';

import {
  androidAssetLinksDocument,
  androidAssetLinksResponse,
  requireAndroidAppLinksConfig,
} from '../src/public/android-app-links';

const DIRECT_FINGERPRINT =
  '3F:32:D8:46:80:FD:22:AA:BC:F1:50:8A:A3:C6:51:9F:FC:F3:4E:B9:FB:21:C0:C0:B4:C8:86:A4:DC:F0:49:BC';

describe('Android App Links association', () => {
  it('publishes the pinned Direct production signing identity without external configuration', async () => {
    const config = requireAndroidAppLinksConfig({});
    expect(config.fingerprints).toEqual([DIRECT_FINGERPRINT]);

    const response = androidAssetLinksResponse({});
    expect(response.status).toBe(200);
    expect(response.headers.get('content-type')).toContain('application/json');
  });

  it('adds valid future signing fingerprints and rejects malformed configuration', async () => {
    const extra = Array.from({ length: 32 }, (_, index) =>
      index.toString(16).padStart(2, '0'),
    ).join(':');

    const config = requireAndroidAppLinksConfig({
      WMW_ANDROID_APP_LINK_CERT_SHA256: extra,
    });
    expect(config.fingerprints).toEqual([DIRECT_FINGERPRINT, extra.toUpperCase()]);

    expect(() =>
      requireAndroidAppLinksConfig({ WMW_ANDROID_APP_LINK_CERT_SHA256: 'not-a-sha256' }),
    ).toThrow();

    const invalid = androidAssetLinksResponse({
      WMW_ANDROID_APP_LINK_CERT_SHA256: 'not-a-sha256',
    });
    expect(invalid.status).toBe(503);
    expect(invalid.headers.get('cache-control')).toBe('no-store');
  });

  it('publishes the standard package association document', () => {
    const config = requireAndroidAppLinksConfig({});
    expect(androidAssetLinksDocument(config)).toEqual([
      {
        relation: ['delegate_permission/common.handle_all_urls'],
        target: {
          namespace: 'android_app',
          package_name: 'com.wakemyway.app',
          sha256_cert_fingerprints: [DIRECT_FINGERPRINT],
        },
      },
    ]);
  });
});
