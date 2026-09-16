import { handleAccountBackupRequest } from '../../../src/account/backup-handler.js';

export default {
  async fetch(request: Request): Promise<Response> {
    return handleAccountBackupRequest(request);
  },
};
