import type { Kysely } from 'kysely';

import { HttpError } from '../http.js';
import {
  ACCOUNT_DATABASE_SCHEMA,
  configuredAccountDatabase,
  type AccountDatabase,
} from './database.js';
import type { VerifiedAccountRealtimeCredential } from './account-realtime-auth.js';

export interface AccountRealtimeDeviceActivation {
  credentialId: string;
  accountId: string;
  accountPseudonym: string;
  installationId: string;
  expiresAt: number;
}

export interface AccountRealtimeDeviceStore {
  activate(activation: AccountRealtimeDeviceActivation): Promise<void>;
  authorize(credential: VerifiedAccountRealtimeCredential): Promise<void>;
  revoke(accountId: string, installationId: string): Promise<void>;
}

export class PostgresAccountRealtimeDeviceStore implements AccountRealtimeDeviceStore {
  constructor(private readonly database: Kysely<AccountDatabase> = configuredAccountDatabase()) {}

  async activate(activation: AccountRealtimeDeviceActivation): Promise<void> {
    const now = new Date();
    const expiresAt = new Date(activation.expiresAt * 1000);

    await this.database
      .withSchema(ACCOUNT_DATABASE_SCHEMA)
      .insertInto('account_realtime_devices')
      .values({
        credential_id: activation.credentialId,
        account_id: activation.accountId,
        account_pseudonym: activation.accountPseudonym,
        installation_id: activation.installationId,
        expires_at: expiresAt,
        revoked_at: null,
        last_used_at: null,
        created_at: now,
        updated_at: now,
      })
      .onConflict(conflict =>
        conflict.columns(['account_id', 'installation_id']).doUpdateSet({
          credential_id: activation.credentialId,
          account_pseudonym: activation.accountPseudonym,
          expires_at: expiresAt,
          revoked_at: null,
          last_used_at: null,
          updated_at: now,
        }),
      )
      .execute();
  }

  async authorize(credential: VerifiedAccountRealtimeCredential): Promise<void> {
    const now = new Date();
    const authorized = await this.database
      .withSchema(ACCOUNT_DATABASE_SCHEMA)
      .updateTable('account_realtime_devices')
      .set({
        last_used_at: now,
        updated_at: now,
      })
      .where('credential_id', '=', credential.credentialId)
      .where('installation_id', '=', credential.installationId)
      .where('account_pseudonym', '=', credential.accountPseudonym)
      .where('revoked_at', 'is', null)
      .where('expires_at', '>', now)
      .returning('credential_id')
      .executeTakeFirst();

    if (!authorized) throw new HttpError(401, 'Unauthorized.');
  }

  async revoke(accountId: string, installationId: string): Promise<void> {
    const now = new Date();
    await this.database
      .withSchema(ACCOUNT_DATABASE_SCHEMA)
      .updateTable('account_realtime_devices')
      .set({
        revoked_at: now,
        updated_at: now,
      })
      .where('account_id', '=', accountId)
      .where('installation_id', '=', installationId)
      .where('revoked_at', 'is', null)
      .execute();
  }
}
