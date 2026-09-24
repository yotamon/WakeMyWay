import { handlePlayVerificationRequest } from '../../../src/commerce/play-verification-handler.js';

export default {
  async fetch(request: Request): Promise<Response> {
    return handlePlayVerificationRequest(request);
  },
};
