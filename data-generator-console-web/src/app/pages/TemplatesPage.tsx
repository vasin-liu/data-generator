import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Checkbox, Input, Modal, Select, Space, Table, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  archiveTemplate,
  fetchTemplateTaxonomy,
  fetchTemplates,
  restoreTemplate,
  runTemplate,
} from '../../api/templates';
import type { TemplateSummary } from '../../api/types';
import { ConsolePageHeader } from '../../components/ConsolePageHeader';
import { ScenarioCatalogModal } from '../components/ScenarioCatalogModal';
import { TemplateStatusTag } from '../../components/TemplateStatusTag';

/**
 * Template catalog grid (parity with Vaadin {@code TemplateListView}).
 */
export function TemplatesPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [includeArchived, setIncludeArchived] = useState(false);
  const [filter, setFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState<string | undefined>();
  const [categoryFilter, setCategoryFilter] = useState<string | undefined>();
  const [tagFilter, setTagFilter] = useState<string | undefined>();
  const [scenarioModalOpen, setScenarioModalOpen] = useState(false);

  const taxonomyQuery = useQuery({
    queryKey: ['template-taxonomy'],
    queryFn: fetchTemplateTaxonomy,
  });

  const listQuery = useQuery({
    queryKey: ['templates', includeArchived, filter, categoryFilter, tagFilter],
    queryFn: () => fetchTemplates(includeArchived, filter, categoryFilter, tagFilter),
  });

  const filteredRows = useMemo(() => {
    const rows = listQuery.data ?? [];
    if (!statusFilter) {
      return rows;
    }
    return rows.filter((row) => row.status === statusFilter);
  }, [listQuery.data, statusFilter]);

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['templates'] });

  const runMutation = useMutation({
    mutationFn: runTemplate,
    onSuccess: (data) => {
      message.success(t('common.run'));
      navigate(`/jobs/${data.instanceId}`);
    },
    onError: (err: Error) => message.error(err.message),
  });

  const archiveMutation = useMutation({
    mutationFn: archiveTemplate,
    onSuccess: () => {
      message.success(t('common.archive'));
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const restoreMutation = useMutation({
    mutationFn: restoreTemplate,
    onSuccess: () => {
      message.success(t('common.restore'));
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const columns: ColumnsType<TemplateSummary> = useMemo(
    () => [
      { title: t('templates.col.id'), dataIndex: 'id', sorter: (a, b) => a.id.localeCompare(b.id) },
      { title: t('templates.col.name'), dataIndex: 'name', sorter: (a, b) => a.name.localeCompare(b.name) },
      { title: t('templates.col.category'), dataIndex: 'category', render: (v: string | null) => v ?? '—' },
      {
        title: t('templates.col.tags'),
        dataIndex: 'tags',
        render: (tags: string[] | undefined) => (tags?.length ? tags.join(', ') : '—'),
      },
      {
        title: t('templates.col.status'),
        dataIndex: 'status',
        render: (status: string | null) =>
          status ? <TemplateStatusTag status={status} /> : '—',
      },
      {
        title: t('templates.col.archived'),
        dataIndex: 'archived',
        render: (v: boolean | null) => (v ? t('common.yes') : t('common.no')),
      },
      {
        title: '',
        key: 'actions',
        render: (_, row) => {
          const active = !row.archived;
          return (
            <Space wrap>
              <Button type="link" onClick={() => navigate(`/templates/${row.id}`)}>
                {t('common.edit')}
              </Button>
              <Button
                type="link"
                disabled={!active}
                onClick={() => navigate(`/jobs?templateId=${encodeURIComponent(String(row.id))}`)}
              >
                {t('templates.viewJobs')}
              </Button>
              <Button
                type="link"
                disabled={!active}
                onClick={() => navigate(`/schedules?templateId=${encodeURIComponent(String(row.id))}`)}
              >
                {t('templates.schedules')}
              </Button>
              <Button
                type="link"
                disabled={!active}
                onClick={() =>
                  Modal.confirm({
                    title: t('common.run'),
                    content: t('templates.run.confirm'),
                    onOk: () => runMutation.mutateAsync(row.id),
                  })
                }
              >
                {t('common.run')}
              </Button>
              <Button
                type="link"
                onClick={() => {
                  const archived = Boolean(row.archived);
                  Modal.confirm({
                    title: archived ? t('common.restore') : t('common.archive'),
                    content: archived
                      ? t('templates.restore.confirm', { name: row.name })
                      : t('templates.archive.confirm', { name: row.name }),
                    onOk: () =>
                      archived
                        ? restoreMutation.mutateAsync(row.id)
                        : archiveMutation.mutateAsync(row.id),
                  });
                }}
              >
                {row.archived ? t('common.restore') : t('common.archive')}
              </Button>
            </Space>
          );
        },
      },
    ],
    [t, navigate, runMutation, archiveMutation, restoreMutation],
  );

  return (
    <div data-testid="templates-page">
      <ConsolePageHeader
        title={t('templates.title')}
        subtitle={t('templates.subtitle')}
        crumbs={[{ label: t('nav.home'), path: '/' }, { label: t('nav.templates') }]}
        extra={
          <Space>
            <Button data-testid="templates-from-scenario-button" onClick={() => setScenarioModalOpen(true)}>
              {t('templates.fromScenario')}
            </Button>
            <Button type="primary" data-testid="templates-new-button" onClick={() => navigate('/templates/new')}>
              {t('templates.new')}
            </Button>
          </Space>
        }
      />
      <ScenarioCatalogModal
        open={scenarioModalOpen}
        onClose={() => setScenarioModalOpen(false)}
        onSelect={(scenarioId: string) => {
          setScenarioModalOpen(false);
          navigate(`/templates/new?scenario=${encodeURIComponent(scenarioId)}`);
        }}
      />
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={t('templates.hint.title')}
        description={t('templates.hint.body')}
      />
      {listQuery.isError ? (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          message={t('templates.loadError')}
          description={(listQuery.error as Error).message}
        />
      ) : null}
      <Space wrap style={{ marginBottom: 16 }}>
        <Button onClick={() => listQuery.refetch()}>{t('common.refresh')}</Button>
        <Input
          allowClear
          placeholder={t('templates.filter.placeholder')}
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          style={{ width: 200 }}
        />
        <Select
          allowClear
          style={{ width: 140 }}
          placeholder={t('templates.filter.status')}
          value={statusFilter}
          onChange={setStatusFilter}
          options={[
            { value: 'DRAFT', label: t('template.status.DRAFT') },
            { value: 'PUBLISHED', label: t('template.status.PUBLISHED') },
          ]}
        />
        <Select
          allowClear
          showSearch
          style={{ width: 160 }}
          placeholder={t('templates.filter.category')}
          value={categoryFilter}
          onChange={setCategoryFilter}
          options={(taxonomyQuery.data?.categories ?? []).map((c) => ({ value: c, label: c }))}
        />
        <Select
          allowClear
          showSearch
          style={{ width: 160 }}
          placeholder={t('templates.filter.tag')}
          value={tagFilter}
          onChange={setTagFilter}
          options={(taxonomyQuery.data?.tags ?? []).map((tag) => ({ value: tag, label: tag }))}
        />
        <Checkbox checked={includeArchived} onChange={(e) => setIncludeArchived(e.target.checked)}>
          {t('templates.includeArchived')}
        </Checkbox>
      </Space>
      <Table<TemplateSummary>
        rowKey={(row) => String(row.id)}
        loading={listQuery.isLoading}
        dataSource={filteredRows}
        columns={columns}
        pagination={{ pageSize: 20 }}
        locale={{
          emptyText: listQuery.isLoading ? ' ' : t('templates.empty'),
        }}
      />
    </div>
  );
}
