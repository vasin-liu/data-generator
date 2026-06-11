import { apiRequest } from './client';
import type { ScenarioCatalogEntry, TemplateEditorPayload } from './types';

/**
 * @returns official V2 scenario catalog rows for the create wizard
 */
export function fetchScenarioCatalog(): Promise<ScenarioCatalogEntry[]> {
  return apiRequest<ScenarioCatalogEntry[]>('/templates/scenarios');
}

/**
 * @param scenarioId official catalog id (e.g. GF-A)
 * @returns editor payload seeded from classpath scenario YAML
 */
export function fetchScenarioScaffold(scenarioId: string): Promise<TemplateEditorPayload> {
  return apiRequest<TemplateEditorPayload>(`/templates/scenarios/${encodeURIComponent(scenarioId)}/scaffold`);
}
