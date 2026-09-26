import { configuredAccountDatabase } from './database.js';

export interface StoredMobileAuthHandoff {
  tokenEnvelope: string;
}

export interface MobileAuthHandoffStore {
  put(input: {
    codeHash: string;
    pkceChallenge: string;
    tokenEnvelope: string;
    expiresAt: Date;
    createdAt: Date;
  }): Promise<void>;
  consume(codeHash: string, pkceChallenge: string, now: Date): Promise<StoredMobileAuthHandoff | null>;
}

export class PostgresMobileAuthHandoffStore implements MobileAuthHandoffStore {
  async put(input: {
    codeHash: string;
    pkceChallenge: string;
    tokenEnvelope: string;
    expiresAt: Date;
    createdAt: Date;
  }): Promise<void> {
    await configuredAccountDatabase()
      .insertInto('mobile_auth_handoffs')
      .values({
        code_hash: input.codeHash,
        pkce_challenge: input.pkceChallenge,
        token_envelope: input.tokenEnvelope,
        expires_at: input.expiresAt,
        created_at: input.createdAt,
      })
      .execute();
  }

  async consume(
    codeHash: string,
    pkceChallenge: string,
    now: Date,
  ): Promise<StoredMobileAuthHandoff | null> {
    const row = await configuredAccountDatabase()
      .deleteFrom('mobile_auth_handoffs')
      .where('code_hash', '=', codeHash)
      .where('pkce_challenge', '=', pkceChallenge)
      .where('expires_at', '>', now)
      .returning(['token_envelope'])
      .executeTakeFirst();

    return row ? { tokenEnvelope: row.token_envelope } : null;
  }
}
