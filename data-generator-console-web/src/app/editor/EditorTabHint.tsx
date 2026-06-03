import { Alert } from 'antd';
import { useTranslation } from 'react-i18next';

type Props = {
  tab: 'general' | 'sources' | 'review';
  isNew: boolean;
};

/**
 * Contextual hints shown at the top of key editor tabs.
 */
export function EditorTabHint({ tab, isNew }: Props) {
  const { t } = useTranslation();

  if (tab === 'general' && isNew) {
    return (
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={t('editor.hint.new.title')}
        description={t('editor.hint.new.body')}
      />
    );
  }

  if (tab === 'sources') {
    return (
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={t('editor.hint.sources.title')}
        description={t('editor.hint.sources.body')}
      />
    );
  }

  if (tab === 'review') {
    return (
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={t('editor.hint.review.title')}
        description={t('editor.hint.review.body')}
      />
    );
  }

  return null;
}
