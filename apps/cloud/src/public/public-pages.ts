import { HttpError } from '../http.js';

export interface PublicSupportConfig {
  supportEmail: string;
}

export function requirePublicSupportConfig(
  environment: NodeJS.ProcessEnv = process.env,
): PublicSupportConfig {
  const supportEmail = environment.WMW_PUBLIC_SUPPORT_EMAIL?.trim().toLowerCase();
  if (!supportEmail) {
    throw new HttpError(503, 'WakeMyWay public support contact is not configured.');
  }
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(supportEmail)) {
    throw new HttpError(503, 'WakeMyWay public support contact is invalid.');
  }
  return { supportEmail };
}

export function privacyPage(config: PublicSupportConfig): string {
  const email = escapeHtml(config.supportEmail);
  return htmlDocument(
    'WakeMyWay Privacy Policy',
    `
      <h1>WakeMyWay Privacy Policy</h1>
      <p class="lede">WakeMyWay is built around a local-first alarm. The alarm itself does not depend on an internet connection to ring.</p>

      <h2>What stays on your device by default</h2>
      <p>Your alarm schedule, preferences, wake history, calibration outcomes and learned wake strategy are stored locally by default.</p>
      <p>WakeMyWay does not archive raw microphone audio, full voice transcripts or continuous raw motion-sensor streams as part of the normal 1.0 wake experience.</p>

      <h2>Microphone and motion</h2>
      <p>If you enable voice behavior, microphone access is used for that interaction. Raw microphone audio is not retained by WakeMyWay in the normal consumer wake path.</p>
      <p>Device motion may be processed locally to derive bounded wake evidence such as pickup, orientation or movement. Continuous raw accelerometer or gyroscope traces are not retained by default.</p>

      <h2>Diagnostics</h2>
      <p>WakeMyWay keeps a bounded local reliability journal with technical alarm lifecycle information. Sharing that report with support is initiated by you.</p>
      <p>The report is designed not to contain raw audio, full transcripts, Tomorrow Contract text, passwords, tokens or raw high-frequency motion.</p>

      <h2>Subscriptions</h2>
      <p>If Google Play subscriptions are enabled, Google Play provides purchase state to the app and WakeMyWay may send the subscription product ID and purchase token over HTTPS to its server so the purchase can be verified with Google Play.</p>
      <p>WakeMyWay does not receive or request your full payment-card details. Raw purchase tokens are treated as sensitive credentials, are not written to normal application logs, and the server lifecycle ledger stores only SHA-256 token digests.</p>

      <h2>Accounts and cloud backup</h2>
      <p>WakeMyWay does not require an account for the core local alarm. If optional account backup is enabled in a future consumer release, this policy will be updated before that data flow is made available.</p>

      <h2>Android and Google Play</h2>
      <p>Google Play and the Android operating system may process app-distribution, billing and platform diagnostic information under their own terms and policies.</p>

      <h2>Medical disclaimer</h2>
      <p>WakeMyWay is a behavioral alarm product. It is not a medical device and does not diagnose, monitor or treat sleep disorders or medically verify wakefulness.</p>

      <h2>Contact</h2>
      <p>Privacy and support questions: <a href="mailto:${email}">${email}</a></p>

      <p class="meta">Effective date: September 24, 2026.</p>
    `,
  );
}

export function supportPage(config: PublicSupportConfig): string {
  const email = escapeHtml(config.supportEmail);
  return htmlDocument(
    'WakeMyWay Support',
    `
      <h1>WakeMyWay Support</h1>
      <p class="lede">If a wake did not behave as expected, we want the technical evidence needed to understand it without asking for private morning content.</p>

      <h2>Contact</h2>
      <p>Email <a href="mailto:${email}">${email}</a>.</p>

      <h2>When an alarm did not behave as expected</h2>
      <p>Please include your WakeMyWay version, Android version, device model, approximate intended wake time and whether the phone had recently restarted, updated, entered battery saver/Doze, or changed Bluetooth/audio route.</p>
      <p>If available, attach the WakeMyWay reliability report for that occurrence.</p>

      <h2>Do not send</h2>
      <p>Please do not send raw microphone recordings, full transcripts, Tomorrow Contract text, calendar content, passwords, security tokens or full payment-card details.</p>

      <h2>Wake Ready and Android permissions</h2>
      <p>Wake Ready summarizes whether Android has the capabilities WakeMyWay needs for future alarm presentation, such as exact-alarm access and notifications. A permission problem should be resolved before relying on a future wake.</p>

      <h2>Audio and Bluetooth</h2>
      <p>If sound used an unexpected route, note whether headphones, a speaker or another Bluetooth device was connected and whether the route changed before or during the wake.</p>

      <h2>Snooze</h2>
      <p>WakeMyWay commits a replacement wake before ending the current one. If a safe replacement cannot be committed, Snooze is designed to fail closed rather than silently end the alarm.</p>

      <h2>Updates</h2>
      <p>Google Play builds update through Google Play. Founder/direct builds use WakeMyWay's signed direct-update path. Updates must not become authority over an already-active wake.</p>

      <h2>Subscriptions</h2>
      <p>Subscriptions, when available, are purchased and managed through Google Play. WakeMyWay support will never ask for your complete card number or card security code.</p>

      <h2>Privacy</h2>
      <p>Read the <a href="/privacy">WakeMyWay Privacy Policy</a>.</p>
    `,
  );
}

export function publicPageResponse(
  kind: 'privacy' | 'support',
  environment: NodeJS.ProcessEnv = process.env,
): Response {
  try {
    const config = requirePublicSupportConfig(environment);
    const body = kind === 'privacy' ? privacyPage(config) : supportPage(config);
    return new Response(body, {
      status: 200,
      headers: {
        'content-type': 'text/html; charset=utf-8',
        'cache-control': 'public, max-age=300, stale-while-revalidate=3600',
        'content-security-policy':
          "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'",
        'referrer-policy': 'no-referrer',
        'x-content-type-options': 'nosniff',
      },
    });
  } catch (error) {
    const status = error instanceof HttpError ? error.status : 503;
    return new Response(
      htmlDocument(
        'WakeMyWay',
        '<h1>WakeMyWay</h1><p>This public information page is not configured yet.</p>',
      ),
      {
        status,
        headers: {
          'content-type': 'text/html; charset=utf-8',
          'cache-control': 'no-store',
          'content-security-policy':
            "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'",
          'referrer-policy': 'no-referrer',
          'x-content-type-options': 'nosniff',
        },
      },
    );
  }
}

function htmlDocument(title: string, content: string): string = `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escapeHtml(title)}</title>
  <style>
    :root { color-scheme: light; font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #fffaf4; color: #182033; }
    body { margin: 0; }
    main { max-width: 760px; margin: 0 auto; padding: 48px 24px 72px; }
    h1 { font-size: clamp(2rem, 6vw, 3.5rem); line-height: 1.02; margin: 0 0 20px; letter-spacing: -0.04em; }
    h2 { margin-top: 36px; font-size: 1.2rem; }
    p { line-height: 1.7; }
    .lede { font-size: 1.15rem; max-width: 65ch; }
    .meta { margin-top: 48px; font-size: 0.9rem; color: #4f596f; }
    a { color: #9a3f00; text-underline-offset: 3px; }
    a:focus-visible { outline: 3px solid currentColor; outline-offset: 3px; }
  </style>
</head>
<body>
  <main>${content}</main>
</body>
</html>`;

function escapeHtml(value: string): string =
  value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
