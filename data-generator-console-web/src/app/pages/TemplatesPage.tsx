import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Checkbox, Input, Modal, Space, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  archiveTemplate,
  fetchTemplates,
  restoreTemplate,
  runTemplate,
} from '../../api/templates';
import type { TemplateSummary } from '../../api/types';

/**
 * Template catalog grid (parity with Vaadin {@code TemplateListView}).
 */
export function TemplatesPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [includeArchived, setIncludeArchived] = useState(false);
  const [filter, setFilter] = useState('');

  const listQuery = useQuery({
    queryKey: ['templates', includeArchived, filter],
    queryFn: () => fetchTemplates(includeArchived, filter),
  });

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
      { title: t('templates.col.id'), dataIndex: 'id', sorter: (a, b) => a.id - b.id },
      { title: t('templates.col.name'), dataIndex: 'name', sorter: (a, b) => a.name.localeCompare(b.name) },
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
    <div>
      <Typography.Title level={3}>{t('templates.title')}</Typography.Title>
      <Typography.Paragraph type="secondary">{t('templates.subtitle')}</Typography.Paragraph>
      <Space wrap style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={() => navigate('/templates/new')}>
          {t('templates.new')}
        </Button>
        <Button onClick={() => listQuery.refetch()}>{t('common.refresh')}</Button>
        <Input
          allowClear
          placeholder={t('templates.filter.placeholder')}
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          style={{ width: 200 }}
        />
        <Checkbox checked={includeArchived} onChange={(e) => setIncludeArchived(e.target.checked)}>
          {t('templates.includeArchived')}
        </Checkbox>
      </Space>
      <Table<TemplateSummary>
        rowKey="id"
        loading={listQuery.isLoading}
        dataSource={listQuery.data ?? []}
        columns={columns}
        pagination={{ pageSize: 20 }}
      />
    </div>
  );
}
