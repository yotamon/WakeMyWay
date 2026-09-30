import { createHash } from 'node:crypto';

import { errorResponse, json, methodNotAllowed, requestId } from '../http.js';
import {
  createAccountWakeRealtimeClientSecret,
  type AccountWakeRealtimeClientSecret,
} from '../voice-spike/direct-openai.js';
import { requireAccountIdentity, type AccountTokenVerifier } from './auth.js';

type MintRealtimeSecret = (options: {
  safetyIdentifier: string;
}) => Promise<AccountWakeRealtimeClientSecret>;

interface RealtimeHandlerOptions {
  verifier?: AccountTokenVerifier;
  mintSecret?: MintRealtimeSecret;
}

export async function handleAccountRealtimeTokenRequest(
  request: Request,
  options: RealtimeHandlerOptions = {},
): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

  try {
    const identity = await requireAccountIdentity(request, options.verifier);
    const safetyIdentifier = accountRealtimeSafetyIdentifier(identity.userId);
    const secret = await (options.mintSecret ?? createAccountWakeRealtimeClientSecret)({
      safetyIdentifier,
    });
    return json(secret, 200, id);
  } catch (error) {
    return errorResponse(error, id, 'account.realtime-token');
  }
}

export function accountRealtimeSafetyIdentifier(userId: string): string {
  const digest = createHash('sha256')
    .update('wakemyway-account-realtime-v1:')
    .update(userId)
    .digest('hex');
  return `wmw_${digest.slice(0, 60)}`;
}
