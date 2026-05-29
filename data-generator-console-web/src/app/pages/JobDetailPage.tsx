import { useQuery } from '@tanstack/react-query';
import { Button, Descriptions, Space, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchJob } from '../../api/jobs';
import { JobStatusTag } from '../../components/JobStatusTag';

const ACTIVE = new Set(['QUEUED', 'RUNNING']);

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

  const row = jobQuery.data;
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
            <Descriptions.Item label={t('jobDetail.queued')}>{row.queuedAt ?? '—'}</Descriptions.Item>
            <Descriptions.Item label={t('jobDetail.started')}>{row.startedAt ?? '—'}</Descriptions.Item>
            <Descriptions.Item label={t('jobDetail.finished')}>{row.finishedAt ?? '—'}</Descriptions.Item>
          </Descriptions>
          <Typography.Title level={5}>{t('jobDetail.metrics')}</Typography.Title>
          <pre className="job-metrics">{body}</pre>
        </>
      )}
    </div>
  );
}
