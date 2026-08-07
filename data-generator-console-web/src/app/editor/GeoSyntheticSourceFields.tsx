import circle from '@turf/circle';
import { polygon } from '@turf/helpers';
import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Spin,
  message,
} from 'antd';
import { lazy, Suspense, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  fetchGeoAssetGeoJson,
  previewLocationGeoJson,
  previewSyntheticPoints,
} from '../../api/geoAssets';
import type { GeoJsonObject, SourceDraft } from '../../api/types';
import { FieldHelp } from '../../components/FieldHelp';
import { SourceFileInput } from '../../components/SourceFileInput';
import { GeoAssetPickerModal } from '../geo/GeoAssetPickerModal';
import type { GeoMapGeoJson, GeoMapHonesty } from '../geo/GeoMapPreview';

const GeoMapPreview = lazy(() => import('../geo/GeoMapPreview'));

/** Preview honesty cap — must match server PREVIEW_MAX_COUNT (T-22-01). */
const PREVIEW_MAX_COUNT = 500;

type GeoSyntheticMode = 'BOUNDARY_POINTS' | 'LINE_SAMPLE' | 'BBOX' | 'CIRCLE';

type Props = {
  source: SourceDraft;
  readOnly: boolean;
  onPatch: (patch: SourceDraft) => void;
};

type PickerRole = 'boundary' | 'network' | null;

function asString(value: unknown): string {
  return typeof value === 'string' ? value : '';
}

function asNumber(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}

function asNumberArray(value: unknown): number[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }
  const nums = value.map((n) => Number(n));
  return nums.every((n) => Number.isFinite(n)) ? nums : undefined;
}

function nonBlank(value: string | undefined | null): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

/** Client Turf rectangle for BBOX guide (D-07). */
function bboxGuideFeature(bbox: number[]): GeoMapGeoJson | null {
  if (bbox.length !== 4 || bbox.some((n) => !Number.isFinite(n))) {
    return null;
  }
  const [minLon, minLat, maxLon, maxLat] = bbox;
  return polygon([
    [
      [minLon, minLat],
      [maxLon, minLat],
      [maxLon, maxLat],
      [minLon, maxLat],
      [minLon, minLat],
    ],
  ]) as unknown as GeoMapGeoJson;
}

/** Client Turf circle for CIRCLE guide (D-07); radius in meters → km. */
function circleGuideFeature(center: number[], radiusMeters: number): GeoMapGeoJson | null {
  if (center.length !== 2 || center.some((n) => !Number.isFinite(n)) || !(radiusMeters > 0)) {
    return null;
  }
  return circle([center[0], center[1]], radiusMeters / 1000, {
    units: 'kilometers',
    steps: 64,
  }) as unknown as GeoMapGeoJson;
}

/**
 * Mode-switched geo_synthetic editor fields + hybrid map preview (GEO-12 / GEO-13).
 */
