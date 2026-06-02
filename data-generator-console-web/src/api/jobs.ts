import { apiRequest } from './client';
import type { JobExecutionDetail, TaskExecutionSummary } from './types';

export type { RunReport, StageMetric } from './types';

/**
 * @param templateId optional filter
 * @param triggerType optional MANUAL or SCHEDULED filter
 */
export function fetchJobs(templateId?: string, triggerType?: string): Promise<TaskExecutionSummary[]> {
  const params = new URLSearchParams();
  if (templateId != null) {
    params.set('templateId', templateId);
  }
  if (triggerType != null && triggerType.trim()) {
    params.set('triggerType', triggerType.trim());
  }
  const query = params.toString();
  const suffix = query ? `?${query}` : '';
  return apiRequest<TaskExecutionSummary[]>(`/jobs${suffix}`);
}

/**
 * @param instanceId snowflake instance id
 * @returns execution summary including structured {@code report} when available
 */
export function fetchJob(instanceId: string): Promise<JobExecutionDetail> {
  return apiRequest<JobExecutionDetail>(`/jobs/${instanceId}`);
}

/** Best-effort cancel for QUEUED, RUNNING, or PAUSED runs. */
export function cancelJob(instanceId: string): Promise<string> {
  return apiRequest<string>(`/jobs/${instanceId}/cancel`, { method: 'POST' });
}

/** Resumes a workflow run blocked on manual pause. */
export function resumeJob(instanceId: string): Promise<string> {
  return apiRequest<string>(`/jobs/${instanceId}/resume`, { method: 'POST' });
}
