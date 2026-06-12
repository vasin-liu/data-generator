import { apiRequest } from './client';
import type { AiCatalog } from './types';

/**
 * @returns bundled AI providers, parsers, and prompt templates for the source editor
 */
export function fetchAiCatalog(): Promise<AiCatalog> {
  return apiRequest<AiCatalog>('/console/ai/catalog');
}