export function GeoSyntheticSourceFields({ source, readOnly, onPatch }: Props) {
  const { t } = useTranslation();
  const mode = (asString(source.mode) || 'BBOX').toUpperCase() as GeoSyntheticMode;
  const seed = asNumber(source.seed, 0);
  const count = asNumber(source.count, 100);
  const boundaryAssetId = asString(source.boundaryAssetId);
  const boundaryPath = asString(source.boundaryPath);
  const networkAssetId = asString(source.networkAssetId);
  const networkPath = asString(source.networkPath);
  const bbox = asNumberArray(source.bbox) ?? [113.2, 23.0, 113.5, 23.2];
  const center = asNumberArray(source.center) ?? [113.3, 23.1];
  const radiusMeters = asNumber(source.radiusMeters, 500);
  const sample = (source.sample as { strategy?: string; spacingMeters?: number } | undefined) ?? {
    strategy: 'BY_COUNT',
    spacingMeters: 50,
  };

  const [pickerRole, setPickerRole] = useState<PickerRole>(null);
  const [samplePoints, setSamplePoints] = useState<GeoJsonObject | null>(null);
  const [sampleCap, setSampleCap] = useState(PREVIEW_MAX_COUNT);
  const [sampleSeed, setSampleSeed] = useState(seed);
  const [sampling, setSampling] = useState(false);

  const boundaryBoth =
    Boolean(nonBlank(boundaryAssetId)) && Boolean(nonBlank(boundaryPath));
  const networkBoth = Boolean(nonBlank(networkAssetId)) && Boolean(nonBlank(networkPath));

  // Asset-id wins for underlay fetch when both are set (D-16 preview preference).
  const underlayAssetId =
    mode === 'BOUNDARY_POINTS'
      ? nonBlank(boundaryAssetId)
      : mode === 'LINE_SAMPLE'
        ? nonBlank(networkAssetId)
        : undefined;
  const underlayPath =
    underlayAssetId != null
      ? undefined
      : mode === 'BOUNDARY_POINTS'
        ? nonBlank(boundaryPath)
        : mode === 'LINE_SAMPLE'
          ? nonBlank(networkPath)
          : undefined;

  const assetGeoJsonQuery = useQuery({
    queryKey: ['geo-synthetic-underlay-asset', underlayAssetId],
    queryFn: () => fetchGeoAssetGeoJson(underlayAssetId!),
    enabled: Boolean(underlayAssetId),
  });

  const pathGeoJsonQuery = useQuery({
    queryKey: ['geo-synthetic-underlay-path', underlayPath],
    queryFn: () => previewLocationGeoJson(underlayPath!),
    enabled: Boolean(underlayPath),
  });

  const underlay: GeoMapGeoJson | null = useMemo(() => {
    if (underlayAssetId) {
      return (assetGeoJsonQuery.data as GeoMapGeoJson | undefined) ?? null;
    }
    if (underlayPath) {
      return (pathGeoJsonQuery.data as GeoMapGeoJson | undefined) ?? null;
    }
    return null;
  }, [underlayAssetId, underlayPath, assetGeoJsonQuery.data, pathGeoJsonQuery.data]);

  const guides: GeoMapGeoJson | null = useMemo(() => {
    if (mode === 'BBOX') {
      return bboxGuideFeature(bbox);
    }
    if (mode === 'CIRCLE') {
      return circleGuideFeature(center, radiusMeters);
    }
    return null;
  }, [mode, bbox, center, radiusMeters]);

  // Clear capped points when mode/geometry config changes so honesty stays accurate.
  const configKey = JSON.stringify({
    mode,
    seed,
    count,
    boundaryAssetId,
    boundaryPath,
    networkAssetId,
    networkPath,
    bbox,
    center,
    radiusMeters,
  });
  useEffect(() => {
    setSamplePoints(null);
  }, [configKey]);

  const hasPreviewContent = Boolean(underlay || guides || samplePoints);
  const honesty: GeoMapHonesty = samplePoints
    ? 'sampling'
    : hasPreviewContent
      ? 'geometry'
      : 'none';
  const honestyText =
    honesty === 'sampling'
      ? t('source.geoSynthetic.honesty.sampling', { cap: sampleCap, seed: sampleSeed })
      : honesty === 'geometry'
        ? t('source.geoSynthetic.honesty.geometry')
        : undefined;

  const underlayLoading =
    (Boolean(underlayAssetId) && assetGeoJsonQuery.isFetching) ||
    (Boolean(underlayPath) && pathGeoJsonQuery.isFetching);
  const underlayError =
    (underlayAssetId && assetGeoJsonQuery.isError) ||
    (underlayPath && pathGeoJsonQuery.isError);

  const modeOptions: { value: GeoSyntheticMode; label: string }[] = [
    { value: 'BOUNDARY_POINTS', label: t('source.geoSynthetic.mode.boundary') },
    { value: 'LINE_SAMPLE', label: t('source.geoSynthetic.mode.line') },
    { value: 'BBOX', label: t('source.geoSynthetic.mode.bbox') },
    { value: 'CIRCLE', label: t('source.geoSynthetic.mode.circle') },
  ];

  const patchBbox = (index: number, value: number | null) => {
    const next = [...bbox];
    next[index] = value ?? 0;
    onPatch({ ...source, bbox: next });
  };

  const patchCenter = (index: number, value: number | null) => {
    const next = [...center];
    next[index] = value ?? 0;
    onPatch({ ...source, center: next });
  };

  const runSamplePreview = async () => {
    const maxCount = Math.min(Math.max(1, count), PREVIEW_MAX_COUNT);
    setSampling(true);
    try {
      // Prefer asset-id when both set so preview matches D-16 runtime preference messaging.
      const view = await previewSyntheticPoints({
        mode,
        seed,
        maxCount,
        boundaryAssetId: nonBlank(boundaryAssetId) ?? null,
        boundaryPath: nonBlank(boundaryAssetId) ? null : (nonBlank(boundaryPath) ?? null),
        networkAssetId: nonBlank(networkAssetId) ?? null,
        networkPath: nonBlank(networkAssetId) ? null : (nonBlank(networkPath) ?? null),
        bbox: mode === 'BBOX' ? bbox : null,
        center: mode === 'CIRCLE' ? center : null,
        radiusMeters: mode === 'CIRCLE' ? radiusMeters : null,
      });
      setSamplePoints(view.featureCollection);
      setSampleCap(view.maxCountCap ?? PREVIEW_MAX_COUNT);
      setSampleSeed(view.seed);
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('source.geoSynthetic.preview.failed'));
    } finally {
      setSampling(false);
    }
  };

  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message={t('source.geoSynthetic.intro')}
      />
      <Form.Item
        label={
          <FieldHelp
            label={t('source.geoSynthetic.mode')}
            help={t('source.geoSynthetic.mode.help')}
            required
          />
        }
      >
        <Select
          disabled={readOnly}
          value={mode}
          options={modeOptions}
          onChange={(value: GeoSyntheticMode) => onPatch({ ...source, mode: value })}
        />
      </Form.Item>

      {mode === 'BOUNDARY_POINTS' ? (
        <>
          <Form.Item
            label={
              <FieldHelp
                label={t('source.geoSynthetic.boundaryAssetId')}
                help={t('source.geoSynthetic.boundaryAssetId.help')}
              />
            }
          >
            <Space.Compact style={{ width: '100%' }}>
              <Input
                readOnly
                value={boundaryAssetId}
                placeholder={t('source.geoSynthetic.assetId.placeholder')}
              />
              <Button
                disabled={readOnly}
                onClick={() => setPickerRole('boundary')}
                data-testid="geo-synthetic-pick-boundary"
              >
                {t('source.geoSynthetic.pickAsset')}
              </Button>
              {boundaryAssetId ? (
                <Button
                  disabled={readOnly}
                  onClick={() => onPatch({ ...source, boundaryAssetId: '' })}
                >
                  {t('source.geoSynthetic.clearAsset')}
                </Button>
              ) : null}
            </Space.Compact>
          </Form.Item>
          <Form.Item
            label={
              <FieldHelp
                label={t('source.geoSynthetic.boundaryPath')}
                help={t('source.geoSynthetic.boundaryPath.help')}
              />
            }
          >
            <SourceFileInput
              path={boundaryPath}
              readOnly={readOnly}
              allowPaste
              accept=".json,.geojson"
              defaultPasteName="boundary.geojson"
              onPathChange={(p) => onPatch({ ...source, boundaryPath: p })}
            />
          </Form.Item>
          {boundaryBoth ? (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 12 }}
              message={t('source.geoSynthetic.assetIdWins')}
            />
          ) : null}
        </>
      ) : null}

      {mode === 'LINE_SAMPLE' ? (
        <>
          <Form.Item
            label={
              <FieldHelp
                label={t('source.geoSynthetic.networkAssetId')}
                help={t('source.geoSynthetic.networkAssetId.help')}
              />
            }
          >
            <Space.Compact style={{ width: '100%' }}>
              <Input
                readOnly
                value={networkAssetId}
                placeholder={t('source.geoSynthetic.assetId.placeholder')}
              />
              <Button
                disabled={readOnly}
                onClick={() => setPickerRole('network')}
                data-testid="geo-synthetic-pick-network"
              >
                {t('source.geoSynthetic.pickAsset')}
              </Button>
              {networkAssetId ? (
                <Button
                  disabled={readOnly}
                  onClick={() => onPatch({ ...source, networkAssetId: '' })}
                >
                  {t('source.geoSynthetic.clearAsset')}
                </Button>
              ) : null}
            </Space.Compact>
          </Form.Item>
          <Form.Item
            label={
              <FieldHelp
                label={t('source.geoSynthetic.networkPath')}
                help={t('source.geoSynthetic.networkPath.help')}
              />
            }
          >
            <SourceFileInput
              path={networkPath}
              readOnly={readOnly}
              allowPaste
              accept=".json,.geojson"
              defaultPasteName="network.geojson"
              onPathChange={(p) => onPatch({ ...source, networkPath: p })}
            />
          </Form.Item>
          {networkBoth ? (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 12 }}
              message={t('source.geoSynthetic.assetIdWins')}
            />
          ) : null}
          <Form.Item
            label={
              <FieldHelp
                label={t('source.geoSynthetic.sample.strategy')}
                help={t('source.geoSynthetic.sample.strategy.help')}
              />
            }
          >
            <Select
              disabled={readOnly}
              value={sample.strategy ?? 'BY_COUNT'}
              options={[
                { value: 'BY_COUNT', label: t('source.geoSynthetic.sample.byCount') },
                {
                  value: 'BY_SPACING_METERS',
                  label: t('source.geoSynthetic.sample.bySpacing'),
                },
              ]}
              onChange={(strategy) =>
                onPatch({ ...source, sample: { ...sample, strategy } })
              }
            />
          </Form.Item>
          <Form.Item
            label={
              <FieldHelp
                label={t('source.geoSynthetic.sample.spacingMeters')}
                help={t('source.geoSynthetic.sample.spacingMeters.help')}
              />
            }
          >
            <InputNumber
              min={0}
              disabled={readOnly}
              style={{ width: '100%' }}
              value={sample.spacingMeters ?? 50}
              onChange={(v) =>
                onPatch({ ...source, sample: { ...sample, spacingMeters: v ?? 50 } })
              }
            />
          </Form.Item>
        </>
      ) : null}

      {mode === 'BBOX' ? (
        <>
          <Form.Item
            label={
              <FieldHelp label={t('source.geoSynthetic.bbox')} help={t('source.geoSynthetic.bbox.help')} />
            }
          >
            <Space wrap>
              {(['minLon', 'minLat', 'maxLon', 'maxLat'] as const).map((label, index) => (
                <InputNumber
                  key={label}
                  disabled={readOnly}
                  addonBefore={t(`source.geoSynthetic.bbox.${label}`)}
                  value={bbox[index]}
                  onChange={(v) => patchBbox(index, v)}
                />
              ))}
            </Space>
          </Form.Item>
        </>
      ) : null}

      {mode === 'CIRCLE' ? (
        <>
          <Form.Item
            label={
              <FieldHelp
                label={t('source.geoSynthetic.center')}
                help={t('source.geoSynthetic.center.help')}
              />
            }
          >
            <Space>
              <InputNumber
                disabled={readOnly}
                addonBefore={t('source.geoSynthetic.center.lon')}
                value={center[0]}
                onChange={(v) => patchCenter(0, v)}
              />
              <InputNumber
                disabled={readOnly}
                addonBefore={t('source.geoSynthetic.center.lat')}
                value={center[1]}
                onChange={(v) => patchCenter(1, v)}
              />
            </Space>
          </Form.Item>
          <Form.Item
            label={
              <FieldHelp
                label={t('source.geoSynthetic.radiusMeters')}
                help={t('source.geoSynthetic.radiusMeters.help')}
              />
            }
          >
            <InputNumber
              min={0}
              disabled={readOnly}
              style={{ width: '100%' }}
              value={radiusMeters}
              onChange={(v) => onPatch({ ...source, radiusMeters: v ?? 0 })}
            />
          </Form.Item>
        </>
      ) : null}

      <Form.Item
        label={<FieldHelp label={t('source.geoSynthetic.seed')} help={t('source.geoSynthetic.seed.help')} />}
      >
        <InputNumber
          disabled={readOnly}
          style={{ width: '100%' }}
          value={seed}
          onChange={(v) => onPatch({ ...source, seed: v ?? 0 })}
        />
      </Form.Item>
      <Form.Item
        label={
          <FieldHelp label={t('source.geoSynthetic.count')} help={t('source.geoSynthetic.count.help')} />
        }
      >
        <InputNumber
          min={1}
          disabled={readOnly}
          style={{ width: '100%' }}
          value={count}
          onChange={(v) => onPatch({ ...source, count: v ?? 1 })}
        />
      </Form.Item>

      <Form.Item
        label={
          <FieldHelp
            label={t('source.geoSynthetic.preview')}
            help={t('source.geoSynthetic.preview.help')}
          />
        }
      >
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          <Button
            type="primary"
            disabled={readOnly || sampling}
            loading={sampling}
            onClick={() => void runSamplePreview()}
            data-testid="geo-synthetic-preview-sample"
          >
            {t('source.geoSynthetic.previewSample')}
          </Button>
          {underlayError ? (
            <Alert
              type="error"
              showIcon
              message={
                underlayPath
                  ? t('geoAssets.error.previewPath')
                  : t('geoAssets.error.map')
              }
            />
          ) : null}
          <div style={{ position: 'relative' }}>
            {underlayLoading ? (
              <Spin style={{ position: 'absolute', zIndex: 2, inset: '40% 0 auto', margin: '0 auto' }} />
            ) : null}
            <Suspense fallback={<Spin style={{ display: 'block', margin: '48px auto' }} />}>
              <GeoMapPreview
                height={400}
                geojson={underlay}
                guides={guides}
                points={samplePoints as GeoMapGeoJson | null}
                honesty={honesty}
                honestyText={honestyText}
                emptyCaption={t('source.geoSynthetic.preview.empty')}
                data-testid="geo-synthetic-editor-map"
              />
            </Suspense>
          </div>
        </Space>
      </Form.Item>

      <GeoAssetPickerModal
        open={pickerRole != null}
        onClose={() => setPickerRole(null)}
        onConfirm={(assetId) => {
          if (pickerRole === 'boundary') {
            onPatch({ ...source, boundaryAssetId: assetId });
          } else if (pickerRole === 'network') {
            onPatch({ ...source, networkAssetId: assetId });
          }
          setPickerRole(null);
        }}
      />
    </>
  );
}
