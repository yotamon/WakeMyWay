import { configuredAccountTokenVerifier, requireAccountIdentity, type AccountTokenVerifier } from './auth.js';
import { consumerBackupSnapshotSchema } from './backup-schema.js';
import { PostgresConsumerBackupStore, type ConsumerBackupStore } from './backup-store.js';
import { errorResponse, json, methodNotAllowed, parseJson, requestId } from '../http.js';

const MAX_BACKUP_BYTES = 256 * 1024;

export interface AccountBackupDependencies {
  verifyAccessToken: AccountTokenVerifier;
  store: ConsumerBackupStore;
  now: () => Date;
}

export async function handleAccountBackupRequest(
  request: Request,
  suppliedDependencies?: Partial<AccountBackupDependencies>,
): Promise<Response> {
  const id = requestId(request);
  if (request.method !== 'GET' && request.method !== 'PUT') {
    return methodNotAllowed(['GET', 'PUT'], id);
  }

  try {
    const dependencies = dependenciesFor(suppliedDependencies);
    const identity = await requireAccountIdentity(request, dependencies.verifyAccessToken);

    if (request.method === 'GET') {
      const backup = await dependencies.store.get(identity.userId);
      return json({ backup }, 200, id);
    }

    const snapshot = await parseJson(request, consumerBackupSnapshotSchema, MAX_BACKUP_BYTES);
    const storedAt = dependencies.now().toISOString();
    await dependencies.store.put(identity.userId, snapshot, storedAt);

    return json(
      {
        backup: {
          generatedAt: snapshot.generatedAt,
          storedAt,
          alarmCount: snapshot.alarms.length,
        },
      },
      200,
      id,
    );
  } catch (error) {
    return errorResponse(error, id, 'account-consumer-backup');
  }
}

function dependenciesFor(
  supplied: Partial<AccountBackupDependencies> | undefined,
): AccountBackupDependencies {
  return {
    verifyAccessToken: supplied?.verifyAccessToken ?? configuredAccountTokenVerifier(),
    store: supplied?.store ?? new PostgresConsumerBackupStore(),
    now: supplied?.now ?? (() => new Date()),
  };
}
