import { useQuery } from '@tanstack/react-query';
import { Button, Card, Col, Row, Select, Space, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { fetchMigrationBacklog, fetchMigrationSummary } from '../../api/migration';
import type { MigrationInventoryEntry } from '../../api/types';

const FILTERS = [
  'ALL',
  'READY',
  'BLOCKED',
  'COMPATIBILITY_ONLY',
  'NEEDS_COMPARE',
  'PENDING_SIGNOFF',
] as const;

/**
 * Global migration inventory summary and backlog grid.
 */
export function MigrationPage() {
  const { t } = useTranslation();
  const [filter, setFilter] = useState<string>('ALL');

  const summaryQuery = useQuery({
    queryKey: ['migration-summary'],
    queryFn: fetchMigrationSummary,
  });

  const backlogQuery = useQuery({
    queryKey: ['migration-backlog', filter],
    queryFn: () => fetchMigrationBacklog(filter === 'ALL' ? undefined : filter),
  });

  const refresh = () => {
    summaryQuery.refetch();
    backlogQuery.refetch();
  };

  const summary = summaryQuery.data;
  const stats = [
    { label: t('migration.stat.total'), value: summary?.totalTemplates ?? '—' },
    { label: t('migration.stat.db'), value: summary?.databaseTemplates ?? '—' },
    { label: t('migration.stat.ready'), value: summary?.readyToPromote ?? '—' },
    { label: t('migration.stat.compat'), value: summary?.compatibilityOnly ?? '—' },
    { label: t('migration.stat.blocked'), value: summary?.blocked ?? '—' },
    { label: t('migration.stat.compare'), value: summary?.withCompareReport ?? '—' },
  ];

  const columns: ColumnsType<MigrationInventoryEntry> = useMemo(
    () => [
      { title: t('migration.col.id'), dataIndex: 'id', sorter: (a, b) => a.id.localeCompare(b.id) },
      { title: t('migration.col.name'), dataIndex: 'name' },
      { title: t('migration.col.class'), dataIndex: 'migrationClass' },
      { title: t('migration.col.family'), dataIndex: 'scenarioFamily' },
      { title: t('migration.col.wave'), dataIndex: 'wave' },
      {
        title: t('migration.col.signoff'),
        dataIndex: 'businessSignoffApproved',
        render: (v: boolean) => (v ? t('common.yes') : t('common.no')),
      },
      {
        title: t('migration.col.report'),
        dataIndex: 'lastCompareReportPath',
        ellipsis: true,
      },
      {
        title: t('migration.col.actions'),
        key: 'actions',
        render: (_, row) =>
          row.dbTemplateId != null ? (
            <Link to={`/templates/${row.dbTemplateId}?tab=migration`}>{t('migration.openEditor')}</Link>
          ) : (
            '—'
          ),
      },
    ],
    [t],
  );

  return (
    <div>
      <Typography.Title level={3}>{t('migration.title')}</Typography.Title>
      <Typography.Paragraph type="secondary">{t('migration.subtitle')}</Typography.Paragraph>
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          style={{ minWidth: 220 }}
          value={filter}
          options={FILTERS.map((f) => ({ value: f, label: t(`migration.filter.${f}`) }))}
          onChange={setFilter}
        />
        <Button onClick={refresh}>{t('common.refresh')}</Button>
        <Link to="/templates">
          <Button>{t('migration.link.templates')}</Button>
        </Link>
      </Space>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {stats.map((s) => (
          <Col key={s.label} xs={12} sm={8} md={4}>
            <Card size="small" loading={summaryQuery.isLoading}>
              <Typography.Text type="secondary">{s.label}</Typography.Text>
              <Typography.Title level={4} style={{ margin: 0 }}>
                {s.value}
              </Typography.Title>
            </Card>
          </Col>
        ))}
      </Row>

      <Table
        rowKey="id"
        loading={backlogQuery.isLoading}
        dataSource={backlogQuery.data ?? []}
        columns={columns}
        scroll={{ x: true }}
      />
    </div>
  );
}
