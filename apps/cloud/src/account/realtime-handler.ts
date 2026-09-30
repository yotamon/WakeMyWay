import { z } from 'zod';

import { errorResponse, json, methodNotAllowed, parseJson, requestId } from '../http.js';
import {
  createAccountWakeRealtimeClientSecret,
  type AccountWakeRealtimeClientSecret,
} from '../voice-spike/direct-openai.js';
import { requireAccountIdentity, type AccountTokenVerifier } from './auth.js';
import {
  accountRealtimeSafetyIdentifier,
  issueAccountRealtimeCredential,
  verifyAccountRealtimeCredential,
  type AccountRealtimeCredential,
} from './account-realtime-auth.js';

const provisionBodySchema = z.object({
  installationId: z.string().uuid(),
});

type MintRealtimeSecret = (options: {
  safetyIdentifier: string;
}) => Promise<AccountWakeRealtimeClientSecret>;

type IssueCredential = (userId: string, installationId: string) => AccountRealtimeCredential;

interface ProvisionOptions {
  verifier?: AccountTokenVerifier;
  issueCredential?: IssueCredential;
}

interface TokenOptions {
  mintSecret?: MintRealtimeSecret;
  verifyCredential?: typeof verifyAccountRealtimeCredential;
}

export async function handleAccountRealtimeProvisionRequest(
  request: Request,
  options: ProvisionOptions = {},
): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

  try {
    const identity = await requireAccountIdentity(request, options.verifier);
    const body = await parseJson(request, provisionBodySchema, 4 * 1024);
    const credential = (options.issueCredential ?? issueAccountRealtimeCredential)(
      identity.userId,
      body.installationId,
    );
    return json(credential, 200, id);
  } catch (error) {
    return errorResponse(error, id, 'account.realtime-provision');
  }
}

export async function handleAccountRealtimeTokenRequest(
  request: Request,
  options: TokenOptions = {},
): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'POST') return methodNotAllowed(['POST'], id);

  try {
    const authorization = request.headers.get('authorization')?.trim().match(/^Bearer\s+([^\s]+)$/i)?.[1];
    if (!authorization) throw new Error('Missing Realtime device credential.');
    const credential = (options.verifyCredential ?? verifyAccountRealtimeCredential)(authorization);
    const safetyIdentifier = accountRealtimeSafetyIdentifier(credential);
    const secret = await (options.mintSecret ?? createAccountWakeRealtimeClientSecret)({
      safetyIdentifier,
    });
    return json(secret, 200, id);
  } catch (error) {
    return errorResponse(error, id, 'account.realtime-token');
  }
}
