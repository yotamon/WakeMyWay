import { Kysely, PostgresDialect, type ColumnType } from 'kysely';
import { Pool } from 'pg';

import { HttpError } from '../http.js';
import { consumerBackupSnapshotSchema, type ConsumerBackupSnapshot } from './backup-schema.js';

const DATABASE_SCHEMA = 'wmw_private';

interface ConsumerBackupTable {
  account_id: string;
  schema_version: number;
  generated_at: ColumnType<Date, Date, Date>;
  snapshot: ColumnType<unknown, unknown, unknown>;
  updated_at: ColumnType<Date, Date, Date>;
}

interface AccountDatabase {
  consumer_backups: ConsumerBackupTable;
}

export interface StoredConsumerBackup {
  snapshot: ConsumerBackupSnapshot;
  storedAt: string;
}

export interface ConsumerBackupStore {
  get(accountId: string): Promise<StoredConsumerBackup | null>;
  put(accountId: string, snapshot: ConsumerBackupSnapshot, storedAt: string): Promise<void>;
}

export class PostgresConsumerBackupStore implements ConsumerBackupStore {
  constructor(private readonly database: Kysely<AccountDatabase> = configuredAccountDatabase()) {}

  async get(accountId: string): Promise<StoredConsumerBackup | null> {
    const row = await this.database
      .withSchema(DATABASE_SCHEMA)
      .selectFrom('consumer_backups')
      .select(['snapshot', 'updated_at'])
      .where('account_id', '=', accountId)
      .executeTakeFirst();
    if (!row) return null;

    const parsed = consumerBackupSnapshotSchema.safeParse(row.snapshot);
    if (!parsed.success) throw new Error('Stored consumer backup is invalid.');

    return {
      snapshot: parsed.data,
      storedAt: row.updated_at.toISOString(),
    };
  }

  async put(accountId: string, snapshot: ConsumerBackupSnapshot, storedAt: string): Promise<void> {
    const storedDate = new Date(storedAt);
    const generatedDate = new Date(snapshot.generatedAt);

    await this.database
      .withSchema(DATABASE_SCHEMA)
      .insertInto('consumer_backups')
      .values({
        account_id: accountId,
        schema_version: snapshot.schemaVersion,
        generated_at: generatedDate,
        snapshot,
        updated_at: storedDate,
      })
      .onConflict(conflict => conflict.column('account_id').doUpdateSet({
        schema_version: snapshot.schemaVersion,
        generated_at: generatedDate,
        snapshot,
        updated_at: storedDate,
      }))
      .execute();
  }
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
