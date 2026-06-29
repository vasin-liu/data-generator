import { useMutation, useQuery } from '@tanstack/react-query';
import { Alert, Button, Descriptions, Space, Spin, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { cancelJob, fetchJob, resumeJob } from '../../api/jobs';
import type { AiCallMetric, RunReport, StageMetric } from '../../api/types';
import { JobStatusTag } from '../../components/JobStatusTag';
import { ConsolePageHeader } from '../../components/ConsolePageHeader';
import { enumLabel } from '../utils/optionLabels';
import { formatDateTime } from '../utils/formatDateTime';
import { triggerTypeLabel } from '../utils/triggerType';

const ACTIVE = new Set(['QUEUED', 'RUNNING', 'PAUSED']);

function formatDuration(ms: number | null | undefined): string {
  if (ms == null) {
    return '—';
  }
  return `${ms} ms`;
}

/**
 * Single execution detail with polling until finished.
 */
export function JobDetailPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { instanceId: instanceIdParam } = useParams();
  const instanceId = instanceIdParam?.trim() ?? '';

  const jobQuery = useQuery({
    queryKey: ['job', instanceId],
    queryFn: () => fetchJob(instanceId),
    enabled: instanceId.length > 0,
    refetchInterval: (query) => {
      const status = query.state.data?.execution.status;
      return status && ACTIVE.has(status) ? 2000 : false;
    },
  });

  const cancelMutation = useMutation({
    mutationFn: () => cancelJob(instanceId),
    onSuccess: () => {
      message.success(t('jobDetail.cancel.done'));
      jobQuery.refetch();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const resumeMutation = useMutation({
    mutationFn: () => resumeJob(instanceId),
    onSuccess: () => {
      message.success(t('jobDetail.resume.done'));
      jobQuery.refetch();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const stageColumns: ColumnsType<StageMetric> = useMemo(
    () => [
      { title: t('jobDetail.report.col.name'), dataIndex: 'name' },
      {
        title: t('jobDetail.report.col.rows'),
        dataIndex: 'rowsProcessed',
        render: (value: number | null) => value ?? '—',
      },
      {
        title: t('jobDetail.report.col.duration'),
        dataIndex: 'durationMs',
        render: (value: number | null) => formatDuration(value),
      },
      {
        title: t('jobDetail.report.col.error'),
        dataIndex: 'errorSample',
        render: (value: string | null) => value ?? '—',
      },
    ],
    [t],
  );

  const aiCallColumns: ColumnsType<AiCallMetric> = useMemo(
    () => [
      { title: t('jobDetail.report.col.name'), dataIndex: 'sourceName' },
      { title: t('jobDetail.report.ai.provider'), dataIndex: 'providerType' },
      { title: t('jobDetail.report.ai.model'), dataIndex: 'model' },
      {
        title: t('jobDetail.report.ai.promptTokens'),
        dataIndex: 'promptTokens',
        render: (value: number | null) => value ?? '—',
      },
      {
        title: t('jobDetail.report.ai.completionTokens'),
        dataIndex: 'completionTokens',
        render: (value: number | null) => value ?? '—',
      },
      {
        title: t('jobDetail.report.col.duration'),
        dataIndex: 'latencyMs',
        render: (value: number | null) => formatDuration(value),
      },
      {
        title: t('jobDetail.report.ai.attempts'),
        dataIndex: 'attempts',
        render: (value: number | null) => value ?? '—',
      },
      {
        title: t('jobDetail.report.ai.estimatedCost'),
        dataIndex: 'estimatedCostUsd',
        render: (value: number | null | undefined) =>
          value == null
            ? '—'
            : new Intl.NumberFormat(undefined, {
                style: 'currency',
                currency: 'USD',
                minimumFractionDigits: 2,
                maximumFractionDigits: 6,
              }).format(value),
      },
    ],
    [t],
  );

  const sinkStageColumns: ColumnsType<StageMetric> = useMemo(
    () => [
      { title: t('jobDetail.report.col.name'), dataIndex: 'name' },
      {
        title: t('jobDetail.report.col.rowsRead'),
        dataIndex: 'rowsRead',
        render: (value: number | null | undefined) => value ?? '—',
      },
      {
        title: t('jobDetail.report.col.rowsOk'),
        dataIndex: 'rowsOk',
        render: (value: number | null | undefined) => value ?? '—',
      },
      {
        title: t('jobDetail.report.col.rowsUpserted'),
        dataIndex: 'rowsUpserted',
        render: (value: number | null | undefined) => value ?? '—',
      },
      {
        title: t('jobDetail.report.col.rowsSkipped'),
        dataIndex: 'rowsSkipped',
        render: (value: number | null | undefined) => value ?? '—',
      },
      {
        title: t('jobDetail.report.col.rowsFailed'),
        dataIndex: 'rowsFailed',
        render: (value: number | null | undefined) => value ?? '—',
      },
      {
        title: t('jobDetail.report.col.rows'),
        dataIndex: 'rowsProcessed',
        render: (value: number | null) => value ?? '—',
      },
      {
        title: t('jobDetail.report.col.error'),
        dataIndex: 'errorSample',
        render: (value: string | null) => value ?? '—',
      },
    ],
    [t],
  );

  const row = jobQuery.data?.execution;
  const distributedJob = jobQuery.data?.distributedJob ?? null;
  const partitionMetrics = jobQuery.data?.partitionMetrics ?? null;
  const report = row?.report ?? null;
  const body =
    row?.metricsJson && row.metricsJson.length > 0
      ? row.metricsJson
      : row?.errorMessage ?? '';

  if (jobQuery.isLoading) {
    return <Spin style={{ display: 'block', margin: '48px auto' }} />;
  }

  if (jobQuery.isError) {
    return (
      <Alert
        type="error"
        showIcon
        message={t('jobDetail.loadError')}
        description={(jobQuery.error as Error).message}
        action={
          <Button onClick={() => navigate('/jobs')}>{t('jobDetail.back')}</Button>
        }
      />
    );
  }

  return (
    <div className="console-page-panel" data-testid="job-detail-page">
      <ConsolePageHeader
        title={`${t('jobDetail.title')} #${instanceIdParam}`}
        crumbs={[
          { label: t('nav.home'), path: '/' },
          { label: t('nav.jobs'), path: '/jobs' },
          { label: `#${instanceIdParam}` },
        ]}
        extra={
          <Space wrap>
            <Button onClick={() => navigate('/jobs')}>{t('jobDetail.back')}</Button>
            {row?.templateId != null && (
              <Button onClick={() => navigate(`/templates/${row.templateId}`)}>
                {t('jobDetail.openTemplate')}
              </Button>
            )}
            {row?.triggerType === 'SCHEDULED' && row?.scheduleId && (
              <Button
                onClick={() => {
                  const params = new URLSearchParams({
                    scheduleId: String(row.scheduleId),
                  });
                  if (row.templateId != null) {
                    params.set('templateId', String(row.templateId));
                  }
                  navigate(`/schedules?${params.toString()}`);
                }}
              >
                {t('jobDetail.openSchedule')}
              </Button>
            )}
            <Button onClick={() => jobQuery.refetch()}>{t('common.refresh')}</Button>
            {row && ACTIVE.has(row.status) && (
              <Button
                danger
                data-testid="job-cancel-button"
                loading={cancelMutation.isPending}
                onClick={() => cancelMutation.mutate()}
              >
                {t('jobDetail.cancel')}
              </Button>
            )}
            {row?.status === 'PAUSED' && (
              <Button
                type="primary"
                data-testid="job-resume-button"
                loading={resumeMutation.isPending}
                onClick={() => resumeMutation.mutate()}
              >
                {t('jobDetail.resume')}
              </Button>
            )}
          </Space>
        }
      />
      {row ? (
        <>
          {ACTIVE.has(row.status) ? (
            <Alert
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
              message={t('jobDetail.poll.active')}
              description={t('jobDetail.poll.active.body')}
            />
          ) : null}
          {row.status === 'PAUSED' && row.pauseReason ? (
            <Alert
              type="warning"
              showIcon
              data-testid="job-pause-reason"
              style={{ marginBottom: 16 }}
              message={t('jobDetail.pauseReason.title')}
              description={row.pauseReason}
            />
          ) : null}
          <Descriptions bordered size="small" column={1} style={{ marginBottom: 16 }}>
            <Descriptions.Item label={t('jobs.col.status')}>
              <JobStatusTag status={row.status} />
            </Descriptions.Item>
            <Descriptions.Item label={t('jobs.col.template')}>
              {row.templateName} (#{row.templateId})
            </Descriptions.Item>
            <Descriptions.Item label={t('jobs.col.kind')}>
              {enumLabel(t, 'jobs.kind', row.definitionKind)}
            </Descriptions.Item>
            <Descriptions.Item label={t('jobs.col.trigger')}>
              {triggerTypeLabel(t, row.triggerType)}
            </Descriptions.Item>
            <Descriptions.Item label={t('jobDetail.scheduleId')}>{row.scheduleId ?? '—'}</Descriptions.Item>
            <Descriptions.Item label={t('jobDetail.rowCount')}>{row.rowCount ?? '—'}</Descriptions.Item>
            <Descriptions.Item label={t('jobDetail.queued')}>{formatDateTime(row.queuedAt)}</Descriptions.Item>
            <Descriptions.Item label={t('jobDetail.started')}>{formatDateTime(row.startedAt)}</Descriptions.Item>
            <Descriptions.Item label={t('jobDetail.finished')}>{formatDateTime(row.finishedAt)}</Descriptions.Item>
          </Descriptions>
          {distributedJob && (
            <>
              <Typography.Title level={5}>{t('jobDetail.distributed.title')}</Typography.Title>
              <Descriptions
                bordered
                size="small"
                column={2}
                style={{ marginBottom: 16 }}
                data-testid="job-detail-distributed"
              >
                <Descriptions.Item label={t('jobDetail.distributed.jobId')}>
                  {distributedJob.jobId}
                </Descriptions.Item>
                <Descriptions.Item label={t('jobDetail.distributed.status')}>
                  {enumLabel(t, 'status', distributedJob.status)}
                </Descriptions.Item>
                <Descriptions.Item label={t('jobDetail.distributed.worker')}>
                  {distributedJob.workerId ?? '—'}
                </Descriptions.Item>
                <Descriptions.Item label={t('jobDetail.distributed.attempts')}>
                  {distributedJob.attempts ?? '—'}
                </Descriptions.Item>
                <Descriptions.Item label={t('jobDetail.distributed.leaseUntil')}>
                  {distributedJob.leaseUntil ?? '—'}
                </Descriptions.Item>
              </Descriptions>
            </>
          )}
          {partitionMetrics && (
            <>
              <Typography.Title level={5}>{t('jobDetail.partition.title')}</Typography.Title>
              <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
                <Descriptions.Item label={t('jobDetail.partition.configured')}>
                  {partitionMetrics.configuredPartitions}
                </Descriptions.Item>
                <Descriptions.Item label={t('jobDetail.partition.executed')}>
                  {partitionMetrics.executedPartitions}
                </Descriptions.Item>
              </Descriptions>
            </>
          )}
          {report && (
            <RunReportSection
              report={report}
              stageColumns={stageColumns}
              sinkStageColumns={sinkStageColumns}
              aiCallColumns={aiCallColumns}
            />
          )}
          {body.length > 0 && (
            <>
              <Typography.Title level={5}>{t('jobDetail.metrics')}</Typography.Title>
              <pre className="job-metrics">{body}</pre>
            </>
          )}
        </>
      ) : (
        <Alert type="warning" showIcon message={t('jobDetail.notFound')} />
      )}
    </div>
  );
}

function RunReportSection({
  report,
  stageColumns,
  sinkStageColumns,
  aiCallColumns,
}: {
  report: RunReport;
  stageColumns: ColumnsType<StageMetric>;
  sinkStageColumns: ColumnsType<StageMetric>;
  aiCallColumns: ColumnsType<AiCallMetric>;
}) {
  const { t } = useTranslation();

  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message={t('jobDetail.report.hint.title')}
        description={t('jobDetail.report.hint.body')}
      />
      <Typography.Title level={5}>{t('jobDetail.report.title')}</Typography.Title>
      <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
        <Descriptions.Item label={t('jobDetail.report.executionMode')}>
          {enumLabel(t, 'execution.mode', report.executionMode ?? undefined)}
        </Descriptions.Item>
        <Descriptions.Item label={t('jobDetail.report.durationMs')}>
          {formatDuration(report.durationMs)}
        </Descriptions.Item>
      </Descriptions>
      <StageMetricTable
        title={t('jobDetail.report.sources')}
        rows={report.sources}
        columns={stageColumns}
      />
      <StageMetricTable
        title={t('jobDetail.report.transformers')}
        rows={report.transformers}
        columns={stageColumns}
      />
      <StageMetricTable
        title={t('jobDetail.report.sinks')}
        rows={report.sinks}
        columns={sinkStageColumns}
      />
      {report.aiCalls && report.aiCalls.length > 0 && (
        <>
          <Typography.Title level={5}>{t('jobDetail.report.aiCalls')}</Typography.Title>
          <Table
            size="small"
            pagination={false}
            rowKey={(row, index) => `${row.sourceName ?? 'ai'}-${index}`}
            dataSource={report.aiCalls}
            columns={aiCallColumns}
            style={{ marginBottom: 16 }}
          />
        </>
      )}
      {report.errorSamples.length > 0 && (
        <>
          <Typography.Title level={5}>{t('jobDetail.report.errorSamples')}</Typography.Title>
          {report.errorSamples.map((sample, index) => (
            <Alert
              key={`${index}-${sample.slice(0, 32)}`}
              type="warning"
              message={sample}
              style={{ marginBottom: 8 }}
            />
          ))}
        </>
      )}
    </>
  );
}

function StageMetricTable({
  title,
  rows,
  columns,
}: {
  title: string;
  rows: StageMetric[];
  columns: ColumnsType<StageMetric>;
}) {
  if (rows.length === 0) {
    return null;
  }

  return (
    <>
      <Typography.Title level={5}>{title}</Typography.Title>
      <Table<StageMetric>
        rowKey={(row, index) => `${row.name}-${index}`}
        size="small"
        pagination={false}
        dataSource={rows}
        columns={columns}
        style={{ marginBottom: 16 }}
      />
    </>
  );
}
