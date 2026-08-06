import type { ApiResult } from './types';
import { ApiRequestError, parseApiResult } from './types';
import { getConsoleRole } from './consoleRole';

const API_BASE = '/api';

/** Console RBAC header used by JSON and raw (geo+json) fetches. */
export function consoleRoleHeaders(): Record<string, string> {
  return {
    'X-Console-Role': getConsoleRole(),
  };
}

/**
 * @param path path after {@code /api} (e.g. {@code /jobs})
 */
export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: {
      Accept: 'application/json',
      ...consoleRoleHeaders(),
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...init?.headers,
    },
    ...init,
  });
  const contentType = res.headers.get('content-type') ?? '';
  if (!contentType.includes('application/json')) {
    const text = await res.text();
    if (res.status === 403) {
      throw new Error(text || 'Forbidden (403): missing or insufficient console role');
    }
    throw new Error(text || `Request failed (${res.status})`);
  }
  const body = (await res.json()) as ApiResult<T>;
  if (!body.success) {
    const message =
      res.status === 403
        ? body.message || 'Forbidden (403): missing or insufficient console role'
        : body.message || `Request failed (${res.status})`;
    // Preserve status + data so 409 usages (and similar) survive for Modals.
    throw new ApiRequestError(message, res.status, body.data);
  }
  return body.data;
}

export { ApiRequestError };

/**
 * @param path path after {@code /api}
 * @param form multipart body (Content-Type omitted)
 */
export async function apiFormRequest<T>(path: string, form: FormData, method = 'POST'): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    body: form,
    headers: consoleRoleHeaders(),
  });
  return parseApiResult<T>(res);
}
