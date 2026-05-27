import { apiRequest } from './client';
import type {
  MigrationAnalysis,
  MigrationCompareReport,
  MigrationInventoryEntry,
  MigrationInventorySummary,
  MigrationSignoffRequest,
  TemplateV2Draft,
} from './types';

/**
 * @returns migration KPI summary
 */
export function fetchMigrationSummary(): Promise<MigrationInventorySummary> {
  return apiRequest<MigrationInventorySummary>('/migration/summary');
}

/**
 * @param filter backlog filter enum name
 */
export function fetchMigrationBacklog(filter?: string): Promise<MigrationInventoryEntry[]> {
  const params = filter ? `?filter=${encodeURIComponent(filter)}` : '';
  return apiRequest<MigrationInventoryEntry[]>(`/migration/backlog${params}`);
}

/**
 * @param templateId persisted id
 */
export function analyzeMigration(templateId: number): Promise<MigrationAnalysis> {
  return apiRequest<MigrationAnalysis>(`/migration/templates/${templateId}/analyze`);
}

/**
 * @param templateId persisted id
 */
export function buildMigrationDraft(templateId: number): Promise<TemplateV2Draft> {
  return apiRequest<TemplateV2Draft>(`/migration/templates/${templateId}/draft`, { method: 'POST' });
}

/**
 * @param templateId persisted id
 */
export function compareMigration(templateId: number): Promise<MigrationCompareReport> {
  return apiRequest<MigrationCompareReport>(`/migration/templates/${templateId}/compare`, {
    method: 'POST',
    body: JSON.stringify({}),
  });
}

/**
 * @param templateId persisted id
 * @param request sign-off body
 */
export function signoffMigration(
  templateId: number,
  request: MigrationSignoffRequest,
): Promise<MigrationInventoryEntry> {
  return apiRequest<MigrationInventoryEntry>(`/migration/templates/${templateId}/signoff`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * @param templateId persisted id
 */
export function promoteMigration(templateId: number): Promise<TemplateV2Draft> {
  return apiRequest<TemplateV2Draft>(`/migration/templates/${templateId}/promote`, { method: 'POST' });
}

/**
 * @param templateId persisted id
 */
export function fetchMigrationInventory(templateId: number): Promise<MigrationInventoryEntry> {
  return apiRequest<MigrationInventoryEntry>(`/migration/templates/${templateId}/inventory`);
}
