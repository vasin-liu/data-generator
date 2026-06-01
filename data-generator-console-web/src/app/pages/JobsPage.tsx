import { useQuery } from '@tanstack/react-query';
import { Button, Descriptions, Input, Space, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router-dom';
import { fetchDistributedMetrics } from '../../api/distributed';
import { fetchJobs } from '../../api/jobs';
import type { TaskExecutionSummary } from '../../api/types';
import { JobStatusTag } from '../../components/JobStatusTag';

const ACTIVE = new Set(['QUEUED', 'RUNNING']);

function parseTemplateId(raw: string): string | undefined {
  const trimmed = raw.trim();
  return trimmed || undefined;
}

/**
 * Job history grid with 2s polling while runs are active.
 */
export function JobsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [templateIdInput, setTemplateIdInput] = useState('');
  const templateId = parseTemplateId(templateIdInput);

  const jobsQuery = useQuery({
    queryKey: ['jobs', templateId],
    queryFn: () => fetchJobs(templateId),
    refetchInterval: (query) => {
      const rows = query.state.data;
      const active = rows?.some((r) => ACTIVE.has(r.status)) ?? false;
      return active ? 2000 : false;
    },
  });

  const distributedQuery = useQuery({
    queryKey: ['distributed-metrics'],
    queryFn: fetchDistributedMetrics,
    refetchInterval: 5000,
  });

  const pollHint =
    jobsQuery.data?.some((r) => ACTIVE.has(r.status)) ?? false
      ? t('jobs.poll.active')
      : t('jobs.poll.idle');

  const columns: ColumnsType<TaskExecutionSummary> = useMemo(
    () => [
      {
        title: t('jobs.col.instance'),
        dataIndex: 'instanceId',
        sorter: (a, b) => a.instanceId.localeCompare(b.instanceId),
      },
      {
        title: t('jobs.col.template'),
        key: 'template',
        render: (_, row) =>
          row.templateId != null ? (
            <Link to={`/templates/${row.templateId}`}>{row.templateName ?? row.templateId}</Link>
          ) : (
            row.templateName ?? '—'
          ),
      },
      { title: t('jobs.col.kind'), dataIndex: 'definitionKind' },
      {
        title: t('jobs.col.status'),
        dataIndex: 'status',
        render: (status: string) => <JobStatusTag status={status} />,
      },
      { title: t('jobs.col.finished'), dataIndex: 'finishedAt' },
      {
        title: '',
        key: 'detail',
        render: (_, row) => (
          <Button type="link" onClick={() => navigate(`/jobs/${row.instanceId}`)}>
            {t('common.detail')}
          </Button>
        ),
      },
    ],
    [t, navigate],
  );

  return (
    <div>
      <Typography.Title level={3}>{t('jobs.title')}</Typography.Title>
      <Typography.Paragraph type="secondary">{t('jobs.subtitle')}</Typography.Paragraph>
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          allowClear
          placeholder={t('jobs.filter.templateId')}
          value={templateIdInput}
          onChange={(e) => setTemplateIdInput(e.target.value)}
          style={{ width: 160 }}
        />
        <Button onClick={() => jobsQuery.refetch()}>{t('common.refresh')}</Button>
        <Typography.Text type="secondary">{pollHint}</Typography.Text>
      </Space>
      {distributedQuery.data?.distributedEnabled && (
        <>
          <Typography.Title level={5}>{t('jobs.distributed.title')}</Typography.Title>
          <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
            <Descriptions.Item label={t('jobs.distributed.workerEnabled')}>
              {distributedQuery.data.workerEnabled ? t('common.yes') : t('common.no')}
            </Descriptions.Item>
            <Descriptions.Item label={t('jobs.distributed.coordinatorPoll')}>
              {distributedQuery.data.coordinatorPollEnabled ? t('common.yes') : t('common.no')}
            </Descriptions.Item>
            <Descriptions.Item label={t('jobs.distributed.queued')} span={2}>
              {distributedQuery.data.jobsByStatus.QUEUED ?? 0}
            </Descriptions.Item>
            <Descriptions.Item label={t('jobs.distributed.leased')}>
              {distributedQuery.data.jobsByStatus.LEASED ?? 0}
            </Descriptions.Item>
            <Descriptions.Item label={t('jobs.distributed.running')}>
              {distributedQuery.data.jobsByStatus.RUNNING ?? 0}
            </Descriptions.Item>
            <Descriptions.Item label={t('jobs.distributed.activeWorkers')} span={2}>
              {distributedQuery.data.activeWorkers.length === 0
                ? '—'
                : distributedQuery.data.activeWorkers
                    .map((w) => `${w.workerId} (${w.activeJobs})`)
                    .join(', ')}
            </Descriptions.Item>
          </Descriptions>
        </>
      )}
      <Table<TaskExecutionSummary>
        rowKey="instanceId"
        loading={jobsQuery.isLoading}
        dataSource={jobsQuery.data ?? []}
        columns={columns}
        pagination={{ pageSize: 20 }}
      />
    </div>
  );
}
