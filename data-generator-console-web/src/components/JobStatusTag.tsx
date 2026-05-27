import { Tag } from 'antd';
import { useTranslation } from 'react-i18next';

const COLOR: Record<string, string> = {
  QUEUED: 'gold',
  RUNNING: 'processing',
  SUCCESS: 'success',
  FAILED: 'error',
  CANCELLED: 'default',
};

type JobStatusTagProps = {
  status: string | null | undefined;
};

/**
 * Localized status pill aligned with Vaadin {@code dg-job-status} semantics.
 */
export function JobStatusTag({ status }: JobStatusTagProps) {
  const { t } = useTranslation();
  const raw = status ?? '—';
  const label = status ? t(`status.${status}`, { defaultValue: status }) : raw;
  const color = status ? COLOR[status] ?? 'default' : 'default';
  return <Tag color={color}>{label}</Tag>;
}
