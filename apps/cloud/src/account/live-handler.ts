import { errorResponse, HttpError, json, methodNotAllowed, parseJson, requestId } from '../http.js';
import { accountRealtimeSafetyIdentifier, verifyAccountRealtimeCredential } from './account-realtime-auth.js';
import { PostgresAccountRealtimeDeviceStore, type AccountRealtimeDeviceStore } from './realtime-device-store.js';
import { PostgresLiveAdmissionStore, type LiveAdmissionStore } from './live-admission-store.js';
import { PostgresLivePilotAccess, type LivePilotAccess } from './live-pilot-store.js';
import { createLiveSession, liveRequestSchema, type LiveSessionAnswer } from '../voice-spike/gpt-live.js';

interface Options {
  verifyCredential?: typeof verifyAccountRealtimeCredential;
  deviceStore?: AccountRealtimeDeviceStore;
  admissionStore?: LiveAdmissionStore;
  pilotAccess?: LivePilotAccess;
  mintLiveSession?: typeof createLiveSession;
}

/**
 * A separate account-authorized SDP broker. Unlike the Realtime token endpoint,
 * GPT-Live session creation is performed on the server with the project key.
 */
export async function handleAccountLiveSessionRequest(
  request: Request,
  options: Options = {},
): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'POST') return methodNotAllowed(['POST'], id);
  try {
    // Authentication happens before parsing the SDP to minimize attack surface.
    const bearer = request.headers.get('authorization')?.trim().match(/^Bearer\s+([^\s]+)$/i)?.[1];
    if (!bearer) throw new HttpError(401, 'Unauthorized.');
    const credential = (options.verifyCredential ?? verifyAccountRealtimeCredential)(bearer);
    await (options.deviceStore ?? new PostgresAccountRealtimeDeviceStore()).authorize(credential);
    const input = await parseJson(request, liveRequestSchema, 72 * 1024);
    // A project owner can activate a single verified pilot account in Neon
    // without granting voice access to all consumers or exposing API keys.
    const allowedPseudonyms = new Set(
      (process.env.WMW_GPT_LIVE_ALLOWED_ACCOUNT_PSEUDONYMS ?? '')
        .split(',')
        .map(value => value.trim().toLowerCase())
        .filter(value => /^[a-f0-9]{40}$/.test(value)),
    );
    const envApproved = process.env.WMW_GPT_LIVE_ENABLED === 'true'
      && allowedPseudonyms.has(credential.accountPseudonym);
    const pilotApproved = envApproved
      ? false
      : await (options.pilotAccess ?? new PostgresLivePilotAccess())
          .isAllowed(credential.credentialId);
    if (!envApproved && !pilotApproved) {
      throw new HttpError(
        403,
        'GPT-Live experiment is unavailable for this account.',
      );
    }
    // This reservation happens before the provider can create a billable session.
    await (options.admissionStore ?? new PostgresLiveAdmissionStore()).reserve(credential.credentialId);
    const answer: LiveSessionAnswer = await (options.mintLiveSession ?? createLiveSession)(
      input,
      { safetyIdentifier: accountRealtimeSafetyIdentifier(credential), pilotApproved },
    );
    return json({ ...answer, configurationId: 'openai:gpt-live-1:webrtc-client-v1' }, 201, id);
  } catch (error) {
    return errorResponse(error, id, 'account.live-session');
  }
}
