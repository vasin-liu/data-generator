import type { ApiResult } from './types';
import { parseApiResult } from './types';

const API_BASE = '/api';

/**
 * @param path path after {@code /api} (e.g. {@code /jobs})
 */
export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: {
      Accept: 'application/json',
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...init?.headers,
    },
    ...init,
  });
  const body = (await res.json()) as ApiResult<T>;
  if (!body.success) {
    throw new Error(body.message || `Request failed (${res.status})`);
  }
  return body.data;
}

/**
 * @param path path after {@code /api}
 * @param form multipart body (Content-Type omitted)
 */
export async function apiFormRequest<T>(path: string, form: FormData, method = 'POST'): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, { method, body: form });
  return parseApiResult<T>(res);
}
