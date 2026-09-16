import { describe, expect, it } from 'vitest';

import { handleAccountBackupRequest } from '../src/account/backup-handler';
import type { AccountIdentity, AccountTokenVerifier } from '../src/account/auth';
import type {
  ConsumerBackupStore,
  StoredConsumerBackup,
} from '../src/account/backup-store';
import type { ConsumerBackupSnapshot } from '../src/account/backup-schema';
import { HttpError } from '../src/http';

const USER_A = '2d53f744-d923-4a85-9ed6-7ea4aeece445';
const USER_B = 'db0cd269-bc53-4c5a-a8ae-97e40a58b865';
const NOW = new Date('2026-09-16T10:15:00.000Z');

class MemoryBackupStore implements ConsumerBackupStore {
  readonly values = new Map<string, StoredConsumerBackup>();
  getCalls = 0;
  putCalls = 0;
  failReads = false;
  failWrites = false;

  async get(accountId: string): Promise<StoredConsumerBackup | null> {
    this.getCalls++;
    if (this.failReads) throw new Error('database unavailable with private diagnostics');
    return this.values.get(accountId) ?? null;
  }

  async put(accountId: string, snapshot: ConsumerBackupSnapshot, storedAt: string): Promise<void> {
    this.putCalls++;
    if (this.failWrites) throw new Error('database unavailable with private diagnostics');
    this.values.set(accountId, { snapshot, storedAt });
  }
}

const verifier: AccountTokenVerifier = async token => {
  const identity: AccountIdentity | undefined = token === 'token-a'
    ? { userId: USER_A, email: 'a@example.test' }
    : token === 'token-b'
      ? { userId: USER_B, email: 'b@example.test' }
      : undefined;
  if (!identity) throw new HttpError(401, 'Unauthorized.');
  return identity;
};

function snapshot(): ConsumerBackupSnapshot {
  return {
    schemaVersion: 1,
    generatedAt: '2026-09-16T09:00:00.000Z',
    preferences: {
      displayName: 'Yotam',
      defaultSoundId: 'morning-light',
      defaultVoiceCheckInEnabled: true,
      defaultVoiceStyle: 'DEFAULT',
      defaultSnoozeMinutes: 5,
      defaultFirstMove: 'Open the curtains',
      appearance: 'DAYLIGHT',
    },
    alarms: [
      {
        id: 'weekday-morning',
        label: 'Weekday morning',
        enabled: true,
        zoneId: 'Europe/Berlin',
        schedule: {
          type: 'weekly',
          days: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
          time: '07:30',
        },
        soundId: 'morning-light',
        voiceCheckInEnabled: true,
        characterId: 'alfred',
        voiceStyle: 'DEFAULT',
        snoozePolicy: {
          enabled: true,
          durationSeconds: 300,
          maxCount: null,
        },
        tomorrowContractMode: 'OPTIONAL',
        firstMoveDefault: 'Open the curtains',
        revision: 4,
        createdAt: '2026-09-10T08:00:00.000Z',
        updatedAt: '2026-09-15T18:00:00.000Z',
      },
    ],
  };
}

function request(
  method: string,
  token?: string,
  body?: unknown,
): Request {
  return new Request('https://cloud.example.test/api/v1/account/backup', {
    method,
    headers: {
      ...(token ? { authorization: `Bearer ${token}` } : {}),
      ...(body === undefined ? {} : { 'content-type': 'application/json' }),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
}

function dependencies(store: ConsumerBackupStore) {
  return {
    verifyAccessToken: verifier,
    store,
    now: () => NOW,
  };
}

describe('account consumer backup API', () => {
  it('requires an authenticated user before reading account storage', async () => {
    const store = new MemoryBackupStore();

    const response = await handleAccountBackupRequest(request('GET'), dependencies(store));

    expect(response.status).toBe(401);
    expect(store.getCalls).toBe(0);
  });

  it('stores one validated latest snapshot under the authenticated account', async () => {
    const store = new MemoryBackupStore();
    const payload = snapshot();

    const response = await handleAccountBackupRequest(
      request('PUT', 'token-a', payload),
      dependencies(store),
    );

    expect(response.status).toBe(200);
    expect(store.putCalls).toBe(1);
    expect(store.values.get(USER_A)).toEqual({
      snapshot: payload,
      storedAt: NOW.toISOString(),
    });
    await expect(response.json()).resolves.toEqual({
      backup: {
        generatedAt: payload.generatedAt,
        storedAt: NOW.toISOString(),
        alarmCount: 1,
      },
    });
  });

  it('keeps backups account-scoped', async () => {
    const store = new MemoryBackupStore();
    const payload = snapshot();
    await store.put(USER_A, payload, NOW.toISOString());

    const response = await handleAccountBackupRequest(
      request('GET', 'token-b'),
      dependencies(store),
    );

    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toEqual({ backup: null });
  });

  it('returns the latest backup to its owning account', async () => {
    const store = new MemoryBackupStore();
    const payload = snapshot();
    await store.put(USER_A, payload, NOW.toISOString());

    const response = await handleAccountBackupRequest(
      request('GET', 'token-a'),
      dependencies(store),
    );

    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toEqual({
      backup: {
        snapshot: payload,
        storedAt: NOW.toISOString(),
      },
    });
  });

  it('rejects unexpected private-content fields instead of persisting them', async () => {
    const store = new MemoryBackupStore();
    const payload = {
      ...snapshot(),
      tomorrowContractText: 'Private text must never enter account backup.',
    };

    const response = await handleAccountBackupRequest(
      request('PUT', 'token-a', payload),
      dependencies(store),
    );

    expect(response.status).toBe(400);
    expect(store.putCalls).toBe(0);
  });

  it('rejects invalid or duplicate alarm intent', async () => {
    const store = new MemoryBackupStore();
    const alarm = snapshot().alarms[0];
    const payload = {
      ...snapshot(),
      alarms: [alarm, alarm],
    };

    const response = await handleAccountBackupRequest(
      request('PUT', 'token-a', payload),
      dependencies(store),
    );

    expect(response.status).toBe(400);
    expect(store.putCalls).toBe(0);
  });

  it('rejects impossible one-shot calendar dates before storage', async () => {
    const store = new MemoryBackupStore();
    const base = snapshot();
    const alarm = base.alarms[0]!;
    const payload = {
      ...base,
      alarms: [
        {
          ...alarm,
          schedule: {
            type: 'oneShot',
            date: '2026-02-31',
            time: '07:30',
          },
        },
      ],
    };

    const response = await handleAccountBackupRequest(
      request('PUT', 'token-a', payload),
      dependencies(store),
    );

    expect(response.status).toBe(400);
    expect(store.putCalls).toBe(0);
  });

  it('fails closed with a redacted response when account storage is unavailable', async () => {
    const store = new MemoryBackupStore();
    store.failReads = true;

    const response = await handleAccountBackupRequest(
      request('GET', 'token-a'),
      dependencies(store),
    );
    const body = await response.json() as { error: string };

    expect(response.status).toBe(502);
    expect(body.error).toBe('Request failed.');
    expect(JSON.stringify(body)).not.toContain('private diagnostics');
  });

  it('does not accept methods outside explicit backup read and write', async () => {
    const store = new MemoryBackupStore();

    const response = await handleAccountBackupRequest(
      request('DELETE', 'token-a'),
      dependencies(store),
    );

    expect(response.status).toBe(405);
    expect(response.headers.get('allow')).toBe('GET, PUT');
    expect(store.getCalls).toBe(0);
    expect(store.putCalls).toBe(0);
  });
});
