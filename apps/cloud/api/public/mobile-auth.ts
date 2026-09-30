import { mobileAuthFallbackResponse } from '../../src/public/mobile-auth.js';

export default {
  async fetch(): Promise<Response> {
    return mobileAuthFallbackResponse();
  },
};
