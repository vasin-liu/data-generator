import { apiRequest } from './client';
import type { AiCatalog, AiModelPricing, AiQuotaStatus, AiUsageSummary } from './types';

/**
 * @returns bundled AI providers, parsers, and prompt templates for the source editor
 */
export function fetchAiCatalog(): Promise<AiCatalog> {
  return apiRequest<AiCatalog>('/console/ai/catalog');
}

/**
 * @returns platform-level AI token usage aggregated from successful job reports
 */
export function fetchAiUsage(): Promise<AiUsageSummary> {
  return apiRequest<AiUsageSummary>('/console/ai/usage');
}

/**
 * @returns effective per-model USD token pricing for operator reference
 */
export function fetchAiPricing(): Promise<AiModelPricing[]> {
  return apiRequest<AiModelPricing[]>('/console/ai/pricing');
}

/**
 * @returns platform daily AI quota limits and UTC-day consumption
 */
export function fetchAiQuota(): Promise<AiQuotaStatus> {
  return apiRequest<AiQuotaStatus>('/console/ai/quota');
}
