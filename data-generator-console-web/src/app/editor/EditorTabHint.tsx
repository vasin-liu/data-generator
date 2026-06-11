import { Alert } from 'antd';
import { useTranslation } from 'react-i18next';

type Props = {
  tab: 'general' | 'sources' | 'transform' | 'sinks' | 'execution' | 'workflow' | 'review';
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

  if (tab === 'transform') {
    return (
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={t('editor.hint.transform.title')}
        description={t('editor.hint.transform.body')}
      />
    );
  }

  if (tab === 'sinks') {
    return (
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={t('editor.hint.sinks.title')}
        description={t('editor.hint.sinks.body')}
      />
    );
  }

  if (tab === 'execution') {
    return (
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={t('editor.hint.execution.title')}
        description={t('editor.hint.execution.body')}
      />
    );
  }

  if (tab === 'workflow') {
    return (
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={t('editor.hint.workflow.title')}
        description={t('editor.hint.workflow.body')}
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
