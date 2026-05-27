import { apiRequest } from './client';
import type { ConsoleRuntime } from './types';

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
