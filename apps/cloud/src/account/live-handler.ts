import { errorResponse, HttpError, json, methodNotAllowed, parseJson, requestId } from '../http.js';
import { accountRealtimeSafetyIdentifier, verifyAccountRealtimeCredential } from './account-realtime-auth.js';
import { PostgresAccountRealtimeDeviceStore, type AccountRealtimeDeviceStore } from './realtime-device-store.js';
import { createLiveSession, liveRequestSchema, type LiveSessionAnswer } from '../voice-spike/gpt-live.js';

interface Options {
  verifyCredential?: typeof verifyAccountRealtimeCredential;
  deviceStore?: AccountRealtimeDeviceStore;
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
    const answer: LiveSessionAnswer = await (options.mintLiveSession ?? createLiveSession)(
      input,
      { safetyIdentifier: accountRealtimeSafetyIdentifier(credential) },
    );
    return json({ ...answer, configurationId: 'openai:gpt-live-1:webrtc-client-v1' }, 201, id);
  } catch (error) {
    return errorResponse(error, id, 'account.live-session');
  }
}
