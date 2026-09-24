import { createHash } from 'node:crypto';

import { Kysely, PostgresDialect, type ColumnType } from 'kysely';
import { Pool } from 'pg';

import { HttpError } from '../http.js';
import type { PlayEntitlementState } from './play-verification.js';

const DATABASE_SCHEMA = 'wmw_private';

interface PlaySubscriptionPurchaseTable {
  purchase_token_sha256: string;
  product_id: string;
  entitlement_state: PlayEntitlementState;
  acknowledged: boolean;
  linked_purchase_token_sha256: string | null;
  expiry_at: ColumnType<Date | null, Date | null, Date | null>;
  last_verified_at: ColumnType<Date, Date, Date>;
  updated_at: ColumnType<Date, Date, Date>;
}

interface CommerceDatabase {
  play_subscription_purchases: PlaySubscriptionPurchaseTable;
}

export interface PlayPurchaseLifecycleRecord {
  purchaseTokenHash: string;
  productId: string;
  entitlement: PlayEntitlementState;
  acknowledged: boolean;
  linkedPurchaseTokenHash?: string;
  expiryAt?: string;
  verifiedAt: string;
}

export interface PlayPurchaseLifecycleStore {
  put(record: PlayPurchaseLifecycleRecord): Promise<void>;
}

export class PostgresPlayPurchaseLifecycleStore implements PlayPurchaseLifecycleStore {
  constructor(private readonly database: Kysely<CommerceDatabase> = configuredCommerceDatabase()) {}

  async put(record: PlayPurchaseLifecycleRecord): Promise<void> {
    const verifiedAt = new Date(record.verifiedAt);
    const expiryAt = record.expiryAt ? new Date(record.expiryAt) : null;

    await this.database
      .withSchema(DATABASE_SCHEMA)
      .insertInto('play_subscription_purchases')
      .values({
        purchase_token_sha256: record.purchaseTokenHash,
        product_id: record.productId,
        entitlement_state: record.entitlement,
        acknowledged: record.acknowledged,
        linked_purchase_token_sha256: record.linkedPurchaseTokenHash ?? null,
        expiry_at: expiryAt,
        last_verified_at: verifiedAt,
        updated_at: verifiedAt,
      })
      .onConflict(conflict => conflict.column('purchase_token_sha256').doUpdateSet({
        product_id: record.productId,
        entitlement_state: record.entitlement,
        acknowledged: record.acknowledged,
        linked_purchase_token_sha256: record.linkedPurchaseTokenHash ?? null,
        expiry_at: expiryAt,
        last_verified_at: verifiedAt,
        updated_at: verifiedAt,
      }))
      .execute();
  }
}

export function purchaseTokenHash(token: string): string {
  return createHash('sha256').update(token, 'utf8').digest('hex');
}

let cachedConnectionString: string | undefined;
let cachedDatabase: Kysely<CommerceDatabase> | undefined;

function configuredCommerceDatabase(): Kysely<CommerceDatabase> {
  const connectionString = process.env.DATABASE_URL?.trim();
  if (!connectionString) {
    throw new HttpError(503, 'Play purchase lifecycle storage is not configured.');
  }

  if (cachedDatabase && cachedConnectionString === connectionString) return cachedDatabase;

  cachedDatabase = new Kysely<CommerceDatabase>({
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
