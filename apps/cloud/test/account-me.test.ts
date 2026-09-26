import { describe, expect, it } from 'vitest';

import { handleAccountMeRequest } from '../src/account/me-handler';
import type { AccountAccessStore } from '../src/account/access-store';

const USER_ID = '2d53f744-d923-4a85-9ed6-7ea4aeece445';

function request(token = 'valid-token'): Request {
  return new Request('https://wakemyway.example/api/v1/account/me', {
    headers: { authorization: `Bearer ${token}` },
  });
}

describe('account profile endpoint', () => {
  it('returns the authenticated account with its server-owned role', async () => {
    const accessStore: AccountAccessStore = {
      roleFor: async accountId => {
        expect(accountId).toBe(USER_ID);
        return 'admin';
      },
    };

    const response = await handleAccountMeRequest(request(), {
      verifyAccessToken: async token => {
        expect(token).toBe('valid-token');
        return { userId: USER_ID, email: 'wake@example.test' };
      },
      accessStore,
    });

    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toMatchObject({
      account: {
        id: USER_ID,
        email: 'wake@example.test',
        role: 'admin',
      },
    });
  });

  it('keeps an account without an explicit role least privileged', async () => {
    const response = await handleAccountMeRequest(request(), {
      verifyAccessToken: async () => ({ userId: USER_ID }),
      accessStore: { roleFor: async () => 'user' },
    });

    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toMatchObject({
      account: {
        id: USER_ID,
        email: null,
        role: 'user',
      },
    });
  });

  it('rejects unauthenticated requests before role lookup', async () => {
    let roleLookups = 0;
    const response = await handleAccountMeRequest(
      new Request('https://wakemyway.example/api/v1/account/me'),
      {
        verifyAccessToken: async () => {
          throw new Error('verifier must not run without a bearer token');
        },
        accessStore: {
          roleFor: async () => {
            roleLookups += 1;
            return 'admin';
          },
        },
      },
    );

    expect(response.status).toBe(401);
    expect(roleLookups).toBe(0);
  });
});
