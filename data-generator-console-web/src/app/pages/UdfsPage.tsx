import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Button,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { deprecateUdf, fetchUdfs, publishUdf, uploadUdf } from '../../api/udfs';
import type { UdfGroupView, UdfVersionView } from '../../api/types';
import { ConsolePageHeader } from '../../components/ConsolePageHeader';
import { formatDateTime } from '../utils/formatDateTime';

type UploadFormValues = {
  udfId: string;
  version: string;
  type: string;
  scriptBody?: string;
  sql?: string;
  sqlName?: string;
  argCount?: string;
  returnType?: string;
  inputSchema?: string;
  outputSchema?: string;
};

const STATE_TAG_COLOR: Record<UdfVersionView['state'], string> = {
  draft: 'default',
  published: 'green',
  deprecated: 'red',
};

/**
 * Operator console UDF registry: list grouped by udfId with version history, lifecycle tags,
 * inline publish/deprecate, and a type-driven upload form.
 */
export function UdfsPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [typeFilter, setTypeFilter] = useState<string>('');
  const [modalOpen, setModalOpen] = useState(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [form] = Form.useForm<UploadFormValues>();
  const selectedType = Form.useWatch('type', form);

  const filterType = typeFilter || undefined;

  const udfsQuery = useQuery({
    queryKey: ['udfs', filterType],
    queryFn: () => fetchUdfs(filterType),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['udfs'] });

  const publishMutation = useMutation({
    mutationFn: ({ udfId, version }: { udfId: string; version: string }) => publishUdf(udfId, version),
    onSuccess: () => {
      message.success(t('udfs.published'));
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const deprecateMutation = useMutation({
    mutationFn: ({ udfId, version }: { udfId: string; version: string }) => deprecateUdf(udfId, version),
    onSuccess: () => {
      message.success(t('udfs.deprecated'));
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const uploadMutation = useMutation({
    mutationFn: (formData: FormData) => uploadUdf(formData),
    onSuccess: () => {
      message.success(t('udfs.uploaded'));
      setModalOpen(false);
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const openUpload = () => {
    form.resetFields();
    form.setFieldsValue({ type: 'sql' });
    setFileList([]);
    setModalOpen(true);
  };

  const submitUpload = (values: UploadFormValues) => {
    const formData = new FormData();
    formData.append('udfId', values.udfId.trim());
    formData.append('version', values.version.trim());
    formData.append('type', values.type);
    if (values.type === 'java-plugin') {
      const file = fileList[0]?.originFileObj;
      if (!file) {
        message.error(t('udfs.upload.fileRequired'));
        return;
      }
      formData.append('file', file);
    } else if (values.type === 'script' || values.type === 'sql') {
      // script/sql share the ScriptUdfPayload envelope: the controller assembles it from these fields.
      formData.append(values.type === 'script' ? 'scriptBody' : 'sql',
        (values.type === 'script' ? values.scriptBody : values.sql) ?? '');
      formData.append('sqlName', values.sqlName?.trim() ?? '');
      if (values.argCount?.trim()) {
        formData.append('argCount', values.argCount.trim());
      }
      if (values.returnType?.trim()) {
        formData.append('returnType', values.returnType.trim());
      }
      if (values.type === 'script') {
        // SCRIPT publish requires non-empty input/output JSON Schemas (D-12).
        formData.append('inputSchema', values.inputSchema ?? '');
        formData.append('outputSchema', values.outputSchema ?? '');
      }
    }
    uploadMutation.mutate(formData);
  };

  const versionColumns: ColumnsType<UdfVersionView> = useMemo(
    () => [
      { title: t('udfs.version.col.version'), dataIndex: 'version' },
      {
        title: t('udfs.version.col.state'),
        dataIndex: 'state',
        render: (state: UdfVersionView['state']) => (
          <Tag color={STATE_TAG_COLOR[state]}>{t(`udfs.status.${state}`)}</Tag>
        ),
      },
      {
        title: t('udfs.version.col.registered'),
        dataIndex: 'registeredAt',
        render: formatDateTime,
      },
      {
        title: '',
        key: 'actions',
        render: (_, row) => (
          <Space>
            <Button
              type="link"
              disabled={row.state !== 'draft'}
              loading={publishMutation.isPending}
              onClick={() => publishMutation.mutate({ udfId: row.udfId, version: row.version })}
            >
              {t('udfs.action.publish')}
            </Button>
            <Button
              type="link"
              danger
              disabled={row.state !== 'published'}
              loading={deprecateMutation.isPending}
              onClick={() => deprecateMutation.mutate({ udfId: row.udfId, version: row.version })}
            >
              {t('udfs.action.deprecate')}
            </Button>
          </Space>
        ),
      },
    ],
    [t, publishMutation, deprecateMutation],
  );

  const columns: ColumnsType<UdfGroupView> = useMemo(
    () => [
      { title: t('udfs.col.udfId'), dataIndex: 'udfId' },
      {
        title: t('udfs.col.type'),
        dataIndex: 'type',
        render: (type: string) => <Tag>{type}</Tag>,
      },
      {
        title: t('udfs.col.versions'),
        key: 'versionCount',
        render: (_, row) => row.versions.length,
      },
    ],
    [t],
  );

  return (
    <section className="console-page-panel" data-testid="udfs-page">
      <ConsolePageHeader
        title={t('udfs.title')}
        subtitle={t('udfs.subtitle')}
        crumbs={[{ label: t('nav.home'), path: '/' }, { label: t('nav.udfs') }]}
        extra={
          <Button type="primary" data-testid="udfs-upload-button" onClick={openUpload}>
            {t('udfs.upload.button')}
          </Button>
        }
      />

      <Space style={{ marginBottom: 16 }} wrap>
        <Select
          value={typeFilter}
          style={{ width: 200 }}
          onChange={setTypeFilter}
          options={[
            { value: '', label: t('udfs.filter.all') },
            { value: 'java-plugin', label: 'java-plugin' },
            { value: 'script', label: 'script' },
            { value: 'sql', label: 'sql' },
          ]}
        />
        <Button onClick={() => udfsQuery.refetch()}>{t('common.refresh')}</Button>
      </Space>

      <Table
        rowKey={(row) => row.udfId}
        loading={udfsQuery.isLoading}
        dataSource={udfsQuery.data ?? []}
        columns={columns}
        pagination={{ pageSize: 20 }}
        locale={{ emptyText: t('udfs.empty') }}
        expandable={{
          expandedRowRender: (row) => (
            <Table
              rowKey={(v) => `${v.udfId}@${v.version}`}
              dataSource={row.versions}
              columns={versionColumns}
              pagination={false}
              size="small"
            />
          ),
        }}
      />

      <Modal
        title={t('udfs.upload.title')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={uploadMutation.isPending}
        okText={t('udfs.upload.submit')}
        cancelText={t('common.cancel')}
        destroyOnClose
        width={560}
      >
        <Form form={form} layout="vertical" onFinish={submitUpload}>
          <Form.Item
            name="udfId"
            label={t('udfs.upload.udfId')}
            rules={[{ required: true, message: t('udfs.upload.udfIdRequired') }]}
          >
            <Input placeholder="com.acme.greet" />
          </Form.Item>
          <Form.Item
            name="version"
            label={t('udfs.upload.version')}
            rules={[{ required: true, message: t('udfs.upload.versionRequired') }]}
          >
            <Input placeholder="1.0.0" />
          </Form.Item>
          <Form.Item name="type" label={t('udfs.upload.type')} rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'java-plugin', label: 'java-plugin' },
                { value: 'script', label: 'script' },
                { value: 'sql', label: 'sql' },
              ]}
            />
          </Form.Item>
          {selectedType === 'java-plugin' && (
            <Form.Item label={t('udfs.upload.file')}>
              <Upload.Dragger
                beforeUpload={() => false}
                maxCount={1}
                fileList={fileList}
                onChange={({ fileList: next }) => setFileList(next.slice(-1))}
              >
                <p className="ant-upload-text">{t('udfs.upload.fileHint')}</p>
              </Upload.Dragger>
            </Form.Item>
          )}
          {selectedType === 'script' && (
            <>
              <Form.Item
                name="scriptBody"
                label={t('udfs.upload.script')}
                rules={[{ required: true, message: t('udfs.upload.scriptRequired') }]}
              >
                <Input.TextArea rows={8} placeholder="return String(args[0]);" />
              </Form.Item>
              <Form.Item
                name="sqlName"
                label={t('udfs.upload.sqlName')}
                rules={[{ required: true, message: t('udfs.upload.sqlNameRequired') }]}
              >
                <Input placeholder="V2_FORMAT_PHONE" />
              </Form.Item>
              <Form.Item name="argCount" label={t('udfs.upload.argCount')}>
                <Input placeholder="1" />
              </Form.Item>
              <Form.Item name="returnType" label={t('udfs.upload.returnType')}>
                <Input placeholder="VARCHAR" />
              </Form.Item>
              <Form.Item
                name="inputSchema"
                label={t('udfs.upload.inputSchema')}
                rules={[{ required: true, message: t('udfs.upload.inputSchemaRequired') }]}
              >
                <Input.TextArea rows={4} placeholder='{"type":"string"}' />
              </Form.Item>
              <Form.Item
                name="outputSchema"
                label={t('udfs.upload.outputSchema')}
                rules={[{ required: true, message: t('udfs.upload.outputSchemaRequired') }]}
              >
                <Input.TextArea rows={4} placeholder='{"type":"string"}' />
              </Form.Item>
            </>
          )}
          {selectedType === 'sql' && (
            <>
              <Form.Item
                name="sql"
                label={t('udfs.upload.sql')}
                rules={[{ required: true, message: t('udfs.upload.sqlRequired') }]}
              >
                <Input.TextArea rows={6} placeholder="return String(args[0]).toUpperCase();" />
              </Form.Item>
              <Form.Item
                name="sqlName"
                label={t('udfs.upload.sqlName')}
                rules={[{ required: true, message: t('udfs.upload.sqlNameRequired') }]}
              >
                <Input placeholder="V2_MASK_EMAIL" />
              </Form.Item>
              <Form.Item name="argCount" label={t('udfs.upload.argCount')}>
                <Input placeholder="1" />
              </Form.Item>
              <Form.Item name="returnType" label={t('udfs.upload.returnType')}>
                <Input placeholder="VARCHAR" />
              </Form.Item>
            </>
          )}
        </Form>
      </Modal>
    </section>
  );
}
