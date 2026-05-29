import { useMutation, useQuery } from '@tanstack/react-query';
import { Alert, Button, Form, Input, Modal, Space, Typography, message } from 'antd';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  analyzeMigration,
  buildMigrationDraft,
  compareMigration,
  fetchMigrationInventory,
  promoteMigration,
  signoffMigration,
} from '../../api/migration';
import type { MigrationAnalysis, TemplateDefinitionKind, TemplateV2Draft } from '../../api/types';

type Props = {
  templateId: string;
  kind: TemplateDefinitionKind;
  onPromoted: () => void;
  onDraftApply: (draft: TemplateV2Draft) => void;
};

const BLOCKED_CLASSES = new Set(['COMPATIBILITY_ONLY', 'BLOCKED']);

/**
 * Per-template migration tab (analyze, draft, compare, sign-off, promote).
 */
export function MigrationTab({ templateId, kind, onPromoted, onDraftApply }: Props) {
  const { t } = useTranslation();
  const [output, setOutput] = useState('');
  const [analysis, setAnalysis] = useState<MigrationAnalysis | null>(null);
  const [signoffOpen, setSignoffOpen] = useState(false);
  const [signoffForm] = Form.useForm<{ approvedBy: string; notes: string }>();

  const inventoryQuery = useQuery({
    queryKey: ['migration-inventory', templateId],
    queryFn: () => fetchMigrationInventory(templateId),
    retry: false,
  });

  const analyzeMutation = useMutation({
    mutationFn: () => analyzeMigration(templateId),
    onSuccess: (result) => {
      setAnalysis(result);
      const lines = [
        t('migration.panel.status', {
          class: result.suggestedClass,
          path: result.recommendedPath ?? '—',
          family: result.scenarioFamily ?? '—',
          wave: result.wave ?? '—',
        }),
      ];
      if (result.blockers?.length) {
        lines.push('', t('migration.panel.blockers'), ...result.blockers);
      }
      if (result.warnings?.length) {
        lines.push('', t('migration.panel.warnings'), ...result.warnings);
      }
      setOutput(lines.join('\n'));
    },
    onError: (err: Error) => message.error(err.message),
  });

  const draftMutation = useMutation({
    mutationFn: () => buildMigrationDraft(templateId),
    onSuccess: (draft) => {
      Modal.info({
        title: t('migration.panel.draft.title'),
        width: 720,
        content: (
          <pre style={{ maxHeight: 400, overflow: 'auto', fontSize: 12 }}>
            {JSON.stringify(draft, null, 2)}
          </pre>
        ),
        okText: t('migration.panel.draft.apply'),
        onOk: () => {
          onDraftApply(draft);
          message.success(t('migration.panel.draft.applied'));
        },
      });
    },
    onError: (err: Error) => message.error(err.message),
  });

  const compareMutation = useMutation({
    mutationFn: () => compareMigration(templateId),
    onSuccess: (report) => {
      const warnings =
        report.warnings?.length > 0 ? `\n${report.warnings.join('\n')}` : '';
      setOutput(
        t('migration.panel.compare.summary', {
          class: report.classification,
          recommendation: report.recommendation ?? '—',
          v1Rows: report.v1RowCount,
          v2Rows: report.v2RowCount,
          match: (report.sampleMatchRate * 100).toFixed(2),
          report: report.reportPath ?? '—',
          warnings,
        }),
      );
      inventoryQuery.refetch();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const promoteMutation = useMutation({
    mutationFn: () => promoteMigration(templateId),
    onSuccess: () => {
      message.success(t('migration.panel.promote.done'));
      onPromoted();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const signoffMutation = useMutation({
    mutationFn: (values: { approvedBy: string; notes: string }) =>
      signoffMigration(templateId, {
        approved: true,
        approvedBy: values.approvedBy,
        notes: values.notes,
      }),
    onSuccess: (entry) => {
      message.success(
        t('migration.panel.signoff.done', {
          id: entry.id,
          at: entry.businessSignoffAt ?? '—',
        }),
      );
      setSignoffOpen(false);
      inventoryQuery.refetch();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const promoteBlocked = useMemo(() => {
    if (kind === 'V2') {
      return true;
    }
    if (analysis) {
      if (
        analysis.suggestedClass === 'COMPATIBILITY_ONLY' ||
        analysis.recommendedPath?.toLowerCase() === 'compatibility_only'
      ) {
        return true;
      }
    }
    const invClass = inventoryQuery.data?.migrationClass;
    return invClass != null && BLOCKED_CLASSES.has(invClass);
  }, [kind, analysis, inventoryQuery.data]);

  const inventoryHint = inventoryQuery.isSuccess
    ? t('migration.panel.inventory', {
        class: inventoryQuery.data.migrationClass ?? '—',
        signoff: inventoryQuery.data.businessSignoffApproved
          ? t('migration.panel.inventory.signed')
          : t('migration.panel.inventory.pending'),
      })
    : t('migration.panel.inventory.missing', { id: templateId });

  const runAnalyze = () => {
    if (kind === 'V2') {
      message.info(t('migration.panel.alreadyV2'));
      return;
    }
    analyzeMutation.mutate();
  };

  return (
    <div>
      <Typography.Paragraph>{t('migration.panel.intro', { id: templateId })}</Typography.Paragraph>
      <Typography.Paragraph type="secondary">{inventoryHint}</Typography.Paragraph>
      <Space wrap style={{ marginBottom: 16 }}>
        <Button onClick={runAnalyze} loading={analyzeMutation.isPending}>
          {t('migration.panel.analyze')}
        </Button>
        <Button onClick={() => draftMutation.mutate()} loading={draftMutation.isPending}>
          {t('migration.panel.draft')}
        </Button>
        <Button onClick={() => compareMutation.mutate()} loading={compareMutation.isPending}>
          {t('migration.panel.compare')}
        </Button>
        <Button onClick={() => setSignoffOpen(true)}>{t('migration.panel.signoff')}</Button>
        <Button
          type="primary"
          disabled={promoteBlocked}
          title={promoteBlocked ? t('migration.panel.promote.blocked') : undefined}
          loading={promoteMutation.isPending}
          onClick={() => promoteMutation.mutate()}
        >
          {t('migration.panel.promote')}
        </Button>
      </Space>
      {promoteBlocked && kind !== 'V2' && (
        <Alert type="warning" message={t('migration.panel.promote.blocked')} style={{ marginBottom: 12 }} />
      )}
      <Typography.Text strong>{t('migration.panel.result')}</Typography.Text>
      <pre
        style={{
          marginTop: 8,
          minHeight: 200,
          padding: 12,
          background: '#f5f5f5',
          borderRadius: 4,
          overflow: 'auto',
        }}
      >
        {output || '—'}
      </pre>

      <Modal
        title={t('migration.panel.signoff.title', { id: templateId })}
        open={signoffOpen}
        onCancel={() => setSignoffOpen(false)}
        onOk={() => signoffForm.submit()}
        confirmLoading={signoffMutation.isPending}
        okText={t('migration.panel.signoff.record')}
      >
        <Form form={signoffForm} layout="vertical" onFinish={(v) => signoffMutation.mutate(v)}>
          <Form.Item name="approvedBy" label={t('migration.panel.signoff.approvedBy')}>
            <Input />
          </Form.Item>
          <Form.Item name="notes" label={t('migration.panel.signoff.notes')}>
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
