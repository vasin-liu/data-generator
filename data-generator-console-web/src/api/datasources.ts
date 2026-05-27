import { apiFormRequest, apiRequest } from './client';
import type {
  DataSourceTestRequest,
  DataSourcesOverview,
} from './types';

/**
 * @returns persisted configs and runtime JDBC keys
 */
export function fetchDataSources(): Promise<DataSourcesOverview> {
  return apiRequest<DataSourcesOverview>('/datasources');
}

/**
 * @param form multipart fields (name, url, username, password, driverClassName, optional driverFile)
 */
export function upsertDataSource(form: FormData): Promise<string> {
  return apiFormRequest<string>('/datasources', form);
}

/**
 * @param name datasource key
 */
export function removeDataSource(name: string): Promise<string> {
  return apiRequest<string>(`/datasources/${encodeURIComponent(name)}`, { method: 'DELETE' });
}

/**
 * @param request JDBC parameters
 */
export function testDataSourceConnection(request: DataSourceTestRequest): Promise<string> {
  return apiRequest<string>('/datasources/test', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * @param name persisted datasource key
 */
export function testDataSourceByName(name: string): Promise<string> {
  return apiRequest<string>(`/datasources/${encodeURIComponent(name)}/test`, { method: 'POST' });
}
