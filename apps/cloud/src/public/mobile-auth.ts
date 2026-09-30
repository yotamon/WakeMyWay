export function mobileAuthFallbackResponse(): Response {
  return new Response(
    `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Return to WakeMyWay</title>
  <meta name="referrer" content="no-referrer">
</head>
<body>
  <main>
    <h1>Return to WakeMyWay</h1>
    <p>Finishing sign-in in the app…</p>
    <p>If WakeMyWay does not open automatically, return to the app and start Google sign-in again.</p>
  </main>
  <script>
    (() => {
      const fragment = window.location.hash.startsWith('#') ? window.location.hash.slice(1) : '';
      if (!fragment) return;
      const params = new URLSearchParams(fragment);
      const handoff = params.get('handoff');
      if (!handoff) return;
      window.location.replace('wakemyway://auth?handoff=' + encodeURIComponent(handoff));
    })();
  </script>
</body>
</html>`,
    {
      status: 200,
      headers: {
        'content-type': 'text/html; charset=utf-8',
        'cache-control': 'no-store',
        'content-security-policy':
          "default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'",
        'referrer-policy': 'no-referrer',
        'x-content-type-options': 'nosniff',
      },
    },
  );
}
