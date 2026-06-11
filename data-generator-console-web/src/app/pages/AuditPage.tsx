import { useQuery } from '@tanstack/react-query';
import { Alert, Input, Select, Space, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { fetchAuditEvents } from '../../api/audit';
import type { AuditEventView } from '../../api/types';
import { ConsolePageHeader } from '../../components/ConsolePageHeader';
import { formatDateTime } from '../utils/formatDateTime';

const ACTION_OPTIONS = [
  'TEMPLATE_PUBLISH',
  'TEMPLATE_SAVE',
  'DATASOURCE_CREATE',
  'DATASOURCE_UPDATE',
  'JOB_RUN',
  'SCHEDULE_TRIGGER',
] as const;

/**
 * Searchable operator audit log (publish, datasource, and run events).
 */
export function AuditPage() {
  const { t } = useTranslation();
  const [actionFilter, setActionFilter] = useState<string | undefined>();
  const [resourceTypeFilter, setResourceTypeFilter] = useState<string | undefined>();

  const auditQuery = useQuery({
    queryKey: ['audit', actionFilter, resourceTypeFilter],
    queryFn: () => fetchAuditEvents(actionFilter, resourceTypeFilter),
    refetchInterval: 30_000,
  });

  const columns: ColumnsType<AuditEventView> = useMemo(
    () => [
      {
        title: t('audit.col.time'),
        dataIndex: 'occurredAt',
        render: (value: string) => formatDateTime(value),
        width: 180,
      },
      { title: t('audit.col.actor'), dataIndex: 'actor', width: 120 },
      { title: t('audit.col.action'), dataIndex: 'action', width: 180 },
      { title: t('audit.col.resourceType'), dataIndex: 'resourceType', width: 120 },
      { title: t('audit.col.resourceId'), dataIndex: 'resourceId', width: 120 },
      {
        title: t('audit.col.detail'),
        dataIndex: 'detail',
        render: (detail: Record<string, unknown>) => (
          <Typography.Text code style={{ fontSize: 12 }}>
            {Object.keys(detail ?? {}).length > 0 ? JSON.stringify(detail) : '—'}
          </Typography.Text>
        ),
      },
    ],
    [t],
  );

  return (
    <div data-testid="audit-page">
      <ConsolePageHeader
        title={t('audit.title')}
        crumbs={[{ label: t('nav.home'), path: '/' }, { label: t('nav.audit') }]}
      />
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16, maxWidth: 900 }}
        message={t('audit.intro.title')}
        description={t('audit.intro.body')}
      />
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          allowClear
          placeholder={t('audit.filter.action')}
          style={{ minWidth: 220 }}
          value={actionFilter}
          options={ACTION_OPTIONS.map((action) => ({ value: action, label: action }))}
          onChange={(value) => setActionFilter(value)}
        />
        <Input
          allowClear
          placeholder={t('audit.filter.resourceType')}
          style={{ width: 180 }}
          value={resourceTypeFilter}
          onChange={(e) => setResourceTypeFilter(e.target.value.trim() || undefined)}
        />
      </Space>
      <Table
        rowKey={(row) => String(row.id)}
        loading={auditQuery.isLoading}
        dataSource={auditQuery.data ?? []}
        columns={columns}
        pagination={{ pageSize: 20, showSizeChanger: true }}
        locale={{ emptyText: t('audit.empty') }}
      />
    </div>
  );
}
