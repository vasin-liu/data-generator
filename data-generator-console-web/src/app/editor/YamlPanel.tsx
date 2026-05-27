import { useMutation } from '@tanstack/react-query';
import { Alert, Button, Input, Modal, Space, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { applyTemplateYaml, fetchTemplateYaml } from '../../api/editor';
import type { TemplateEditorPayload } from '../../api/types';

type Props = {
  templateId: number | null;
  saveAllowed: boolean;
  v1Yaml: string | null;
  onApplied: (payload: TemplateEditorPayload) => void;
};

/**
 * YAML advanced mode (persisted templates only).
 */
export function YamlPanel({ templateId, saveAllowed, v1Yaml, onApplied }: Props) {
  const { t } = useTranslation();
  const [yaml, setYaml] = useState('');
  const [dirty, setDirty] = useState(false);

  const loadMutation = useMutation({
    mutationFn: () => fetchTemplateYaml(templateId!),
    onSuccess: (text) => {
      setYaml(text);
      setDirty(false);
    },
    onError: (err: Error) => message.error(err.message),
  });

  useEffect(() => {
    if (templateId != null && saveAllowed && !v1Yaml) {
      loadMutation.mutate();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- load when id becomes available
  }, [templateId, saveAllowed, v1Yaml]);

  const applyMutation = useMutation({
    mutationFn: () => applyTemplateYaml(templateId!, yaml),
    onSuccess: (payload) => {
      message.success(t('review.yaml.applied'));
      setDirty(false);
      onApplied(payload);
    },
    onError: (err: Error) => message.error(err.message),
  });

  const confirmApply = () => {
    Modal.confirm({
      title: t('editor.yaml.confirm.title'),
      content: t('editor.yaml.confirm.text'),
      onOk: () => applyMutation.mutate(),
    });
  };

  if (v1Yaml) {
    return (
      <>
        <Alert type="info" message={t('editor.v1.note')} style={{ marginBottom: 16 }} />
        <Typography.Text strong>{t('editor.v1.yaml')}</Typography.Text>
        <Input.TextArea rows={16} readOnly value={v1Yaml} style={{ marginTop: 8, fontFamily: 'monospace' }} />
      </>
    );
  }

  if (templateId == null) {
    return <Alert type="info" message={t('editor.migration.saveFirst')} />;
  }

  return (
    <div>
      <Typography.Paragraph type="secondary">{t('editor.yaml.hint')}</Typography.Paragraph>
      <Space style={{ marginBottom: 8 }}>
        <Button onClick={() => loadMutation.mutate()} disabled={!saveAllowed}>
          {t('editor.yaml.sync')}
        </Button>
        <Button type="primary" disabled={!saveAllowed} loading={applyMutation.isPending} onClick={confirmApply}>
          {t('editor.yaml.apply')}
        </Button>
      </Space>
      <Input.TextArea
        rows={20}
        readOnly={!saveAllowed}
        value={yaml}
        style={{ fontFamily: 'monospace' }}
        onChange={(e) => {
          setYaml(e.target.value);
          setDirty(true);
        }}
      />
      {dirty && (
        <Typography.Text type="warning" style={{ display: 'block', marginTop: 8 }}>
          {t('editor.yaml.hint')}
        </Typography.Text>
      )}
    </div>
  );
}
