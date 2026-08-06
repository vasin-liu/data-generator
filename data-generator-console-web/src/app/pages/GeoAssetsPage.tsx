import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Button,
  Card,
  Col,
  Empty,
  Form,
  Input,
  Modal,
  Row,
  Space,
  Spin,
  Table,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';
import { lazy, Suspense, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  deleteGeoAsset,
  fetchGeoAssetGeoJson,
  fetchGeoAssets,
  uploadGeoAsset,
} from '../../api/geoAssets';
import { ApiRequestError } from '../../api/client';
import type {
  GeoAssetInUsePayload,
  GeoAssetSummary,
  GeoAssetTemplateUsage,
  GeoJsonObject,
} from '../../api/types';
import { ConsolePageHeader } from '../../components/ConsolePageHeader';

const GeoMapPreview = lazy(() => import('../geo/GeoMapPreview'));

type UploadFormValues = {
  name?: string;
};

function formatBboxSummary(row: GeoAssetSummary): string {
  const { minLon, minLat, maxLon, maxLat } = row;
  if (
    [minLon, minLat, maxLon, maxLat].some((n) => n == null || !Number.isFinite(Number(n)))
  ) {
    return '—';
  }
  const fmt = (n: number) => Number(n).toFixed(4);
  return `[${fmt(minLon)}, ${fmt(minLat)}, ${fmt(maxLon)}, ${fmt(maxLat)}]`;
}

function formatUsageList(usages: GeoAssetTemplateUsage[]): string {
  return usages
    .map((u) => `${u.templateName} (${u.templateId})`)
    .join(', ');
}

/**
 * Operator console geo-asset registry: left list + right MapLibre preview,
 * multipart upload, and hard-delete with 409 usage Modal (D-01..D-05, D-11).
 */
