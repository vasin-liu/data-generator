import { Tag } from 'antd';
import { useTranslation } from 'react-i18next';

type Props = {
  status: string | null | undefined;
};

function statusColor(status: string): 'success' | 'processing' | 'default' | 'warning' {
  if (status === 'PUBLISHED') {
    return 'success';
  }
  if (status === 'DRAFT') {
    return 'processing';
  }
  if (status === 'ARCHIVED') {
    return 'default';
  }
  return 'warning';
}

/**
 * Template lifecycle status badge shared by list and editor.
 */
export function TemplateStatusTag({ status }: Props) {
  const { t } = useTranslation();
  if (!status) {
    return null;
  }
  return (
    <Tag color={statusColor(status)}>
      {t(`template.status.${status}`, { defaultValue: status })}
    </Tag>
  );
}
