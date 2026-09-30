import { androidAssetLinksResponse } from '../../src/public/android-app-links.js';
import { mobileAuthFallbackResponse } from '../../src/public/mobile-auth.js';
import { publicPageResponse } from '../../src/public/public-pages.js';
import { json, requestId } from '../../src/http.js';

type PublicPage = 'privacy' | 'support' | 'assetlinks' | 'mobile-auth';

export default {
  async fetch(request: Request): Promise<Response> {
    const page = pageFromRequest(request);
    if (!page) {
      const id = requestId(request);
      return json({ error: 'Public page was not found.', requestId: id }, 404, id);
    }
    if (request.method !== 'GET') {
      return new Response(null, {
        status: 405,
        headers: { allow: 'GET' },
      });
    }

    switch (page) {
      case 'privacy':
        return publicPageResponse('privacy');
      case 'support':
        return publicPageResponse('support');
      case 'assetlinks':
        return androidAssetLinksResponse();
      case 'mobile-auth':
        return mobileAuthFallbackResponse();
    }
  },
};

function pageFromRequest(request: Request): PublicPage | undefined {
  const pathname = new URL(request.url).pathname.replace(/\/+$/, '');
  if (pathname === '/privacy' || pathname.endsWith('/public/privacy')) return 'privacy';
  if (pathname === '/support' || pathname.endsWith('/public/support')) return 'support';
  if (
    pathname === '/.well-known/assetlinks.json' ||
    pathname.endsWith('/public/assetlinks')
  ) return 'assetlinks';
  if (pathname === '/auth/mobile' || pathname.endsWith('/public/mobile-auth')) {
    return 'mobile-auth';
  }
  return undefined;
}
