import { androidAssetLinksResponse } from '../../src/public/android-app-links.js';
import { mobileAuthFallbackResponse } from '../../src/public/mobile-auth.js';
import { publicPageResponse } from '../../src/public/public-pages.js';
import { json, requestId } from '../../src/http.js';

const PAGES = new Set(['privacy', 'support', 'assetlinks', 'mobile-auth']);

export default {
  async fetch(request: Request): Promise<Response> {
    const page = new URL(request.url).pathname.split('/').filter(Boolean).at(-1);
    if (!page || !PAGES.has(page)) {
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
      default: {
        const id = requestId(request);
        return json({ error: 'Public page was not found.', requestId: id }, 404, id);
      }
    }
  },
};
