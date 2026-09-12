import { randomUUID, timingSafeEqual } from 'node:crypto';
import { z } from 'zod';

import { getAIConfig } from './ai/config';

const MAX_JSON_BYTES = 32 * 1024;

export class HttpError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'HttpError';
  }
}

export function requestId(request: Request): string {
  const supplied = request.headers.get('x-request-id')?.trim();
  if (supplied && /^[A-Za-z0-9._:-]{1,96}$/.test(supplied)) return supplied;
  return randomUUID();
}

export function json(data: unknown, status = 200, id?: string): Response {
  return Response.json(data, {
    status,
    headers: {
      'cache-control': 'no-store',
      ...(id ? { 'x-request-id': id } : {}),
    },
  });
}

export function methodNotAllowed(methods: string[], id: string): Response {
  return new Response('Method Not Allowed', {
    status: 405,
    headers: {
      allow: methods.join(', '),
      'cache-control': 'no-store',
      'x-request-id': id,
    },
  });
}

function bearerToken(request: Request): string | undefined {
  const header = request.headers.get('authorization');
  if (!header) return undefined;
  const match = /^Bearer\s+(.+)$/i.exec(header.trim());
  return match?.[1];
}

export function secureEqual(left: string, right: string): boolean {
  const leftBytes = Buffer.from(left);
  const rightBytes = Buffer.from(right);
  if (leftBytes.length !== rightBytes.length) return false;
  return timingSafeEqual(leftBytes, rightBytes);
}

export function isInternallyAuthorized(request: Request, secret: string): boolean {
  const token = bearerToken(request);
  return Boolean(token && secureEqual(token, secret));
}

export function requireInternalAuthorization(request: Request): void {
  const secret = getAIConfig().internalApiKey;
  if (!secret) throw new HttpError(503, 'Internal API authentication is not configured.');
  if (!isInternallyAuthorized(request, secret)) throw new HttpError(401, 'Unauthorized.');
}

export async function parseJson<T extends z.ZodType>(
  request: Request,
  schema: T,
  maxBytes = MAX_JSON_BYTES,
): Promise<z.output<T>> {
  const declaredLength = Number(request.headers.get('content-length') ?? 0);
  if (Number.isFinite(declaredLength) && declaredLength > maxBytes) {
    throw new HttpError(413, 'Request body is too large.');
  }

  const raw = await readBodyBounded(request, maxBytes);

  let decoded: unknown;
  try {
    decoded = JSON.parse(raw);
  } catch {
    throw new HttpError(400, 'Request body must be valid JSON.');
  }

  const parsed = schema.safeParse(decoded);
  if (!parsed.success) throw new HttpError(400, 'Request body is invalid.');
  return parsed.data;
}

async function readBodyBounded(request: Request, maxBytes: number): Promise<string> {
  const reader = request.body?.getReader();
  if (!reader) return '';

  const chunks: Uint8Array[] = [];
  let totalBytes = 0;

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      if (!value) continue;

      totalBytes += value.byteLength;
      if (totalBytes > maxBytes) {
        await reader.cancel('request body too large').catch(() => undefined);
        throw new HttpError(413, 'Request body is too large.');
      }
      chunks.push(value);
    }
  } finally {
    reader.releaseLock();
  }

  return Buffer.concat(chunks.map(chunk => Buffer.from(chunk))).toString('utf8');
}

export function errorResponse(error: unknown, id: string, operation: string): Response {
  if (error instanceof HttpError) {
    return json({ error: error.message, requestId: id }, error.status, id);
  }

  // Intentionally do not log error messages, provider payloads, prompts, transcripts, or audio.
  console.error('[wmw-cloud]', {
    requestId: id,
    operation,
    errorType: error instanceof Error ? error.name : typeof error,
  });

  return json({ error: 'Request failed.', requestId: id }, 502, id);
}
