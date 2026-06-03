import type { TFunction } from 'i18next';

/**
 * Maps backend trigger type codes to localized labels.
 */
export function triggerTypeLabel(t: TFunction, triggerType?: string | null): string {
  if (triggerType === 'MANUAL') {
    return t('jobs.trigger.manual');
  }
  if (triggerType === 'SCHEDULED') {
    return t('jobs.trigger.scheduled');
  }
  return triggerType ?? '—';
}
