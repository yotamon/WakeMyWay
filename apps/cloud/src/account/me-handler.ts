import { requireAccountIdentity, type AccountTokenVerifier } from './auth.js';
import {
  PostgresAccountAccessStore,
  type AccountAccessStore,
} from './access-store.js';
import { errorResponse, json, methodNotAllowed, requestId } from '../http.js';

export interface AccountMeDependencies {
  verifyAccessToken: AccountTokenVerifier;
  accessStore: AccountAccessStore;
}

export async function handleAccountMeRequest(
  request: Request,
  suppliedDependencies?: Partial<AccountMeDependencies>,
): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'GET') return methodNotAllowed(['GET'], id);

  try {
    const identity = await requireAccountIdentity(request, suppliedDependencies?.verifyAccessToken);
    const accessStore = suppliedDependencies?.accessStore ?? new PostgresAccountAccessStore();
    const role = await accessStore.roleFor(identity.userId);

    return json(
      {
        account: {
          id: identity.userId,
          email: identity.email ?? null,
          role,
        },
      },
      200,
      id,
    );
  } catch (error) {
    return errorResponse(error, id, 'account-me');
  }
}
