import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { createSchedule, deleteSchedule, fetchSchedules, updateSchedule } from '../../api/schedules';
import { fetchTemplates } from '../../api/templates';
import { fetchConsoleRuntime } from '../../api/runtime';
import type { TaskScheduleUpsertRequest, TaskScheduleView } from '../../api/types';
import { ConsolePageHeader } from '../../components/ConsolePageHeader';
import { FieldHelp } from '../../components/FieldHelp';
import { CronPresetButtons } from '../schedules/CronPresetButtons';
import { CronPreviewField } from '../schedules/CronPreviewField';
import { formatDateTime } from '../utils/formatDateTime';

type ScheduleFormValues = {
  templateId: string;
  cronExpression: string;
  enabled: boolean;
  description?: string;
};

function formatId(value: string | number): string {
  return String(value);
}

/**
 * Cron schedule administration for automated template runs.
 */
export function SchedulesPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const highlightScheduleId = searchParams.get('scheduleId')?.trim() ?? '';
  const [templateFilter, setTemplateFilter] = useState(searchParams.get('templateId') ?? '');
  const highlightHandledRef = useRef<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<TaskScheduleView | null>(null);
  const [dialogKey, setDialogKey] = useState('new');
  const [form] = Form.useForm<ScheduleFormValues>();

  const filterTemplateId = templateFilter.trim() || undefined;

  const schedulesQuery = useQuery({
    queryKey: ['schedules', filterTemplateId],
    queryFn: () => fetchSchedules(filterTemplateId),
    refetchInterval: 30_000,
  });

  const runtimeQuery = useQuery({
    queryKey: ['console-runtime'],
    queryFn: fetchConsoleRuntime,
  });

  const templatesQuery = useQuery({
    queryKey: ['templates', false, ''],
    queryFn: () => fetchTemplates(false),
  });

  const templateOptions = useMemo(() => {
    const rows = templatesQuery.data ?? [];
    return rows
      .filter((row) => row.status === 'PUBLISHED' && !row.archived)
      .map((row) => ({
        value: formatId(row.id),
        label: `${row.name} (${row.id})`,
      }));
  }, [templatesQuery.data]);

  const templateNameById = useMemo(() => {
    const map = new Map<string, string>();
    for (const row of templatesQuery.data ?? []) {
      map.set(formatId(row.id), row.name);
    }
    return map;
  }, [templatesQuery.data]);

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['schedules'] });

  const saveMutation = useMutation({
    mutationFn: async (values: ScheduleFormValues) => {
      const body: TaskScheduleUpsertRequest = {
        templateId: values.templateId,
        cronExpression: values.cronExpression.trim(),
        enabled: values.enabled,
        description: values.description?.trim() || null,
      };
      if (editing) {
        return updateSchedule(formatId(editing.id), body);
      }
      return createSchedule(body);
    },
    onSuccess: () => {
      message.success(t('schedules.dialog.saved'));
      setModalOpen(false);
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteSchedule,
    onSuccess: () => {
      message.success(t('schedules.removed'));
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const openCreate = () => {
    setEditing(null);
    setDialogKey(`new-${Date.now()}`);
    form.setFieldsValue({
      templateId: filterTemplateId ?? undefined,
      cronExpression: '0 0 2 * * *',
      enabled: true,
      description: '',
    });
    setModalOpen(true);
  };

  const syncSearchParams = (next: { templateId?: string; scheduleId?: string }) => {
    const params: Record<string, string> = {};
    const templateId = next.templateId?.trim();
    const scheduleId = next.scheduleId?.trim();
    if (templateId) {
      params.templateId = templateId;
    }
    if (scheduleId) {
      params.scheduleId = scheduleId;
    }
    setSearchParams(params);
  };

  const openEdit = (row: TaskScheduleView) => {
    setEditing(row);
    setDialogKey(`edit-${formatId(row.id)}`);
    form.setFieldsValue({
      templateId: formatId(row.templateId),
      cronExpression: row.cronExpression,
      enabled: row.enabled,
      description: row.description ?? '',
    });
    setModalOpen(true);
  };

  const confirmDelete = (row: TaskScheduleView) => {
    const id = formatId(row.id);
    Modal.confirm({
      title: t('schedules.remove.confirm.title', { id }),
      content: t('schedules.remove.confirm.text'),
      okText: t('common.remove'),
      cancelText: t('common.cancel'),
      okButtonProps: { danger: true },
      onOk: () => deleteMutation.mutateAsync(id),
    });
  };

  const highlightedRow = useMemo(() => {
    if (!highlightScheduleId) {
      return undefined;
    }
    return (schedulesQuery.data ?? []).find((row) => formatId(row.id) === highlightScheduleId);
  }, [highlightScheduleId, schedulesQuery.data]);

  useEffect(() => {
    highlightHandledRef.current = null;
  }, [highlightScheduleId]);

  useEffect(() => {
    if (!highlightScheduleId || schedulesQuery.isLoading) {
      return;
    }
    if (highlightHandledRef.current === highlightScheduleId) {
      return;
    }
    const match = highlightedRow;
    if (!match) {
      return;
    }
    highlightHandledRef.current = highlightScheduleId;
    const templateId = formatId(match.templateId);
    if (!templateFilter.trim()) {
      setTemplateFilter(templateId);
    }
    syncSearchParams({
      templateId: templateFilter.trim() || templateId,
      scheduleId: highlightScheduleId,
    });
  }, [
    highlightScheduleId,
    highlightedRow,
    schedulesQuery.isLoading,
    templateFilter,
  ]);

  const columns: ColumnsType<TaskScheduleView> = useMemo(
    () => [
      {
        title: t('schedules.col.id'),
        dataIndex: 'id',
        render: (id: string | number) => formatId(id),
      },
      {
        title: t('schedules.col.template'),
        key: 'template',
        render: (_, row) => {
          const tid = formatId(row.templateId);
          const name = templateNameById.get(tid) ?? tid;
          return <Link to={`/templates/${tid}`}>{name}</Link>;
        },
      },
      {
        title: t('schedules.col.cron'),
        dataIndex: 'cronExpression',
        render: (cron: string) => <Typography.Text code>{cron}</Typography.Text>,
      },
      {
        title: t('schedules.col.enabled'),
        dataIndex: 'enabled',
        render: (enabled: boolean) =>
          enabled ? (
            <Tag color="cyan">{t('common.yes')}</Tag>
          ) : (
            <Tag>{t('common.no')}</Tag>
          ),
      },
      {
        title: t('schedules.col.next'),
        dataIndex: 'nextTriggerAt',
        render: formatDateTime,
      },
      {
        title: t('schedules.col.last'),
        dataIndex: 'lastTriggeredAt',
        render: formatDateTime,
      },
      {
        title: t('schedules.col.lastInstance'),
        key: 'lastInstanceId',
        render: (_, row) =>
          row.lastInstanceId == null ? (
            '—'
          ) : (
            <Link to={`/jobs/${formatId(row.lastInstanceId)}`}>{formatId(row.lastInstanceId)}</Link>
          ),
      },
      {
        title: t('schedules.col.description'),
        dataIndex: 'description',
        ellipsis: true,
        render: (text: string | null) => text ?? '—',
      },
      {
        title: '',
        key: 'actions',
        render: (_, row) => (
          <Space>
            <Button type="link" onClick={() => openEdit(row)}>
              {t('common.edit')}
            </Button>
            <Button
              type="link"
              onClick={() =>
                navigate(
                  `/jobs?templateId=${encodeURIComponent(formatId(row.templateId))}&triggerType=SCHEDULED`,
                )
              }
            >
              {t('schedules.viewJobs')}
            </Button>
            <Button type="link" danger onClick={() => confirmDelete(row)}>
              {t('common.remove')}
            </Button>
          </Space>
        ),
      },
    ],
    [t, templateNameById, navigate],
  );

  return (
    <section className="console-page-panel" data-testid="schedules-page">
      <ConsolePageHeader
        title={t('schedules.title')}
        subtitle={t('schedules.subtitle')}
        crumbs={[{ label: t('nav.home'), path: '/' }, { label: t('nav.schedules') }]}
        extra={
          <Button type="primary" data-testid="schedules-new-button" onClick={openCreate}>
            {t('schedules.new')}
          </Button>
        }
      />

      {runtimeQuery.data && !runtimeQuery.data.scheduleEnabled && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message={t('schedules.poller.off.title')}
          description={t('schedules.poller.off.body')}
        />
      )}

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={t('schedules.hint.title')}
        description={t('schedules.hint.body')}
      />

      {highlightScheduleId && (
        <Alert
          type={highlightedRow ? 'success' : 'warning'}
          showIcon
          closable
          style={{ marginBottom: 16 }}
          message={
            highlightedRow
              ? t('schedules.highlight.found', { id: highlightScheduleId })
              : t('schedules.highlight.missing', { id: highlightScheduleId })
          }
          onClose={() => syncSearchParams({ templateId: templateFilter })}
        />
      )}

      <Space style={{ marginBottom: 16 }} wrap>
        <Input
          placeholder={t('schedules.filter.templateId')}
          value={templateFilter}
          onChange={(e) => {
            const next = e.target.value;
            setTemplateFilter(next);
            syncSearchParams({
              templateId: next,
              scheduleId: highlightScheduleId,
            });
          }}
          style={{ width: 220 }}
          allowClear
        />
        <Button onClick={() => schedulesQuery.refetch()}>{t('common.refresh')}</Button>
      </Space>

      <Table
        rowKey={(row) => formatId(row.id)}
        loading={schedulesQuery.isLoading}
        dataSource={schedulesQuery.data ?? []}
        columns={columns}
        rowClassName={(row) =>
          highlightScheduleId && formatId(row.id) === highlightScheduleId
            ? 'schedules-row-highlight'
            : ''
        }
        pagination={{ pageSize: 20 }}
        locale={{ emptyText: t('schedules.empty') }}
      />

      <Modal
        key={dialogKey}
        title={editing ? t('schedules.dialog.edit') : t('schedules.dialog.create')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={saveMutation.isPending}
        destroyOnClose
        width={560}
      >
        <Form form={form} layout="vertical" onFinish={(v) => saveMutation.mutate(v)}>
          <Form.Item
            name="templateId"
            label={
              <FieldHelp label={t('schedules.form.templateId')} help={t('schedules.form.templateId.help')} />
            }
            rules={[{ required: true, message: t('schedules.form.templateIdRequired') }]}
          >
            <Select
              showSearch
              optionFilterProp="label"
              options={templateOptions}
              placeholder={t('schedules.form.templateIdPlaceholder')}
              loading={templatesQuery.isLoading}
            />
          </Form.Item>
          <Form.Item
            name="cronExpression"
            label={<FieldHelp label={t('schedules.form.cron')} help={t('schedules.form.cron.help')} />}
            rules={[{ required: true, message: t('schedules.form.cronRequired') }]}
          >
            <Input placeholder="0 0 2 * * *" />
          </Form.Item>
          <CronPresetButtons form={form} />
          <CronPreviewField form={form} />
          <Form.Item
            name="enabled"
            label={<FieldHelp label={t('schedules.form.enabled')} help={t('schedules.form.enabled.help')} />}
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
          <Form.Item
            name="description"
            label={<FieldHelp label={t('schedules.form.description')} help={t('schedules.form.description.help')} />}
          >
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </section>
  );
}
