import { apiRequest } from './client';
import type { AuditEventView } from './types';

/**
 * @param action optional action filter
 * @param resourceType optional resource type filter
 * @param limit max rows (default 100)
 */
export function fetchAuditEvents(
  action?: string,
  resourceType?: string,
  limit = 100,
): Promise<AuditEventView[]> {
  const params = new URLSearchParams();
  if (action?.trim()) {
    params.set('action', action.trim());
  }
  if (resourceType?.trim()) {
    params.set('resourceType', resourceType.trim());
  }
  params.set('limit', String(limit));
  const query = params.toString();
  return apiRequest<AuditEventView[]>(`/console/audit${query ? `?${query}` : ''}`);
}
