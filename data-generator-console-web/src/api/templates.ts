import { apiRequest } from './client';
import type { RunStartResult, TemplateSummary, TemplateTaxonomy } from './types';

/**
 * @param includeArchived include soft-deleted rows
 * @param q optional name/id filter
 * @param category optional category filter
 * @param tag optional tag filter
 */
export function fetchTemplates(
  includeArchived: boolean,
  q?: string,
  category?: string,
  tag?: string,
): Promise<TemplateSummary[]> {
  const params = new URLSearchParams();
  params.set('includeArchived', String(includeArchived));
  if (q?.trim()) {
    params.set('q', q.trim());
  }
  if (category?.trim()) {
    params.set('category', category.trim());
  }
  if (tag?.trim()) {
    params.set('tag', tag.trim());
  }
  return apiRequest<TemplateSummary[]>(`/templates?${params}`);
}

/**
 * @returns distinct categories and tags for catalog filters
 */
export function fetchTemplateTaxonomy(): Promise<TemplateTaxonomy> {
  return apiRequest<TemplateTaxonomy>('/templates/taxonomy');
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
