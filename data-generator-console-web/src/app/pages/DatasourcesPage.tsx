import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Form, Input, Modal, Space, Table, Typography, Upload, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  fetchDataSources,
  removeDataSource,
  testDataSourceByName,
  testDataSourceConnection,
  upsertDataSource,
} from '../../api/datasources';
import type { DataSourceSummary } from '../../api/types';
import { DriverPresetFields } from '../datasources/DriverPresetFields';
import { isBundledPreset, resolveDriverPresets } from '../datasources/jdbcDriverPresets';
import { ConsolePageHeader } from '../../components/ConsolePageHeader';

type FormValues = {
  name: string;
  url: string;
  username: string;
  password: string;
  driverClassName: string;
};

/**
 * JDBC datasource administration (persisted configs + runtime keys).
 */
export function DatasourcesPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<DataSourceSummary | null>(null);
  const [jarFile, setJarFile] = useState<File | null>(null);
  const [dialogKey, setDialogKey] = useState('new');
  const [selectedPresetId, setSelectedPresetId] = useState<string | undefined>();
  const [form] = Form.useForm<FormValues>();

  const overviewQuery = useQuery({
    queryKey: ['datasources'],
    queryFn: fetchDataSources,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['datasources'] });

  const saveMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const body = new FormData();
      body.set('name', values.name);
      body.set('url', values.url);
      body.set('username', values.username);
      body.set('password', values.password);
      body.set('driverClassName', values.driverClassName);
      if (jarFile) {
        body.set('driverFile', jarFile);
      }
      return upsertDataSource(body);
    },
    onSuccess: () => {
      message.success(t('datasources.dialog.saved', { name: form.getFieldValue('name') }));
      setModalOpen(false);
      setJarFile(null);
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const removeMutation = useMutation({
    mutationFn: removeDataSource,
    onSuccess: (_, name) => {
      message.success(t('datasources.removed', { name }));
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const testFormMutation = useMutation({
    mutationFn: (values: FormValues) =>
      testDataSourceConnection({
        url: values.url,
        username: values.username,
        password: values.password,
        driverClassName: values.driverClassName,
        driverJarPath: editing?.driverJarPath,
      }),
    onSuccess: (msg) => message.success(msg),
    onError: (err: Error) => message.error(err.message),
  });

  const openCreate = () => {
    setEditing(null);
    setJarFile(null);
    setDialogKey(`new-${Date.now()}`);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = (row: DataSourceSummary) => {
    setEditing(row);
    setJarFile(null);
    setDialogKey(`edit-${row.name}`);
    form.setFieldsValue({
      name: row.name,
      url: row.url,
      username: row.username ?? '',
      password: '',
      driverClassName: row.driverClassName,
    });
    setModalOpen(true);
  };

  const confirmRemove = (name: string) => {
    Modal.confirm({
      title: t('datasources.remove.confirm.title', { name }),
      content: t('datasources.remove.confirm.text'),
      okText: t('common.remove'),
      cancelText: t('common.cancel'),
      okButtonProps: { danger: true },
      onOk: () => removeMutation.mutateAsync(name),
    });
  };

  const driverPresets = useMemo(
    () => resolveDriverPresets(overviewQuery.data?.driverPresets),
    [overviewQuery.data?.driverPresets],
  );

  const columns: ColumnsType<DataSourceSummary> = useMemo(
    () => [
      { title: t('datasources.col.name'), dataIndex: 'name', sorter: (a, b) => a.name.localeCompare(b.name) },
      { title: t('datasources.col.url'), dataIndex: 'url', ellipsis: true },
      { title: t('datasources.col.driver'), dataIndex: 'driverClassName' },
      {
        title: t('datasources.col.enabled'),
        dataIndex: 'enabled',
        render: (v: boolean) => (v ? t('common.yes') : t('common.no')),
      },
      {
        title: t('datasources.col.actions'),
        key: 'actions',
        render: (_, row) => (
          <Space wrap>
            <Button type="link" onClick={() => openEdit(row)}>
              {t('common.edit')}
            </Button>
            <Button
              type="link"
              onClick={() =>
                testDataSourceByName(row.name)
                  .then((msg) => message.success(msg))
                  .catch((err: Error) => message.error(err.message))
              }
            >
              {t('common.test')}
            </Button>
            <Button type="link" danger onClick={() => confirmRemove(row.name)}>
              {t('common.remove')}
            </Button>
          </Space>
        ),
      },
    ],
    [t],
  );

  const uploadFileList: UploadFile[] = jarFile
    ? [{ uid: '-1', name: jarFile.name, status: 'done' }]
    : [];

  return (
    <div>
      <ConsolePageHeader
        title={t('datasources.title')}
        subtitle={t('datasources.subtitle')}
        crumbs={[{ label: t('nav.home'), path: '/' }, { label: t('nav.datasources') }]}
        extra={
          <Button type="primary" onClick={openCreate}>
            {t('datasources.new')}
          </Button>
        }
      />
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={t('datasources.workflow.title')}
        description={t('datasources.workflow.body')}
      />
      {overviewQuery.isError ? (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          message={t('datasources.loadError')}
          description={(overviewQuery.error as Error).message}
        />
      ) : null}
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => overviewQuery.refetch()}>{t('common.refresh')}</Button>
      </Space>

      <Typography.Title level={5}>{t('datasources.section.persisted')}</Typography.Title>
      <Table
        rowKey="name"
        loading={overviewQuery.isLoading}
        dataSource={overviewQuery.data?.persisted ?? []}
        columns={columns}
        pagination={false}
        style={{ marginBottom: 24 }}
      />

      <Typography.Title level={5}>{t('datasources.section.runtime')}</Typography.Title>
      <Typography.Paragraph>
        {(overviewQuery.data?.runtimeKeys ?? []).join(', ') || '—'}
      </Typography.Paragraph>

      <Modal
        title={t('datasources.dialog.title')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={(v) => saveMutation.mutate(v)}>
          <Form.Item name="name" label={t('datasources.dialog.name')} rules={[{ required: true }]}>
            <Input readOnly={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="url" label={t('datasources.dialog.url')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="username" label={t('datasources.dialog.username')}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label={t('datasources.dialog.password')}>
            <Input.Password placeholder={editing ? '••••••' : undefined} />
          </Form.Item>
          <DriverPresetFields
            form={form}
            dialogKey={dialogKey}
            presets={driverPresets}
            onPresetIdChange={setSelectedPresetId}
          />
          {!isBundledPreset(driverPresets, selectedPresetId) ? (
            <Form.Item label={t('datasources.dialog.upload')} extra={t('datasources.dialog.uploadHint')}>
              <Upload
                accept=".jar,application/java-archive"
                maxCount={1}
                beforeUpload={(file) => {
                  setJarFile(file);
                  return false;
                }}
                onRemove={() => setJarFile(null)}
                fileList={uploadFileList}
              >
                <Button>{t('datasources.dialog.upload')}</Button>
              </Upload>
            </Form.Item>
          ) : null}
          <Space>
            <Button onClick={() => testFormMutation.mutate(form.getFieldsValue())} loading={testFormMutation.isPending}>
              {t('datasources.dialog.test')}
            </Button>
            <Button type="primary" htmlType="submit" loading={saveMutation.isPending}>
              {t('common.save')}
            </Button>
            <Button onClick={() => setModalOpen(false)}>{t('common.cancel')}</Button>
          </Space>
        </Form>
      </Modal>
    </div>
  );
}
