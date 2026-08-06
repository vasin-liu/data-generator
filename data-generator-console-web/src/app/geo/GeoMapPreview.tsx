import bbox from '@turf/bbox';
import { Alert, theme, Typography } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import Map, { Layer, Source, type MapRef } from 'react-map-gl/maplibre';
import type { StyleSpecification } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';

/** Honesty slot mode for preview ≠ full-run messaging (D-09..D-11). */
export type GeoMapHonesty = 'sampling' | 'geometry' | 'none';

/** Loose GeoJSON payload accepted by MapLibre sources (never rendered as HTML). */
export type GeoMapGeoJson = {
  type: string;
  [key: string]: unknown;
};

export interface GeoMapPreviewProps {
  /** Uploaded asset or underlay Feature/FeatureCollection. */
  geojson?: GeoMapGeoJson | null;
  /** Client Turf BBOX/CIRCLE guide geometry. */
  guides?: GeoMapGeoJson | null;
  /** Capped synthetic point FeatureCollection. */
  points?: GeoMapGeoJson | null;
  /** When not {@code none}, shows persistent warning Alert above the map. */
  honesty?: GeoMapHonesty;
  /** Pre-resolved honesty copy (i18n done by caller). */
  honestyText?: string;
  /** Map canvas height; assets default ≥400px, picker mini-map ~200px. */
  height?: number | string;
  /** Caption when no layers are selected. */
  emptyCaption?: string;
  /** Attribution caption override (defaults to OSM). */
  attributionCaption?: string;
  'data-testid'?: string;
}

const OSM_STYLE: StyleSpecification = {
  version: 8,
  sources: {
    'osm-raster': {
      type: 'raster',
      tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
      tileSize: 256,
      attribution: '© OpenStreetMap contributors',
    },
  },
  layers: [
    {
      id: 'osm-raster-layer',
      type: 'raster',
      source: 'osm-raster',
      minzoom: 0,
      maxzoom: 19,
    },
  ],
};

const DEFAULT_VIEW = { longitude: 113.5, latitude: 23.1, zoom: 8 };

function collectFitTargets(
  geojson?: GeoMapGeoJson | null,
  guides?: GeoMapGeoJson | null,
  points?: GeoMapGeoJson | null,
): GeoMapGeoJson[] {
  return [geojson, guides, points].filter((g): g is GeoMapGeoJson => g != null && typeof g.type === 'string');
}

/**
 * Shared MapLibre preview panel for geo assets, synthetic editor, and asset picker.
 * Import only via {@code React.lazy} so maplibre CSS stays out of non-geo chunks.
 *
 * @param props map layers, honesty slot, and sizing
 */
