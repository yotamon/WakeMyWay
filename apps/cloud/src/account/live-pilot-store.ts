import { sql, type Kysely } from 'kysely';
import { configuredAccountDatabase, type AccountDatabase } from './database.js';

/** Resolves a verified, revocable device credential to an opt-in pilot account. */
export interface LivePilotAccess {
  isAllowed(credentialId: string): Promise<boolean>;
}

export class PostgresLivePilotAccess implements LivePilotAccess {
  constructor(private readonly database: Kysely<AccountDatabase> = configuredAccountDatabase()) {}

  async isAllowed(credentialId: string): Promise<boolean> {
    const result = await sql<{ ok: boolean }>`
      SELECT TRUE AS ok
      FROM wmw_private.account_realtime_devices AS device
      INNER JOIN wmw_private.live_pilot_accounts AS pilot
        ON pilot.account_id = device.account_id
      WHERE device.credential_id = ${credentialId}::uuid
        AND device.revoked_at IS NULL
        AND device.expires_at > now()
        AND pilot.enabled IS TRUE
      LIMIT 1
    `.execute(this.database);
    return result.rows.length === 1;
  }
}
