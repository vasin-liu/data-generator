import { apiFormRequest, apiRequest, consoleRoleHeaders } from './client';
import type {
  GeoAssetSummary,
  GeoAssetUploadView,
  GeoJsonObject,
  GeoSyntheticPreviewRequest,
  GeoSyntheticPreviewView,
} from './types';

const API_BASE = '/api';

/**
 * Lists registered geo assets (summary rows without GeoJSON body).
 */
export function fetchGeoAssets(): Promise<GeoAssetSummary[]> {
  return apiRequest<GeoAssetSummary[]>('/console/geo-assets');
}

/**
 * Fetches raw GeoJSON for an asset. Does not use {@link apiRequest} — the
 * endpoint returns {@code application/geo+json}, not an {@code R} envelope.
 *
 * @param id asset UUID
 */
export async function fetchGeoAssetGeoJson(id: string): Promise<GeoJsonObject> {
  const res = await fetch(`${API_BASE}/console/geo-assets/${encodeURIComponent(id)}/geojson`, {
    headers: {
      Accept: 'application/geo+json, application/json',
      ...consoleRoleHeaders(),
    },
  });
  if (!res.ok) {
    throw new Error((await res.text()) || `GeoJSON fetch failed (${res.status})`);
  }
  return (await res.json()) as GeoJsonObject;
}

/**
 * Uploads a GeoJSON multipart form ({@code file} + optional {@code name}).
 *
 * @param form multipart body
 */
export function uploadGeoAsset(form: FormData): Promise<GeoAssetUploadView> {
  return apiFormRequest<GeoAssetUploadView>('/console/geo-assets', form);
}

/**
 * Hard-deletes a geo asset. On HTTP 409 throws {@link ApiRequestError} with
 * {@code data.usages} for the delete Modal (D-04).
 *
 * @param id asset UUID
 */
export function deleteGeoAsset(id: string): Promise<void> {
  return apiRequest<void>(`/console/geo-assets/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  });
}

/**
 * Resolves path/classpath/{@code asset:} GeoJSON for map underlays (D-06).
 * Returns raw geo+json — not an {@code R} envelope (locked Plan 22-01 path).
 *
 * @param location classpath, filesystem, or {@code asset:{uuid}} location
 */
export async function previewLocationGeoJson(location: string): Promise<GeoJsonObject> {
  const res = await fetch(`${API_BASE}/console/geo-assets/preview/location`, {
    method: 'POST',
    headers: {
      Accept: 'application/geo+json, application/json',
      'Content-Type': 'application/json',
      ...consoleRoleHeaders(),
    },
    body: JSON.stringify({ location }),
  });
  if (!res.ok) {
    throw new Error((await res.text()) || `Location preview failed (${res.status})`);
  }
  return (await res.json()) as GeoJsonObject;
}

/**
 * Capped synthetic point sample for map honesty preview (D-08).
 * Locked path: {@code POST /api/console/geo-assets/preview/synthetic}.
 *
 * @param body mode config + seed + maxCount (≤ 500)
 */
export function previewSyntheticPoints(
  body: GeoSyntheticPreviewRequest,
): Promise<GeoSyntheticPreviewView> {
  return apiRequest<GeoSyntheticPreviewView>('/console/geo-assets/preview/synthetic', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}
