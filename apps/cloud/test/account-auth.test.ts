import { createServer, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { exportJWK, generateKeyPair, SignJWT, type CryptoKey } from 'jose';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { createSupabaseAccountTokenVerifier } from '../src/account/auth';

const USER_ID = '2d53f744-d923-4a85-9ed6-7ea4aeece445';
const KEY_ID = 'wmw-test-key';

let server: Server;
let baseUrl: string;
let privateKey: CryptoKey;
let jwksJson: string;

beforeEach(async () => {
  const pair = await generateKeyPair('ES256', { extractable: true });
  privateKey = pair.privateKey;
  const publicJwk = await exportJWK(pair.publicKey);
  jwksJson = JSON.stringify({
    keys: [
      {
        ...publicJwk,
        kid: KEY_ID,
        alg: 'ES256',
        use: 'sig',
      },
    ],
  });

  server = createServer((request, response) => {
    if (request.url === '/auth/v1/.well-known/jwks.json') {
      response.writeHead(200, {
        'content-type': 'application/json',
        'cache-control': 'no-store',
      });
      response.end(jwksJson);
      return;
    }
    response.writeHead(404);
    response.end();
  });

  await new Promise<void>(resolve => server.listen(0, '127.0.0.1', resolve));
  const address = server.address() as AddressInfo;
  baseUrl = `http://127.0.0.1:${address.port}`;
});

afterEach(async () => {
  await new Promise<void>((resolve, reject) => {
    server.close(error => error ? reject(error) : resolve());
  });
});

async function token(overrides?: {
  audience?: string;
  issuer?: string;
  subject?: string;
  expiresIn?: string;
}): Promise<string> {
  return new SignJWT({ email: 'wake@example.test' })
    .setProtectedHeader({ alg: 'ES256', kid: KEY_ID })
    .setIssuer(overrides?.issuer ?? `${baseUrl}/auth/v1`)
    .setAudience(overrides?.audience ?? 'authenticated')
    .setSubject(overrides?.subject ?? USER_ID)
    .setIssuedAt()
    .setExpirationTime(overrides?.expiresIn ?? '5m')
    .sign(privateKey);
}

describe('Supabase account access-token verification', () => {
  it('accepts a valid signed authenticated-user token', async () => {
    const verifier = createSupabaseAccountTokenVerifier(baseUrl);

    await expect(verifier(await token())).resolves.toEqual({
      userId: USER_ID,
      email: 'wake@example.test',
    });
  });

  it('rejects a token for the wrong audience', async () => {
    const verifier = createSupabaseAccountTokenVerifier(baseUrl);

    await expect(verifier(await token({ audience: 'anon' }))).rejects.toMatchObject({
      status: 401,
      message: 'Unauthorized.',
    });
  });

  it('rejects a token from the wrong issuer', async () => {
    const verifier = createSupabaseAccountTokenVerifier(baseUrl);

    await expect(verifier(await token({ issuer: 'https://attacker.example/auth/v1' }))).rejects.toMatchObject({
      status: 401,
      message: 'Unauthorized.',
    });
  });

  it('rejects expired tokens', async () => {
    const verifier = createSupabaseAccountTokenVerifier(baseUrl);

    await expect(verifier(await token({ expiresIn: '-1m' }))).rejects.toMatchObject({
      status: 401,
      message: 'Unauthorized.',
    });
  });

  it('requires the Supabase subject to be an account UUID', async () => {
    const verifier = createSupabaseAccountTokenVerifier(baseUrl);

    await expect(verifier(await token({ subject: 'not-an-account-id' }))).rejects.toMatchObject({
      status: 401,
      message: 'Unauthorized.',
    });
  });
});
