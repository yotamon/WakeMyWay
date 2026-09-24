import { handlePlayRtdnRequest } from '../../../src/commerce/play-rtdn-handler.js';

export default {
  async fetch(request: Request): Promise<Response> {
    return handlePlayRtdnRequest(request);
  },
};
