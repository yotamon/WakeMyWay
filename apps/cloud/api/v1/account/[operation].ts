import { handleAccountBackupRequest } from '../../../src/account/backup-handler.js';
import { handleAccountMeRequest } from '../../../src/account/me-handler.js';
import {
  handleAccountRealtimeProvisionRequest,
  handleAccountRealtimeTokenRequest,
} from '../../../src/account/realtime-handler.js';
import {
  handleMobileGoogleComplete,
  handleMobileGoogleExchange,
  handleMobileGoogleStart,
} from '../../../src/account/mobile-oauth.js';
import { json, requestId } from '../../../src/http.js';

const OPERATIONS = new Set([
  'me',
  'backup',
  'mobile-google-start',
  'mobile-google-complete',
  'mobile-google-exchange',
  'realtime-token',
  'realtime-provision',
]);

export default {
  async fetch(request: Request): Promise<Response> {
    const operation = operationFromRequest(request);
    if (!operation || !OPERATIONS.has(operation)) {
      const id = requestId(request);
      return json({ error: 'Account operation was not found.', requestId: id }, 404, id);
    }

    switch (operation) {
      case 'me':
        return handleAccountMeRequest(request);
      case 'backup':
        return handleAccountBackupRequest(request);
      case 'mobile-google-start':
        return handleMobileGoogleStart(request);
      case 'mobile-google-complete':
        return handleMobileGoogleComplete(request);
      case 'mobile-google-exchange':
        return handleMobileGoogleExchange(request);
      case 'realtime-token':
        return handleAccountRealtimeTokenRequest(request);
      case 'realtime-provision':
        return handleAccountRealtimeProvisionRequest(request);
      default: {
        const id = requestId(request);
        return json({ error: 'Account operation was not found.', requestId: id }, 404, id);
      }
    }
  },
};

function operationFromRequest(request: Request): string | undefined {
  const pathname = new URL(request.url).pathname.replace(/\/+$/, '');
  return pathname.split('/').filter(Boolean).at(-1);
}