export function GeoMapPreview({
  geojson = null,
  guides = null,
  points = null,
  honesty = 'none',
  honestyText,
  height = 400,
  emptyCaption,
  attributionCaption = '© OpenStreetMap contributors',
  'data-testid': testId = 'geo-assets-map',
}: GeoMapPreviewProps) {
  const { token } = theme.useToken();
  const mapRef = useRef<MapRef>(null);
  // Client-only mount guard — MapLibre needs a browser DOM (Pitfall 4).
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const accent = token.colorPrimary;
  const warning = token.colorWarning;
  const hasLayers = Boolean(geojson || guides || points);
  const showHonesty = honesty !== 'none' && Boolean(honestyText);

  const fitBoundsKey = useMemo(() => {
    const targets = collectFitTargets(geojson, guides, points);
    if (targets.length === 0) {
      return null;
    }
    try {
      // Turf accepts Feature / FeatureCollection / Geometry; cast keeps deps untyped.
      const bounds = bbox({
        type: 'FeatureCollection',
        features: targets.flatMap((t) => {
          if (t.type === 'FeatureCollection' && Array.isArray(t.features)) {
            return t.features as object[];
          }
          if (t.type === 'Feature') {
            return [t];
          }
          return [{ type: 'Feature', properties: {}, geometry: t }];
        }),
      } as never);
      if (!bounds.every((n) => Number.isFinite(n))) {
        return null;
      }
      return bounds.join(',');
    } catch {
      return null;
    }
  }, [geojson, guides, points]);

  useEffect(() => {
    if (!mounted || !fitBoundsKey || !mapRef.current) {
      return;
    }
    const parts = fitBoundsKey.split(',').map(Number);
    if (parts.length !== 4 || parts.some((n) => !Number.isFinite(n))) {
      return;
    }
    const [minLon, minLat, maxLon, maxLat] = parts;
    // Degenerate point bbox — pad slightly so fitBounds still works.
    const pad = minLon === maxLon || minLat === maxLat ? 0.01 : 0;
    mapRef.current.fitBounds(
      [
        [minLon - pad, minLat - pad],
        [maxLon + pad, maxLat + pad],
      ],
      { padding: 40, duration: 0 },
    );
  }, [mounted, fitBoundsKey]);

  const canvasHeight = typeof height === 'number' ? `${height}px` : height;

  return (
    <div data-testid={testId} style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {showHonesty ? (
        <Alert
          type="warning"
          showIcon
          message={honestyText}
          data-testid="geo-assets-honesty-alert"
          style={{ marginBottom: 0 }}
        />
      ) : null}
      <div style={{ position: 'relative', width: '100%', height: canvasHeight, minHeight: 200 }}>
        {mounted ? (
          <Map
            ref={mapRef}
            mapLib={import('maplibre-gl')}
            initialViewState={DEFAULT_VIEW}
            style={{ width: '100%', height: '100%' }}
            mapStyle={OSM_STYLE}
            attributionControl={false}
          >
            {geojson ? (
              <Source id="geo-asset" type="geojson" data={geojson as never}>
                <Layer
                  id="geo-asset-fill"
                  type="fill"
                  filter={['==', '$type', 'Polygon']}
                  paint={{
                    'fill-color': accent,
                    'fill-opacity': 0.25,
                  }}
                />
                <Layer
                  id="geo-asset-line"
                  type="line"
                  paint={{
                    'line-color': accent,
                    'line-width': 2,
                  }}
                />
              </Source>
            ) : null}
            {guides ? (
              <Source id="geo-guides" type="geojson" data={guides as never}>
                <Layer
                  id="geo-guides-fill"
                  type="fill"
                  filter={['==', '$type', 'Polygon']}
                  paint={{
                    'fill-color': warning,
                    'fill-opacity': 0.1,
                  }}
                />
                <Layer
                  id="geo-guides-line"
                  type="line"
                  paint={{
                    'line-color': warning,
                    'line-width': 2,
                    'line-dasharray': [2, 2],
                  }}
                />
              </Source>
            ) : null}
            {points ? (
              <Source id="geo-points" type="geojson" data={points as never}>
                <Layer
                  id="geo-points-circle"
                  type="circle"
                  paint={{
                    'circle-radius': 4.5,
                    'circle-color': accent,
                    'circle-stroke-width': 1,
                    'circle-stroke-color': '#ffffff',
                  }}
                />
              </Source>
            ) : null}
          </Map>
        ) : (
          <div style={{ width: '100%', height: '100%', background: token.colorFillSecondary }} />
        )}
        {!hasLayers && emptyCaption ? (
          <Typography.Text
            type="secondary"
            style={{
              position: 'absolute',
              inset: 0,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              pointerEvents: 'none',
              fontSize: 12,
              padding: 16,
              textAlign: 'center',
            }}
          >
            {emptyCaption}
          </Typography.Text>
        ) : null}
      </div>
      <Typography.Text type="secondary" style={{ fontSize: 12, lineHeight: 1.5 }}>
        {attributionCaption}
      </Typography.Text>
    </div>
  );
}

export default GeoMapPreview;
