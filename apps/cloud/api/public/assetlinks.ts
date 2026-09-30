import { androidAssetLinksResponse } from '../../src/public/android-app-links.js';

export default {
  async fetch(): Promise<Response> {
    return androidAssetLinksResponse();
  },
};
