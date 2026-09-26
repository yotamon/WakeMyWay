import type { Kysely } from 'kysely';

import {
  ACCOUNT_DATABASE_SCHEMA,
  configuredAccountDatabase,
  type AccountDatabase,
} from './database.js';
import { consumerBackupSnapshotSchema, type ConsumerBackupSnapshot } from './backup-schema.js';

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
      .withSchema(ACCOUNT_DATABASE_SCHEMA)
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
      .withSchema(ACCOUNT_DATABASE_SCHEMA)
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
