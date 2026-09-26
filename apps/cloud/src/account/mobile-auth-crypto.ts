import {
  createCipheriv,
  createDecipheriv,
  createHash,
  randomBytes,
  timingSafeEqual,
} from 'node:crypto';

const HANDOFF_KEY_LABEL = 'wmw-mobile-auth-handoff-v1';
const AES_GCM = 'aes-256-gcm';
const IV_BYTES = 12;
const AUTH_TAG_BYTES = 16;

export function randomBase64Url(bytes = 32): string {
  return randomBytes(bytes).toString('base64url');
}

export function sha256Hex(value: string): string {
  return createHash('sha256').update(value, 'utf8').digest('hex');
}

export function pkceChallenge(verifier: string): string {
  return createHash('sha256').update(verifier, 'utf8').digest('base64url');
}

export function safeEqual(left: string, right: string): boolean {
  const a = Buffer.from(left, 'utf8');
  const b = Buffer.from(right, 'utf8');
  return a.length === b.length && timingSafeEqual(a, b);
}

export function encryptSessionToken(token: string, secret: string): string {
  const key = handoffKey(secret);
  const iv = randomBytes(IV_BYTES);
  const cipher = createCipheriv(AES_GCM, key, iv);
  const ciphertext = Buffer.concat([cipher.update(token, 'utf8'), cipher.final()]);
  const tag = cipher.getAuthTag();
  return Buffer.concat([iv, tag, ciphertext]).toString('base64url');
}

export function decryptSessionToken(envelope: string, secret: string): string {
  const bytes = Buffer.from(envelope, 'base64url');
  if (bytes.length <= IV_BYTES + AUTH_TAG_BYTES) throw new Error('Invalid auth handoff envelope.');

  const iv = bytes.subarray(0, IV_BYTES);
  const tag = bytes.subarray(IV_BYTES, IV_BYTES + AUTH_TAG_BYTES);
  const ciphertext = bytes.subarray(IV_BYTES + AUTH_TAG_BYTES);
  const decipher = createDecipheriv(AES_GCM, handoffKey(secret), iv);
  decipher.setAuthTag(tag);
  return Buffer.concat([decipher.update(ciphertext), decipher.final()]).toString('utf8');
}

function handoffKey(secret: string): Buffer {
  return createHash('sha256')
    .update(HANDOFF_KEY_LABEL, 'utf8')
    .update('\0', 'utf8')
    .update(secret, 'utf8')
    .digest();
}
