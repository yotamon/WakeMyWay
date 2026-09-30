import { z } from 'zod';

import { errorResponse, HttpError, json, methodNotAllowed, parseJson, requestId } from '../http.js';
import {
  createAccountWakeRealtimeClientSecret,
  type AccountWakeRealtimeClientSecret,
} from '../voice-spike/direct-openai.js';
import { requireAccountIdentity, type AccountTokenVerifier } from './auth.js';
import {
  accountRealtimePseudonym,
  accountRealtimeSafetyIdentifier,
  issueAccountRealtimeCredential,
  verifyAccountRealtimeCredential,
  type AccountRealtimeCredential,
} from './account-realtime-auth.js';
import {
  PostgresAccountRealtimeDeviceStore,
  type AccountRealtimeDeviceStore,
} from './realtime-device-store.js';

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
  deviceStore?: AccountRealtimeDeviceStore;
}

interface TokenOptions {
  mintSecret?: MintRealtimeSecret;
  verifyCredential?: typeof verifyAccountRealtimeCredential;
  deviceStore?: AccountRealtimeDeviceStore;
}

export async function handleAccountRealtimeProvisionRequest(
  request: Request,
  options: ProvisionOptions = {},
): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'POST' && request.method !== 'DELETE') {
    return methodNotAllowed(['POST', 'DELETE'], id);
  }

  try {
    const identity = await requireAccountIdentity(request, options.verifier);
    const body = await parseJson(request, provisionBodySchema, 4 * 1024);
    const deviceStore = options.deviceStore ?? new PostgresAccountRealtimeDeviceStore();

    if (request.method === 'DELETE') {
      await deviceStore.revoke(identity.userId, body.installationId);
      return json({ revoked: true }, 200, id);
    }

    const credential = (options.issueCredential ?? issueAccountRealtimeCredential)(
      identity.userId,
      body.installationId,
    );
    await deviceStore.activate({
      credentialId: credential.credentialId,
      accountId: identity.userId,
      accountPseudonym: accountRealtimePseudonym(identity.userId),
      installationId: body.installationId,
      expiresAt: credential.expiresAt,
    });

    return json(
      {
        deviceToken: credential.deviceToken,
        expiresAt: credential.expiresAt,
      },
      200,
      id,
    );
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
    if (!authorization) throw new HttpError(401, 'Unauthorized.');

    const credential = (options.verifyCredential ?? verifyAccountRealtimeCredential)(authorization);
    const deviceStore = options.deviceStore ?? new PostgresAccountRealtimeDeviceStore();
    await deviceStore.authorize(credential);

    const safetyIdentifier = accountRealtimeSafetyIdentifier(credential);
    const secret = await (options.mintSecret ?? createAccountWakeRealtimeClientSecret)({
      safetyIdentifier,
    });
    return json(secret, 200, id);
  } catch (error) {
    return errorResponse(error, id, 'account.realtime-token');
  }
}
