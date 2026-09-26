import { handleAccountMeRequest } from '../../../src/account/me-handler.js';

export default {
  async fetch(request: Request): Promise<Response> {
    return handleAccountMeRequest(request);
  },
};
