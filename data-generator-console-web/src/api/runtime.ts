import { apiRequest } from './client';
import type { ConsoleRuntime, EditorDataSources } from './types';

/**
 * @returns navbar runtime flags
 */
export function fetchConsoleRuntime(): Promise<ConsoleRuntime> {
  return apiRequest<ConsoleRuntime>('/console/runtime');
}

/**
 * @returns JDBC datasource keys for editor dropdowns
 */
export function fetchJdbcNames(): Promise<string[]> {
  return apiRequest<string[]>('/console/jdbc-names');
}

/**
 * @returns JDBC, Kafka, and Elasticsearch keys for editor dropdowns
 */
export function fetchEditorDataSources(): Promise<EditorDataSources> {
  return apiRequest<EditorDataSources>('/console/editor-data-sources');
}
