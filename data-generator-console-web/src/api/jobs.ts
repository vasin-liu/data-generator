import { apiRequest } from './client';
import type { TaskExecutionSummary } from './types';

/**
 * @param templateId optional filter
 */
export function fetchJobs(templateId?: string): Promise<TaskExecutionSummary[]> {
  const suffix = templateId != null ? `?templateId=${templateId}` : '';
  return apiRequest<TaskExecutionSummary[]>(`/jobs${suffix}`);
}

/**
 * @param instanceId snowflake instance id
 */
export function fetchJob(instanceId: string): Promise<TaskExecutionSummary> {
  return apiRequest<TaskExecutionSummary>(`/jobs/${instanceId}`);
}
