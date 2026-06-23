import { apiFormRequest, apiRequest } from './client';
import type { UdfGroupView, UdfVersionView } from './types';

/**
 * @param type optional UDF type filter (java-plugin | script | sql)
 */
export function fetchUdfs(type?: string): Promise<UdfGroupView[]> {
  const suffix = type != null && type !== '' ? `?type=${encodeURIComponent(type)}` : '';
  return apiRequest<UdfGroupView[]>(`/console/udfs${suffix}`);
}

/**
 * @param form multipart upload body: udfId, version, type + type-specific parts —
 *   java-plugin: file; script: scriptBody + sqlName + argCount? + returnType? + inputSchema + outputSchema;
 *   sql: sql + sqlName + argCount? + returnType?
 */
export function uploadUdf(form: FormData): Promise<UdfVersionView> {
  return apiFormRequest<UdfVersionView>('/console/udfs', form);
}

/**
 * @param udfId   reverse-DNS identifier
 * @param version semver version
 */
export function publishUdf(udfId: string, version: string): Promise<UdfVersionView> {
  return apiRequest<UdfVersionView>(
    `/console/udfs/${encodeURIComponent(udfId)}/${encodeURIComponent(version)}/publish`,
    { method: 'POST' },
  );
}

/**
 * @param udfId   reverse-DNS identifier
 * @param version semver version
 */
export function deprecateUdf(udfId: string, version: string): Promise<UdfVersionView> {
  return apiRequest<UdfVersionView>(
    `/console/udfs/${encodeURIComponent(udfId)}/${encodeURIComponent(version)}/deprecate`,
    { method: 'POST' },
  );
}
