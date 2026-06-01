import { useMutation, useQuery } from '@tanstack/react-query';
import { Alert, Button, Descriptions, Space, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { cancelJob, fetchJob, resumeJob } from '../../api/jobs';
import type { RunReport, StageMetric } from '../../api/types';
import { JobStatusTag } from '../../components/JobStatusTag';

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
      const status = query.state.data?.status;
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

  const row = jobQuery.data;
  const report = row?.report ?? null;
  const body =
    row?.metricsJson && row.metricsJson.length > 0
      ? row.metricsJson
      : row?.errorMessage ?? '';

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => navigate('/jobs')}>{t('jobDetail.back')}</Button>
        {row?.templateId != null && (
          <Button onClick={() => navigate(`/templates/${row.templateId}`)}>
            {t('jobDetail.openTemplate')}
          </Button>
        )}
        <Button onClick={() => jobQuery.refetch()}>{t('common.refresh')}</Button>
        {jobQuery.data && ACTIVE.has(jobQuery.data.status) && (
          <Button
            danger
            loading={cancelMutation.isPending}
            onClick={() => cancelMutation.mutate()}
          >
            {t('jobDetail.cancel')}
          </Button>
        )}
        {jobQuery.data?.status === 'PAUSED' && (
          <Button
            type="primary"
            loading={resumeMutation.isPending}
            onClick={() => resumeMutation.mutate()}
          >
            {t('jobDetail.resume')}
          </Button>
        )}
      </Space>
      <Typography.Title level={3}>
        {t('jobDetail.title')} #{instanceIdParam}
      </Typography.Title>
      {row && (
        <>
          <Descriptions bordered size="small" column={1} style={{ marginBottom: 16 }}>
            <Descriptions.Item label={t('jobs.col.status')}>
              <JobStatusTag status={row.status} />
            </Descriptions.Item>
            <Descriptions.Item label={t('jobs.col.template')}>
              {row.templateName} (#{row.templateId})
            </Descriptions.Item>
            <Descriptions.Item label={t('jobs.col.kind')}>{row.definitionKind}</Descriptions.Item>
            <Descriptions.Item label={t('jobDetail.rowCount')}>{row.rowCount ?? '—'}</Descriptions.Item>
            <Descriptions.Item label={t('jobDetail.queued')}>{row.queuedAt ?? '—'}</Descriptions.Item>
            <Descriptions.Item label={t('jobDetail.started')}>{row.startedAt ?? '—'}</Descriptions.Item>
            <Descriptions.Item label={t('jobDetail.finished')}>{row.finishedAt ?? '—'}</Descriptions.Item>
          </Descriptions>
          {report && <RunReportSection report={report} stageColumns={stageColumns} />}
          {body.length > 0 && (
            <>
              <Typography.Title level={5}>{t('jobDetail.metrics')}</Typography.Title>
              <pre className="job-metrics">{body}</pre>
            </>
          )}
        </>
      )}
    </div>
  );
}

function RunReportSection({
  report,
  stageColumns,
}: {
  report: RunReport;
  stageColumns: ColumnsType<StageMetric>;
}) {
  const { t } = useTranslation();

  return (
    <>
      <Typography.Title level={5}>{t('jobDetail.report.title')}</Typography.Title>
      <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
        <Descriptions.Item label={t('jobDetail.report.executionMode')}>
          {report.executionMode ?? '—'}
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
        columns={stageColumns}
      />
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