export function GeoAssetsPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [form] = Form.useForm<UploadFormValues>();

  const assetsQuery = useQuery({
    queryKey: ['geo-assets'],
    queryFn: fetchGeoAssets,
  });

  const geojsonQuery = useQuery({
    queryKey: ['geo-assets', selectedId, 'geojson'],
    queryFn: () => fetchGeoAssetGeoJson(selectedId!),
    enabled: Boolean(selectedId),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['geo-assets'] });

  const showInUseModal = (name: string, usages: GeoAssetTemplateUsage[]) => {
    Modal.info({
      title: t('geoAssets.delete.inUseTitle'),
      content: t('geoAssets.delete.inUseBody', {
        name,
        usageList: formatUsageList(usages) || '—',
      }),
      okText: t('geoAssets.delete.inUseDismiss'),
    });
  };

  const uploadMutation = useMutation({
    mutationFn: (formData: FormData) => uploadGeoAsset(formData),
    onSuccess: (uploaded) => {
      message.success(t('geoAssets.uploaded'));
      setUploadOpen(false);
      setFileList([]);
      form.resetFields();
      void invalidate().then(() => setSelectedId(uploaded.id));
    },
    onError: (err: Error) => {
      message.error(t('geoAssets.error.upload', { serverMessage: err.message }));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteGeoAsset(id),
    onSuccess: (_data, id) => {
      message.success(t('geoAssets.deleted'));
      if (selectedId === id) {
        setSelectedId(null);
      }
      invalidate();
    },
    onError: (err: Error, id) => {
      // D-04: 409 → usage Modal; list stays unchanged (no soft-delete).
      if (err instanceof ApiRequestError && err.status === 409) {
        const payload = err.data as GeoAssetInUsePayload | null | undefined;
        const usages = payload?.usages ?? [];
        const row = (assetsQuery.data ?? []).find((a) => a.id === id);
        showInUseModal(row?.name ?? id, usages);
        return;
      }
      message.error(err.message);
    },
  });

  const openUpload = () => {
    form.resetFields();
    setFileList([]);
    setUploadOpen(true);
  };

  const submitUpload = (values: UploadFormValues) => {
    const file = fileList[0]?.originFileObj;
    if (!file) {
      message.error(t('geoAssets.upload.fileRequired'));
      return;
    }
    const formData = new FormData();
    formData.append('file', file);
    const name = values.name?.trim();
    if (name) {
      formData.append('name', name);
    }
    uploadMutation.mutate(formData);
  };

  const confirmDelete = (row: GeoAssetSummary) => {
    Modal.confirm({
      title: t('geoAssets.delete.confirmTitle'),
      content: t('geoAssets.delete.confirmBody', { name: row.name }),
      okText: t('geoAssets.delete.confirm'),
      cancelText: t('common.cancel'),
      okButtonProps: { danger: true },
      onOk: () => deleteMutation.mutateAsync(row.id),
    });
  };

  // D-02: show contentType only when at least one list row exposes it.
  const hasContentType = (assetsQuery.data ?? []).some(
    (a) => a.contentType != null && a.contentType !== '',
  );

  const columns: ColumnsType<GeoAssetSummary> = useMemo(() => {
    const cols: ColumnsType<GeoAssetSummary> = [
      { title: t('geoAssets.col.name'), dataIndex: 'name', ellipsis: true },
      {
        title: t('geoAssets.col.featureCount'),
        dataIndex: 'featureCount',
        width: 100,
      },
      {
        title: t('geoAssets.col.bbox'),
        key: 'bbox',
        ellipsis: true,
        render: (_, row) => (
          <span style={{ fontSize: 12, lineHeight: 1.5 }}>{formatBboxSummary(row)}</span>
        ),
      },
    ];
    if (hasContentType) {
      cols.push({
        title: t('geoAssets.col.contentType'),
        dataIndex: 'contentType',
        width: 120,
        ellipsis: true,
        render: (v: string | null | undefined) => v || '—',
      });
    }
    cols.push({
      title: '',
      key: 'actions',
      width: 88,
      render: (_, row) => (
        <Button
          type="link"
          danger
          size="small"
          loading={deleteMutation.isPending && deleteMutation.variables === row.id}
          onClick={(e) => {
            e.stopPropagation();
            confirmDelete(row);
          }}
        >
          {t('geoAssets.action.delete')}
        </Button>
      ),
    });
    return cols;
    // confirmDelete / deleteMutation close over latest t + query data; omit from deps to avoid column churn.
    // eslint-disable-next-line react-hooks/exhaustive-deps -- mirror UdfsPage action column pattern
  }, [t, hasContentType, deleteMutation.isPending, deleteMutation.variables]);

  const mapGeoJson: GeoJsonObject | null =
    geojsonQuery.data && !geojsonQuery.isError ? geojsonQuery.data : null;
  const showMapHonesty = Boolean(selectedId && mapGeoJson);

  return (
    <section className="console-page-panel" data-testid="geo-assets-page">
      <ConsolePageHeader
        title={t('geoAssets.title')}
        subtitle={t('geoAssets.subtitle')}
        crumbs={[{ label: t('nav.home'), path: '/' }, { label: t('nav.geoAssets') }]}
        extra={
          <Button type="primary" data-testid="geo-assets-upload" onClick={openUpload}>
            {t('geoAssets.upload.button')}
          </Button>
        }
      />

      {assetsQuery.isError ? (
        <Empty description={t('geoAssets.error.list')} />
      ) : (
        <Row gutter={32} wrap={false} style={{ alignItems: 'stretch' }}>
          <Col flex="0 0 40%" style={{ minWidth: 320, maxWidth: '40%' }}>
            <Card styles={{ body: { padding: 16 } }}>
              <Table
                rowKey={(row) => row.id}
                loading={assetsQuery.isLoading}
                dataSource={assetsQuery.data ?? []}
                columns={columns}
                pagination={{ pageSize: 20, size: 'small' }}
                size="small"
                locale={{
                  emptyText: (
                    <Empty
                      description={
                        <Space direction="vertical" size={4}>
                          <span>{t('geoAssets.empty.heading')}</span>
                          <span style={{ fontSize: 12 }}>{t('geoAssets.empty.body')}</span>
                        </Space>
                      }
                    />
                  ),
                }}
                rowClassName={(row) => (row.id === selectedId ? 'ant-table-row-selected' : '')}
                onRow={(row) => ({
                  onClick: () => setSelectedId(row.id),
                  style: { cursor: 'pointer' },
                })}
              />
            </Card>
          </Col>
          <Col flex="1 1 60%" style={{ minWidth: 0 }}>
            <Card styles={{ body: { padding: 16, minHeight: 440 } }}>
              <Spin spinning={Boolean(selectedId) && geojsonQuery.isFetching}>
                {geojsonQuery.isError && selectedId ? (
                  <Empty description={t('geoAssets.error.map')} />
                ) : (
                  <Suspense fallback={<Spin style={{ display: 'block', margin: '120px auto' }} />}>
                    <GeoMapPreview
                      geojson={mapGeoJson}
                      honesty={showMapHonesty ? 'geometry' : 'none'}
                      honestyText={
                        showMapHonesty ? t('geoAssets.honesty.geometry') : undefined
                      }
                      emptyCaption={t('geoAssets.map.selectAsset')}
                      height={400}
                      data-testid="geo-assets-map"
                    />
                  </Suspense>
                )}
              </Spin>
            </Card>
          </Col>
        </Row>
      )}

      <Modal
        title={t('geoAssets.upload.title')}
        open={uploadOpen}
        onCancel={() => setUploadOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={uploadMutation.isPending}
        okText={t('geoAssets.upload.submit')}
        cancelText={t('common.cancel')}
        destroyOnClose
        width={560}
      >
        <Form form={form} layout="vertical" onFinish={submitUpload}>
          <Form.Item name="name" label={t('geoAssets.upload.name')}>
            <Input placeholder={t('geoAssets.upload.namePlaceholder')} />
          </Form.Item>
          <Form.Item label={t('geoAssets.upload.file')} required>
            <Upload.Dragger
              beforeUpload={() => false}
              maxCount={1}
              accept=".geojson,.json,application/geo+json,application/json"
              fileList={fileList}
              onChange={({ fileList: next }) => setFileList(next.slice(-1))}
            >
              <p className="ant-upload-text">{t('geoAssets.upload.fileHint')}</p>
            </Upload.Dragger>
          </Form.Item>
        </Form>
      </Modal>
    </section>
  );
}
