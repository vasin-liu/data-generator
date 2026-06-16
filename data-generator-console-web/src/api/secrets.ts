import { apiRequest } from './client';
import type { SecretSummary } from './types';

/**
 * @returns secret names for template secretRef pickers (values are never returned)
 */
export function fetchSecretSummaries(): Promise<SecretSummary[]> {
  return apiRequest<SecretSummary[]>('/secrets');
}
