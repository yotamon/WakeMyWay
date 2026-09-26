import { handleAccountRequest } from '../../../src/account/router.js';

export default {
  async fetch(request: Request): Promise<Response> {
    return handleAccountRequest(request);
  },
};
