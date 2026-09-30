import { Kysely, PostgresDialect, type ColumnType } from 'kysely';
import { Pool } from 'pg';

import { HttpError } from '../http.js';

export const ACCOUNT_DATABASE_SCHEMA = 'wmw_private';

export interface ConsumerBackupTable {
  account_id: string;
  schema_version: number;
  generated_at: ColumnType<Date, Date, Date>;
  snapshot: ColumnType<unknown, unknown, unknown>;
  updated_at: ColumnType<Date, Date, Date>;
}

export interface AccountRoleTable {
  account_id: string;
  role: 'user' | 'admin';
  created_at: ColumnType<Date, Date | undefined, never>;
  updated_at: ColumnType<Date, Date | undefined, Date>;
}

export interface AccountRealtimeDeviceTable {
  credential_id: string;
  account_id: string;
  account_pseudonym: string;
  installation_id: string;
  expires_at: ColumnType<Date, Date, Date>;
  revoked_at: ColumnType<Date | null, Date | null | undefined, Date | null>;
  last_used_at: ColumnType<Date | null, Date | null | undefined, Date | null>;
  created_at: ColumnType<Date, Date | undefined, never>;
  updated_at: ColumnType<Date, Date | undefined, Date>;
}

export interface AccountDatabase {
  consumer_backups: ConsumerBackupTable;
  account_roles: AccountRoleTable;
  account_realtime_devices: AccountRealtimeDeviceTable;
}

let cachedConnectionString: string | undefined;
let cachedDatabase: Kysely<AccountDatabase> | undefined;

export function configuredAccountDatabase(): Kysely<AccountDatabase> {
  const connectionString = process.env.DATABASE_URL?.trim();
  if (!connectionString) throw new HttpError(503, 'Account storage is not configured.');

  if (cachedDatabase && cachedConnectionString === connectionString) return cachedDatabase;

  cachedDatabase = new Kysely<AccountDatabase>({
    dialect: new PostgresDialect({
      pool: new Pool({
        connectionString,
        max: 4,
        idleTimeoutMillis: 10_000,
        connectionTimeoutMillis: 5_000,
      }),
    }),
  });
  cachedConnectionString = connectionString;
  return cachedDatabase;
}
