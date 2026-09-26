import type { Kysely } from 'kysely';

import {
  ACCOUNT_DATABASE_SCHEMA,
  configuredAccountDatabase,
  type AccountDatabase,
} from './database.js';

export type AccountRole = 'user' | 'admin';

export interface AccountAccessStore {
  roleFor(accountId: string): Promise<AccountRole>;
}

export class PostgresAccountAccessStore implements AccountAccessStore {
  constructor(private readonly database: Kysely<AccountDatabase> = configuredAccountDatabase()) {}

  async roleFor(accountId: string): Promise<AccountRole> {
    const row = await this.database
      .withSchema(ACCOUNT_DATABASE_SCHEMA)
      .selectFrom('account_roles')
      .select('role')
      .where('account_id', '=', accountId)
      .executeTakeFirst();

    return row?.role ?? 'user';
  }
}
