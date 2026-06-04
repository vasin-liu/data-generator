import { useMutation } from '@tanstack/react-query';
import { Alert, Button, Modal, Space, Typography, message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  applyTemplateYaml,
  exportDraftYaml,
  fetchTemplateYaml,
  parseDraftYaml,
} from '../../api/editor';
import type { TemplateEditorPayload, TemplateV2Draft } from '../../api/types';
import { YamlEditor } from './YamlEditor';

type Props = {
  draft: TemplateV2Draft;
  templateId: string | null;
  saveAllowed: boolean;
  v1Yaml: string | null;
  onApplied: (payload: TemplateEditorPayload) => void;
  onDraftParsed: (draft: TemplateV2Draft) => void;
};

/**
 * YAML advanced mode with CodeMirror and form round-trip.
 */
export function YamlPanel({
  draft,
  templateId,
  saveAllowed,
  v1Yaml,
  onApplied,
  onDraftParsed,
}: Props) {
  const { t } = useTranslation();
  const [yaml, setYaml] = useState('');
  const [dirty, setDirty] = useState(false);
  const bootstrappedRef = useRef(false);

  const loadMutation = useMutation({
    mutationFn: () => fetchTemplateYaml(templateId!),
    onSuccess: (text) => {
      setYaml(text);
      setDirty(false);
    },
    onError: (err: Error) => message.error(err.message),
  });

  const syncFromFormMutation = useMutation({
    mutationFn: () => exportDraftYaml(draft),
    onSuccess: (text) => {
      setYaml(text);
      setDirty(false);
      message.success(t('editor.yaml.synced'));
    },
    onError: (err: Error) => message.error(err.message),
  });

  useEffect(() => {
    if (!saveAllowed || v1Yaml || bootstrappedRef.current) {
      return;
    }
    if (templateId != null && yaml === '' && !loadMutation.isPending) {
      bootstrappedRef.current = true;
      loadMutation.mutate();
      return;
    }
    if (templateId == null && yaml === '' && !syncFromFormMutation.isPending) {
      bootstrappedRef.current = true;
      syncFromFormMutation.mutate();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- one-time yaml bootstrap
  }, [templateId, saveAllowed, v1Yaml]);

  const applyPersistedMutation = useMutation({
    mutationFn: () => applyTemplateYaml(templateId!, yaml),
    onSuccess: (payload) => {
      message.success(t('review.yaml.applied'));
      setDirty(false);
      onApplied(payload);
    },
    onError: (err: Error) => message.error(err.message),
  });

  const applyDraftMutation = useMutation({
    mutationFn: () => parseDraftYaml(yaml),
    onSuccess: (parsed) => {
      message.success(t('review.yaml.applied'));
      setDirty(false);
      onDraftParsed(parsed);
    },
    onError: (err: Error) => message.error(err.message),
  });

  const confirmApply = () => {
    Modal.confirm({
      title: t('editor.yaml.confirm.title'),
      content: t('editor.yaml.confirm.text'),
      onOk: () => {
        if (templateId != null) {
          applyPersistedMutation.mutate();
        } else {
          applyDraftMutation.mutate();
        }
      },
    });
  };

  if (v1Yaml) {
    return (
      <>
        <Alert type="info" message={t('editor.v1.note')} style={{ marginBottom: 16 }} />
        <Typography.Text strong>{t('editor.v1.yaml')}</Typography.Text>
        <div style={{ marginTop: 8 }}>
          <YamlEditor value={v1Yaml} readOnly />
        </div>
      </>
    );
  }

  const applying = applyPersistedMutation.isPending || applyDraftMutation.isPending;

  return (
    <div>
      <Typography.Paragraph type="secondary">{t('editor.yaml.hint')}</Typography.Paragraph>
      <Space wrap style={{ marginBottom: 8 }}>
        <Button
          loading={syncFromFormMutation.isPending}
          disabled={!saveAllowed}
          onClick={() => syncFromFormMutation.mutate()}
        >
          {t('editor.yaml.sync')}
        </Button>
        {templateId != null ? (
          <Button disabled={!saveAllowed} onClick={() => loadMutation.mutate()}>
            {t('editor.yaml.reload')}
          </Button>
        ) : null}
        <Button type="primary" disabled={!saveAllowed} loading={applying} onClick={confirmApply}>
          {t('editor.yaml.apply')}
        </Button>
      </Space>
      <YamlEditor
        value={yaml}
        readOnly={!saveAllowed}
        onChange={(v) => {
          setYaml(v);
          setDirty(true);
        }}
      />
      {dirty && (
        <Typography.Text type="warning" style={{ display: 'block', marginTop: 8 }}>
          {t('editor.yaml.unsaved')}
        </Typography.Text>
      )}
    </div>
  );
}
