import { describe, expect, it } from 'vitest';
import { z } from 'zod';

import { parseJson } from '../src/http';

const schema = z.object({ value: z.string() });

describe('bounded JSON parsing', () => {
  it('parses a valid body within the configured limit', async () => {
    const request = new Request('https://example.test', {
      method: 'POST',
      body: JSON.stringify({ value: 'wake' }),
    });

    await expect(parseJson(request, schema, 64)).resolves.toEqual({ value: 'wake' });
  });

  it('rejects an oversized streamed body even without a content-length header', async () => {
    const request = new Request('https://example.test', {
      method: 'POST',
      body: JSON.stringify({ value: 'x'.repeat(256) }),
    });

    await expect(parseJson(request, schema, 64)).rejects.toMatchObject({
      status: 413,
      message: 'Request body is too large.',
    });
  });

  it('rejects an oversized declared body before reading it', async () => {
    const request = new Request('https://example.test', {
      method: 'POST',
      headers: { 'content-length': '1024' },
      body: JSON.stringify({ value: 'wake' }),
    });

    await expect(parseJson(request, schema, 64)).rejects.toMatchObject({
      status: 413,
      message: 'Request body is too large.',
    });
  });

  it('measures UTF-8 bytes rather than JavaScript character count', async () => {
    const request = new Request('https://example.test', {
      method: 'POST',
      body: JSON.stringify({ value: '🙂'.repeat(32) }),
    });

    await expect(parseJson(request, schema, 80)).rejects.toMatchObject({ status: 413 });
  });
});
