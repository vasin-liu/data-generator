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
  status: string | null;
  archived: boolean;
  saveAllowed: boolean;
  onSaved: (templateId: string) => void;
  onPublished?: () => void;
};

/**
 * Review tab: validate, preview, save, run.
 */
export function ReviewPanel({
  draft,
  templateId,
  kind,
  status,
  archived,
  saveAllowed,
  onSaved,
  onPublished,
}: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [output, setOutput] = useState('');

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
    onSuccess: () => {
      message.success(t('review.publish.done'));
      onPublished?.();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const statusParts = [
    `kind=${kind}`,
    templateId != null ? `id=${templateId}` : t('review.status.new'),
    archived ? t('review.status.archived') : '',
  ].filter(Boolean);

  const needsPublish = status !== 'PUBLISHED' && templateId != null;
  const isDraftRun = needsPublish;

  return (
    <div>
      <Typography.Paragraph type="secondary">{statusParts.join(' | ')}</Typography.Paragraph>
      {needsPublish ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message={t('review.publish.required.title')}
          description={t('review.publish.required.body')}
        />
      ) : null}
      {isDraftRun ? (
        <Alert
          type="info"
          showIcon
          data-testid="review-run-draft-hint"
          style={{ marginBottom: 16 }}
          message={t('review.run.draft.title')}
          description={t('review.run.draft.body')}
        />
      ) : null}
      <Space wrap style={{ marginBottom: 16 }}>
        <Button onClick={() => validateMutation.mutate()} loading={validateMutation.isPending}>
          {t('review.validate')}
        </Button>
        <Button
          onClick={() => previewMutation.mutate()}
          loading={previewMutation.isPending}
          title={t('review.tooltip.preview.ok')}
        >
          {t('review.preview')}
        </Button>
        <Button
          type="primary"
          data-testid="review-save"
          disabled={!saveAllowed}
          loading={saveMutation.isPending}
          onClick={() => saveMutation.mutate(false)}
        >
          {t('review.save')}
        </Button>
        <Button
          data-testid="review-save-and-return"
          disabled={!saveAllowed}
          loading={saveMutation.isPending}
          onClick={() => saveMutation.mutate(true)}
        >
          {t('review.saveAndReturn')}
        </Button>
        <Button
          type="primary"
          disabled={!saveAllowed || templateId == null}
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
          disabled={!saveAllowed}
          loading={runMutation.isPending}
          onClick={() => runMutation.mutate()}
          title={isDraftRun ? t('review.run.draft.title') : t('review.tooltip.run.ok')}
        >
          {isDraftRun ? t('review.run.draft') : t('review.run')}
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
