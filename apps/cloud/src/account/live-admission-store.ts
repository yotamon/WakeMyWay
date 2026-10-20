import { sql, type Kysely } from 'kysely';
import { HttpError } from '../http.js';
import { configuredAccountDatabase, type AccountDatabase } from './database.js';

export interface LiveAdmissionStore {
  /** Must reserve an admission atomically before making a billable provider request. */
  reserve(accountId: string): Promise<void>;
}

export const MAX_LIVE_SESSIONS_PER_UTC_DAY = 16;
export const MIN_LIVE_SESSION_START_INTERVAL_SECONDS = 20;

/**
 * Atomic reserve, including across Vercel instances. An unsuccessful OpenAI
 * request consumes an admission intentionally: retry storms are not free.
 */
export class PostgresLiveAdmissionStore implements LiveAdmissionStore {
  constructor(private readonly db: Kysely<AccountDatabase> = configuredAccountDatabase()) {}

  async reserve(accountId: string): Promise<void> {
    const result = await sql<{ sessions_started: number }>`
      INSERT INTO wmw_private.live_session_admissions
        (account_id, utc_day, sessions_started, last_started_at)
      VALUES (${accountId}::uuid, (now() at time zone 'utc')::date, 1, now())
      ON CONFLICT (account_id, utc_day)
      DO UPDATE SET
        sessions_started = wmw_private.live_session_admissions.sessions_started + 1,
        last_started_at = now()
      WHERE wmw_private.live_session_admissions.sessions_started < ${MAX_LIVE_SESSIONS_PER_UTC_DAY}
        AND wmw_private.live_session_admissions.last_started_at
          <= now() - (${MIN_LIVE_SESSION_START_INTERVAL_SECONDS} * interval '1 second')
      RETURNING sessions_started
    `.execute(this.db);
    if (result.rows.length === 0) {
      throw new HttpError(429, 'GPT-Live daily limit or start cooldown was reached.');
    }
  }
}
