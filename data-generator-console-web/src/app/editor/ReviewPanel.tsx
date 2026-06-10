import { useMutation, useQuery } from '@tanstack/react-query';
import { Alert, App, Button, Select, Space, Typography, message } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { canPublishWithRole, getConsoleRole } from '../../api/consoleRole';
import { fetchConsoleRuntime } from '../../api/runtime';
import { createTemplate, publishTemplate, previewDraft, runDraft, saveTemplate, validateDraft } from '../../api/editor';
import type { TemplateDefinitionKind, TemplateV2Draft } from '../../api/types';
import { listTransformers } from '../editor/draftUtils';
import { hasWorkflow, listDagPreviewTargets } from '../editor/workflowUtils';

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
  const { modal } = App.useApp();
  const [output, setOutput] = useState('');
  const [throughTransformIndex, setThroughTransformIndex] = useState<number | undefined>(undefined);
  const [dagPreviewKey, setDagPreviewKey] = useState<string | undefined>(undefined);
  const [consoleRole, setConsoleRoleState] = useState(() => getConsoleRole());

  useEffect(() => {
    const syncRole = () => setConsoleRoleState(getConsoleRole());
    window.addEventListener('console-role-changed', syncRole);
    return () => window.removeEventListener('console-role-changed', syncRole);
  }, []);

  const runtimeQuery = useQuery({
    queryKey: ['console-runtime'],
    queryFn: fetchConsoleRuntime,
  });
  const publishAllowed =
    !runtimeQuery.data?.consoleSecurityEnabled || canPublishWithRole(consoleRole);

  const transformerSteps = useMemo(() => listTransformers(draft), [draft]);
  const dagPreviewTargets = useMemo(() => listDagPreviewTargets(draft), [draft]);
  const stagedLinearAvailable = !hasWorkflow(draft) && transformerSteps.length > 0;
  const stagedDagAvailable = dagPreviewTargets.length > 0;
  const stagedPreviewAvailable = stagedLinearAvailable || stagedDagAvailable;
  const stagedPreviewOptions = useMemo(
    () =>
      transformerSteps.map((step, index) => ({
        value: index,
        label: t('review.preview.throughStep', {
          index: index + 1,
          name: step.name?.trim() || step.type || `step-${index + 1}`,
        }),
      })),
    [transformerSteps, t],
  );
  const dagPreviewOptions = useMemo(
    () =>
      dagPreviewTargets.map((target) => ({
        value: `${target.computeBlockId}|${target.nodeId}`,
        label: t('review.preview.throughDagNode', { label: target.label }),
      })),
    [dagPreviewTargets, t],
  );

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
    mutationFn: () => {
      const dagTarget = dagPreviewKey?.split('|');
      return previewDraft(
        draft,
        templateId,
        undefined,
        dagPreviewKey ? undefined : throughTransformIndex,
        dagTarget?.[1],
        dagTarget?.[0],
      );
    },
    onSuccess: (result) => {
      if (result.templateId && !templateId) {
        onSaved(result.templateId);
      }
      setOutput(JSON.stringify(result.preview, null, 2));
      modal.info({
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
      {stagedPreviewAvailable ? (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message={t('review.preview.staged.title')}
          description={t('review.preview.staged.body')}
        />
      ) : null}
      {!publishAllowed && runtimeQuery.data?.consoleSecurityEnabled ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message={t('review.publish.rbac.title')}
          description={t('review.publish.rbac.body')}
        />
      ) : null}
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
        {stagedLinearAvailable ? (
          <Select
            allowClear
            style={{ minWidth: 220 }}
            placeholder={t('review.preview.throughPlaceholder')}
            value={dagPreviewKey ? undefined : throughTransformIndex}
            options={stagedPreviewOptions}
            onChange={(value) => {
              setDagPreviewKey(undefined);
              setThroughTransformIndex(value ?? undefined);
            }}
            data-testid="review-preview-through-select"
          />
        ) : null}
        {stagedDagAvailable ? (
          <Select
            allowClear
            style={{ minWidth: 260 }}
            placeholder={t('review.preview.throughDagPlaceholder')}
            value={dagPreviewKey}
            options={dagPreviewOptions}
            onChange={(value) => {
              setThroughTransformIndex(undefined);
              setDagPreviewKey(value ?? undefined);
            }}
            data-testid="review-preview-dag-select"
          />
        ) : null}
        <Button
          onClick={() => previewMutation.mutate()}
          loading={previewMutation.isPending}
          title={t('review.tooltip.preview.ok')}
          data-testid="review-preview"
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
          disabled={!saveAllowed || templateId == null || !publishAllowed}
          loading={publishMutation.isPending}
          data-testid="review-publish"
          onClick={() =>
            modal.confirm({
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
          data-testid="review-run"
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
