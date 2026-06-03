import { useMutation } from '@tanstack/react-query';
import { Alert, Button, Modal, Space, Typography, message } from 'antd';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { createTemplate, publishTemplate, previewDraft, runDraft, saveTemplate, validateDraft } from '../../api/editor';
import type { TemplateDefinitionKind, TemplateV2Draft } from '../../api/types';

type Props = {
  draft: TemplateV2Draft;
  templateId: string | null;
  kind: TemplateDefinitionKind;
  archived: boolean;
  saveAllowed: boolean;
  onSaved: (templateId: string) => void;
};

/**
 * Review tab: validate, preview, save, run.
 */
export function ReviewPanel({
  draft,
  templateId,
  kind,
  archived,
  saveAllowed,
  onSaved,
}: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [output, setOutput] = useState('');

  const v1Blocked = kind === 'V1';

  const validateMutation = useMutation({
    mutationFn: () => validateDraft(draft, templateId),
    onSuccess: (result) => {
      const lines = [
        result.valid ? t('review.validation.passed') : t('review.validation.failed'),
        ...result.errors.map((e) => `ERROR: ${e}`),
        ...result.warnings.map((w) => `WARN: ${w}`),
      ];
      setOutput(lines.join('\n'));
    },
    onError: (err: Error) => message.error(err.message),
  });

  const previewMutation = useMutation({
    mutationFn: () => previewDraft(draft, templateId),
    onSuccess: (result) => {
      if (result.templateId && !templateId) {
        onSaved(result.templateId);
      }
      setOutput(JSON.stringify(result.preview, null, 2));
      Modal.info({
        title: t('review.preview.title'),
        width: 720,
        content: (
          <pre style={{ maxHeight: 400, overflow: 'auto', fontSize: 12 }}>
            {JSON.stringify(result.preview, null, 2)}
          </pre>
        ),
      });
    },
    onError: (err: Error) => message.error(err.message),
  });

  const saveMutation = useMutation({
    mutationFn: async (andReturn: boolean) => {
      const payload =
        templateId != null
          ? await saveTemplate(templateId, draft)
          : await createTemplate(draft);
      const id = payload.templateId ?? templateId;
      if (id != null) {
        onSaved(id);
        message.success(t('review.saved', { id }));
      }
      if (andReturn) {
        navigate('/templates');
      }
      return payload;
    },
    onError: (err: Error) => message.error(err.message),
  });

  const runMutation = useMutation({
    mutationFn: () => runDraft(draft, templateId),
    onSuccess: (data) => {
      onSaved(data.templateId);
      message.success(t('review.run.started'));
      navigate(`/jobs/${data.instanceId}`);
    },
    onError: (err: Error) => message.error(err.message),
  });

  const publishMutation = useMutation({
    mutationFn: () => {
      if (templateId == null) {
        return Promise.reject(new Error(t('review.publish.needsSave')));
      }
      return publishTemplate(templateId);
    },
    onSuccess: () => message.success(t('review.publish.done')),
    onError: (err: Error) => message.error(err.message),
  });

  const statusParts = [
    `kind=${kind}`,
    templateId != null ? `id=${templateId}` : t('review.status.new'),
    archived ? t('review.status.archived') : '',
  ].filter(Boolean);

  return (
    <div>
      <Typography.Paragraph type="secondary">{statusParts.join(' | ')}</Typography.Paragraph>
      <Space wrap style={{ marginBottom: 16 }}>
        <Button onClick={() => validateMutation.mutate()} loading={validateMutation.isPending}>
          {t('review.validate')}
        </Button>
        <Button
          onClick={() => previewMutation.mutate()}
          loading={previewMutation.isPending}
          disabled={v1Blocked}
          title={v1Blocked ? t('review.tooltip.preview.v1') : t('review.tooltip.preview.ok')}
        >
          {t('review.preview')}
        </Button>
        <Button
          type="primary"
          disabled={!saveAllowed}
          loading={saveMutation.isPending}
          onClick={() => saveMutation.mutate(false)}
        >
          {t('review.save')}
        </Button>
        <Button
          disabled={!saveAllowed}
          loading={saveMutation.isPending}
          onClick={() => saveMutation.mutate(true)}
        >
          {t('review.saveAndReturn')}
        </Button>
        <Button
          type="primary"
          disabled={!saveAllowed || templateId == null || v1Blocked}
          loading={publishMutation.isPending}
          onClick={() =>
            Modal.confirm({
              title: t('review.publish'),
              content: t('review.publish.confirm'),
              onOk: () => publishMutation.mutateAsync(),
            })
          }
        >
          {t('review.publish')}
        </Button>
        <Button
          type="primary"
          danger
          disabled={!saveAllowed || v1Blocked}
          loading={runMutation.isPending}
          onClick={() => runMutation.mutate()}
          title={v1Blocked ? t('review.tooltip.run.v1') : t('review.tooltip.run.ok')}
        >
          {t('review.run')}
        </Button>
        {templateId != null && (
          <Button onClick={() => navigate(`/jobs?templateId=${templateId}`)}>
            {t('templates.viewJobs')}
          </Button>
        )}
      </Space>
      {output.length > 0 ? (
        <Alert
          type={output.includes('ERROR:') ? 'error' : output.includes('WARN:') ? 'warning' : 'success'}
          showIcon
          style={{ marginBottom: 8 }}
          message={t('review.validation')}
          description={
            <pre
              style={{
                margin: 0,
                marginTop: 8,
                whiteSpace: 'pre-wrap',
                fontFamily: 'inherit',
                fontSize: 12,
              }}
            >
              {output}
            </pre>
          }
        />
      ) : (
        <Typography.Paragraph type="secondary">{t('review.validation.empty')}</Typography.Paragraph>
      )}
    </div>
  );
}
