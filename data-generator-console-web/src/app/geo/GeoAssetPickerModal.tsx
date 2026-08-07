import React, { lazy, Suspense, useEffect, useState, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Col, Modal, Row, Space, Spin, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useTranslation } from 'react-i18next';
import { fetchGeoAssetGeoJson, fetchGeoAssets } from '../../api/geoAssets';
import type { GeoAssetSummary, GeoJsonObject } from '../../api/types';

const GeoMapPreview = lazy(() => import('./GeoMapPreview'));

type Props = {
  open: boolean;
  onClose: () => void;
  /** Writes the selected asset UUID into the calling form field. */
  onConfirm: (assetId: string) => void;
};

/**
 * Modal picker for geo assets (D-15): list + optional mini-map; confirm writes asset id.
 * Footer uses Use this asset / Close (never Cancel).
 */
export function GeoAssetPickerModal({ open, onClose, onConfirm }: Props) {
  const { t } = useTranslation();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [mapFailed, setMapFailed] = useState(false);

  useEffect(() => {
    if (!open) {
      setSelectedId(null);
      setMapFailed(false);
    }
  }, [open]);

  const assetsQuery = useQuery({
    queryKey: ['geo-assets'],
    queryFn: fetchGeoAssets,
    enabled: open,
  });

  const geojsonQuery = useQuery({
    queryKey: ['geo-assets', selectedId, 'geojson', 'picker'],
    queryFn: () => fetchGeoAssetGeoJson(selectedId!),
    enabled: open && Boolean(selectedId) && !mapFailed,
  });

  const columns: ColumnsType<GeoAssetSummary> = [
    {
      title: t('geoAssets.col.name'),
      dataIndex: 'name',
      ellipsis: true,
    },
    {
      title: t('geoAssets.col.featureCount'),
      dataIndex: 'featureCount',
      width: 96,
    },
  ];

  const handleConfirm = () => {
    if (!selectedId) {
      return;
    }
    onConfirm(selectedId);
    onClose();
  };

  const underlay: GeoJsonObject | null =
    geojsonQuery.data && !geojsonQuery.isError ? geojsonQuery.data : null;

  return (
    <Modal
      title={t('geoAssets.picker.title')}
      open={open}
      onCancel={onClose}
      width={720}
      destroyOnClose
      footer={
        <Space>
          <Button onClick={onClose}>{t('geoAssets.picker.close')}</Button>
          <Button type="primary" disabled={!selectedId} onClick={handleConfirm}>
            {t('geoAssets.picker.confirm')}
          </Button>
        </Space>
      }
    >
      <div data-testid="geo-synthetic-picker">
        <Row gutter={16}>
          <Col xs={24} md={14}>
            <Table<GeoAssetSummary>
              rowKey="id"
              size="small"
              loading={assetsQuery.isLoading}
              dataSource={assetsQuery.data ?? []}
              columns={columns}
              pagination={false}
              scroll={{ y: 280 }}
              rowSelection={{
                type: 'radio',
                selectedRowKeys: selectedId ? [selectedId] : [],
                onChange: (keys) => setSelectedId((keys[0] as string) ?? null),
              }}
              onRow={(row) => ({
                onClick: () => setSelectedId(row.id),
              })}
              locale={{
                emptyText: assetsQuery.isError
                  ? t('geoAssets.error.list')
                  : t('geoAssets.empty.heading'),
              }}
            />
          </Col>
          <Col xs={24} md={10}>
            {mapFailed || geojsonQuery.isError ? (
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {t('geoAssets.picker.mapUnavailable')}
              </Typography.Text>
            ) : (
              <Suspense
                fallback={<Spin style={{ display: 'block', margin: '48px auto' }} />}
              >
                <MapErrorCatch onFail={() => setMapFailed(true)}>
                  <GeoMapPreview
                    height={200}
                    geojson={underlay}
                    honesty={underlay ? 'geometry' : 'none'}
                    honestyText={underlay ? t('geoAssets.honesty.geometry') : undefined}
                    emptyCaption={t('geoAssets.map.selectAsset')}
                    data-testid="geo-synthetic-picker-map"
                  />
                </MapErrorCatch>
              </Suspense>
            )}
          </Col>
        </Row>
        {assetsQuery.isError ? (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message={t('geoAssets.error.list')}
          />
        ) : null}
      </div>
    </Modal>
  );
}

/** Minimal boundary so a map chunk failure falls back to list-only (D-15). */
class MapErrorCatch extends React.Component<
  { children: ReactNode; onFail: () => void },
  { failed: boolean }
> {
  state = { failed: false };

  static getDerivedStateFromError(): { failed: boolean } {
    return { failed: true };
  }

  componentDidCatch(): void {
    this.props.onFail();
  }

  render() {
    // Parent swaps to Caption via onFail → mapFailed; avoid hard-coded copy here.
    if (this.state.failed) {
      return null;
    }
    return this.props.children;
  }
}
