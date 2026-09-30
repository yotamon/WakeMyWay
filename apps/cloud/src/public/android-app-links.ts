import { HttpError } from '../http.js';

const PACKAGE_NAME = 'com.wakemyway.app';
const SHA256_HEX = /^[A-F0-9]{64}$/;

export interface AndroidAppLinksConfig {
  fingerprints: string[];
}

export function requireAndroidAppLinksConfig(
  environment: NodeJS.ProcessEnv = process.env,
): AndroidAppLinksConfig {
  const raw = environment.WMW_ANDROID_APP_LINK_CERT_SHA256?.trim();
  if (!raw) {
    throw new HttpError(503, 'WakeMyWay Android App Link certificate fingerprints are not configured.');
  }

  const fingerprints = [...new Set(
    raw
      .split(',')
      .map(value => normalizeFingerprint(value))
      .filter((value): value is string => value !== null),
  )];

  if (fingerprints.length === 0) {
    throw new HttpError(503, 'WakeMyWay Android App Link certificate fingerprints are invalid.');
  }
  return { fingerprints };
}

export function androidAssetLinksDocument(config: AndroidAppLinksConfig): unknown[] {
  return [
    {
      relation: ['delegate_permission/common.handle_all_urls'],
      target: {
        namespace: 'android_app',
        package_name: PACKAGE_NAME,
        sha256_cert_fingerprints: config.fingerprints,
      },
    },
  ];
}

export function androidAssetLinksResponse(
  environment: NodeJS.ProcessEnv = process.env,
): Response {
  try {
    const config = requireAndroidAppLinksConfig(environment);
    return new Response(JSON.stringify(androidAssetLinksDocument(config)), {
      status: 200,
      headers: {
        'content-type': 'application/json; charset=utf-8',
        'cache-control': 'public, max-age=300, stale-while-revalidate=3600',
        'x-content-type-options': 'nosniff',
      },
    });
  } catch (error) {
    const status = error instanceof HttpError ? error.status : 503;
    return new Response(JSON.stringify({ error: 'Android App Links are not configured.' }), {
      status,
      headers: {
        'content-type': 'application/json; charset=utf-8',
        'cache-control': 'no-store',
        'x-content-type-options': 'nosniff',
      },
    });
  }
}

function normalizeFingerprint(value: string): string | null {
  const compact = value.trim().replaceAll(':', '').toUpperCase();
  if (!SHA256_HEX.test(compact)) return null;
  return compact.match(/.{2}/g)?.join(':') ?? null;
}
