import { apiRequest } from './client';
import type { RunStartResult, TemplateSummary } from './types';

/**
 * @param includeArchived include soft-deleted rows
 * @param q optional name/id filter
 */
export function fetchTemplates(includeArchived: boolean, q?: string): Promise<TemplateSummary[]> {
  const params = new URLSearchParams();
  params.set('includeArchived', String(includeArchived));
  if (q?.trim()) {
    params.set('q', q.trim());
  }
  return apiRequest<TemplateSummary[]>(`/templates?${params}`);
}

/**
 * @param templateId row id
 */
export function archiveTemplate(templateId: string): Promise<string> {
  return apiRequest<string>(`/templates/${templateId}/archive`, { method: 'POST' });
}

/**
 * @param templateId row id
 */
export function restoreTemplate(templateId: string): Promise<string> {
  return apiRequest<string>(`/templates/${templateId}/restore`, { method: 'POST' });
}

/**
 * @param templateId row id
 */
export function runTemplate(templateId: string): Promise<RunStartResult> {
  return apiRequest<RunStartResult>(`/templates/${templateId}/run`, { method: 'POST' });
}
