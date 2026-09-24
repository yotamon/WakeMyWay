import { publicPageResponse } from '../../src/public/public-pages.js';

export default {
  async fetch(): Promise<Response> {
    return publicPageResponse('privacy');
  },
};
