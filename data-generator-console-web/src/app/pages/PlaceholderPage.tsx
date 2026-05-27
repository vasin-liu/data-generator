import { Typography } from 'antd';
import { useTranslation } from 'react-i18next';

type PlaceholderPageProps = {
  messageKey: string;
};

/**
 * Stub route until M4/M5 pages land.
 */
export function PlaceholderPage({ messageKey }: PlaceholderPageProps) {
  const { t } = useTranslation();
  return (
    <section>
      <Typography.Paragraph>{t(messageKey)}</Typography.Paragraph>
    </section>
  );
}
