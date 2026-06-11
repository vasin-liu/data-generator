import { apiRequest } from './client';
import type { DistributedQueueMetrics } from './types';

/** Read-only distributed queue and worker health snapshot. */
export function fetchDistributedMetrics(): Promise<DistributedQueueMetrics> {
  return apiRequest<DistributedQueueMetrics>('/console/distributed/metrics');
}
