import { handleAccountBackupRequest } from './backup-handler.js';
import {
  handleMobileAuthExchange,
  handleMobileGoogleCallback,
  handleMobileGoogleStart,
} from './mobile-auth-handler.js';
import { handleAccountMeRequest } from './me-handler.js';
import { json, requestId } from '../http.js';

export async function handleAccountRequest(request: Request): Promise<Response> {
  const pathname = new URL(request.url).pathname;
  const prefix = '/api/v1/account/';
  const route = pathname.startsWith(prefix) ? pathname.slice(prefix.length).replace(/\/+$/, '') : '';

  switch (route) {
    case 'me':
      return handleAccountMeRequest(request);
    case 'backup':
      return handleAccountBackupRequest(request);
    case 'auth/mobile/google/start':
      return handleMobileGoogleStart(request);
    case 'auth/mobile/callback':
      return handleMobileGoogleCallback(request);
    case 'auth/mobile/exchange':
      return handleMobileAuthExchange(request);
    default:
      return json({ error: 'Not found.', requestId: requestId(request) }, 404);
  }
}
