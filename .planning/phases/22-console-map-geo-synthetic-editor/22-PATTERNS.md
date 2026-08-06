# Phase 22: Console Map + geo_synthetic Editor - Pattern Map

**Mapped:** 2026-08-06  
**Files analyzed:** 14  
**Analogs found:** 12 / 14  

> Research disabled for this phase — file list derived from `22-CONTEXT.md`, `22-UI-SPEC.md`, and ROADMAP Phase 22 success criteria. Prefer existing `src/api/*.ts` client location over UI-SPEC `app/geo/geoAssetApi.ts` path (same role; matches UDF/secrets convention).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `console-web/.../pages/GeoAssetsPage.tsx` | component | CRUD + file-I/O | `.../pages/UdfsPage.tsx` | exact |
| `console-web/src/api/geoAssets.ts` | service (API client) | request-response + file-I/O | `console-web/src/api/udfs.ts` + `client.ts` | exact |
| `console-web/.../geo/GeoMapPreview.tsx` | component | transform + request-response | *(none — MapLibre new)*; shell from `ConsolePageHeader` / Card layouts | none |
| `console-web/.../geo/GeoAssetPickerModal.tsx` | component | CRUD (select) | `.../components/ScenarioCatalogModal.tsx` | role-match |
| `console-web/.../editor/SourceFieldsForm.tsx` | component | transform (form) | same file `geojson` + `AiSourceFields` branches | exact |
| `console-web/.../editor/draftUtils.ts` | utility | transform | same file `EditableSourceKind` / `defaultSourceForKind` | exact |
| `console-web/.../editor/steps/SourcesStep.tsx` | component | CRUD (draft) | same file + `labeledOptions(..., EDITABLE_SOURCE_KINDS)` | exact |
| `console-web/src/app/App.tsx` | route | request-response | same file `udfs` route | exact |
| `console-web/.../layout/ConsoleLayout.tsx` | route / nav | request-response | same file nav after UDFs | exact |
| `console-web/src/i18n/locales/{en,zh-CN}.json` | config | — | `udfs.*` / `source.kind.*` / `nav.*` keys | exact |
| `console-web/package.json` | config | — | STACK.md MapLibre pins; no local map deps yet | role-match |
| `service/.../ConsoleGeoAssetController.java` | controller | CRUD + file-I/O (+ new preview) | same controller; multipart like `ConsoleUdfController` | exact / partial for preview |
| `service/.../dto/GeoAssetSummaryView.java` | model | CRUD | same DTO (+ optional `contentType` from `GeoAssetPO`) | exact |
| `service/.../GeoAssetService.java` (preview helpers) | service | transform + request-response | same service + `GeoSyntheticGenerator.generateRows` | role-match |

## Pattern Assignments

### `GeoAssetsPage.tsx` (component, CRUD + file-I/O)

**Analog:** `data-generator-console-web/src/app/pages/UdfsPage.tsx`

**Imports / React Query + i18n** (lines 1–21):
```typescript
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, Modal, Select, Space, Table, Tag, Upload, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { deprecateUdf, fetchUdfs, publishUdf, uploadUdf } from '../../api/udfs';
import { ConsolePageHeader } from '../../components/ConsolePageHeader';
```

**Page shell + primary Upload CTA in header `extra`** (lines 193–203) — copy for D-03:
```tsx
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
```

**List query + invalidate + upload mutation** (lines 57–90):
```typescript
const udfsQuery = useQuery({
  queryKey: ['udfs', filterType],
  queryFn: () => fetchUdfs(filterType),
});
const invalidate = () => queryClient.invalidateQueries({ queryKey: ['udfs'] });
const uploadMutation = useMutation({
  mutationFn: (formData: FormData) => uploadUdf(formData),
  onSuccess: () => {
    message.success(t('udfs.uploaded'));
    setModalOpen(false);
    invalidate();
  },
  onError: (err: Error) => message.error(err.message),
});
```

**Upload Modal with custom `okText` (not Ant Design OK)** (lines 240–248):
```tsx
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
```

**Delete confirm pattern (secondary analog):** `SchedulesPage.tsx` `Modal.confirm` (lines 162–168) — use for D-04 confirm; on 409 show a second Modal with usage list (see Shared Patterns — conflict payload).

**Layout delta for Phase 22:** Udfs is full-width Table only. Split **left Table / right map** per UI-SPEC S1 (`Row`/`Col` or CSS grid, gap 32px, both in Card). Row select → fetch geojson → pass into `GeoMapPreview`.

**List columns from existing API:** `GeoAssetSummaryView` already has `name`, `featureCount`, `minLon/minLat/maxLon/maxLat` (bbox summary client-side). `contentType` is on `GeoAssetPO` but **not** on list DTO today — show only if planner adds trivial DTO field; **no byteSize column on PO** → omit size (D-02).

---

### `src/api/geoAssets.ts` (API client, request-response + file-I/O)

**Analog:** `data-generator-console-web/src/api/udfs.ts` + `client.ts`

**CRUD / multipart** (udfs.ts lines 7–19):
```typescript
import { apiFormRequest, apiRequest } from './client';

export function fetchUdfs(type?: string): Promise<UdfGroupView[]> {
  return apiRequest<UdfGroupView[]>(`/console/udfs${suffix}`);
}
export function uploadUdf(form: FormData): Promise<UdfVersionView> {
  return apiFormRequest<UdfVersionView>('/console/udfs', form);
}
```

**Copy for geo assets:**
- `GET /console/geo-assets` → `apiRequest<GeoAssetSummary[]>`
- `POST /console/geo-assets` multipart (`file` + optional `name`) → `apiFormRequest`
- `DELETE /console/geo-assets/{id}` → `apiRequest` (needs 409-aware variant — see Shared Patterns)
- `POST /console/geo-assets/preview/synthetic` → `apiRequest` JSON body

**Raw GeoJSON fetch (no R envelope):** `ConsoleGeoAssetController.geoJson` returns `application/geo+json` bytes. `apiRequest` rejects non-JSON — **do not reuse it**. New helper pattern:
```typescript
// Mirror client.ts fetch + role headers, but parse text/JSON FeatureCollection directly
export async function fetchGeoAssetGeoJson(id: string): Promise<GeoJSON.GeoJSON> {
  const res = await fetch(`/api/console/geo-assets/${encodeURIComponent(id)}/geojson`, {
    headers: { Accept: 'application/geo+json, application/json', /* X-Console-Role */ },
  });
  if (!res.ok) throw new Error(await res.text() || `GeoJSON fetch failed (${res.status})`);
  return res.json();
}
```

**Types:** Add `GeoAssetSummary` / upload view next to other views in `src/api/types.ts` (mirror Java record fields).

---

### `GeoMapPreview.tsx` (component, transform + request-response)

**Analog:** **No existing MapLibre / react-map-gl code in repo.** New stack from STACK.md / UI-SPEC.

**Shell patterns to reuse:**
- Honesty `Alert` — same Ant Design usage as `SourceFieldsForm` query intro (lines 43–48) and `ScenarioCatalogModal` (lines 72–78): `type="warning"`, `showIcon`, `marginBottom: 8|16`, **no closable** while preview active (D-09..D-11).
- Lazy Vite chunk (discretion): first lazy route/component in console — introduce `React.lazy` + `Suspense` around map module; side-effect `import 'maplibre-gl/dist/maplibre-gl.css'` **only** inside the lazy module.

**Locked paint / basemap:** UI-SPEC map layer table (accent fill 25%, warning dashed guides, OSM raster tiles). Implement as props: `geojson?`, `guides?` (Turf Feature), `points?`, `honesty: 'sampling' | 'geometry' | 'none'`.

**Turf:** `@turf/bbox` for `fitBounds`; `@turf/circle` + helpers for CIRCLE/BBOX guides client-side (D-07) — no server round-trip.

---

### `GeoAssetPickerModal.tsx` (component, select / CRUD)

**Analog:** `data-generator-console-web/src/app/components/ScenarioCatalogModal.tsx`

**Modal + Table + i18n + enabled-when-open query** (lines 17–90):
```tsx
export function ScenarioCatalogModal({ open, onClose, onSelect }: Props) {
  const catalogQuery = useQuery({
    queryKey: ['scenario-catalog'],
    queryFn: fetchScenarioCatalog,
    enabled: open,
  });
  return (
    <Modal title={t('scenarios.title')} open={open} onCancel={onClose} footer={null} width={880} destroyOnClose>
      <Table rowKey="scenarioId" loading={catalogQuery.isLoading} dataSource={catalogQuery.data ?? []} ... />
    </Modal>
  );
}
```

**Phase 22 deltas (D-15 / UI-SPEC S3):**
- `width={720}`; left Table (`name`, `featureCount`); right optional mini-map (`GeoMapPreview` height 200px).
- Footer confirm **Use this asset** / dismiss **Close** (custom strings — not Ant Design Cancel/OK alone).
- `onSelect(assetId)` writes `boundaryAssetId` / `networkAssetId` / `assetId` via parent `onPatch`.
- Fallback Caption when map chunk unavailable — picker still usable.

---

### `SourceFieldsForm.tsx` (component, form transform)

**Analog:** same file — `geojson` branch + `AiSourceFields` mode switching.

**geojson path + SourceFileInput** (lines 286–311) — reuse for path role on `geo_synthetic`:
```tsx
if (kind === 'geojson') {
  return (
    <>
      <Form.Item label={<FieldHelp label={t('source.path')} help={t('source.path.geoHelp')} required />}>
        <SourceFileInput
          path={(source.path as string) ?? ''}
          readOnly={readOnly}
          allowPaste
          accept=".json,.geojson"
          defaultPasteName="source.geojson"
          onPathChange={(p) => onPatch({ ...source, path: p })}
        />
      </Form.Item>
      ...
    </>
  );
}
```

**Mode-switched fields analog:** `AiSourceFields` `providerType === 'INLINE' ? ... : null` / `isRemoteProvider` (lines 461–474) — mirror for `mode` Select then BOUNDARY / LINE / BBOX / CIRCLE field sets (D-14).

**Asset-id wins warning (D-16):** inline `<Alert type="warning" showIcon message={t('source.geoSynthetic.assetIdWins')} />` when both asset-id and path set for same role.

**VO field names (do not invent):** from `GeoSyntheticSourceVO` — `mode`, `count`, `seed`, `boundaryPath` / `boundaryAssetId`, `networkPath` / `networkAssetId`, `bbox`, `center`, `radiusMeters`, plus related sample/options as needed.

**Keep kinds separate:** new `if (kind === 'geo_synthetic')` branch; never fold into `geojson`.

---

### `draftUtils.ts` (utility, transform)

**Analog:** same file lines 16–35, 94–131.

```typescript
export type EditableSourceKind =
  | 'query' | 'iterator' | 'inline_rows' | 'csv' | 'json' | 'excel' | 'ai' | 'geojson';
  // ADD: | 'geo_synthetic'

export const EDITABLE_SOURCE_KINDS: EditableSourceKind[] = [ ..., 'geojson' /* + 'geo_synthetic' */ ];

export function defaultSourceForKind(kind: EditableSourceKind): SourceDraft {
  case 'geojson':
    return { type: 'geojson', path: '' };
  // ADD case 'geo_synthetic': return { type: 'geo_synthetic', mode: 'BBOX', count: 100, seed: 0, ... }
}
```

`SourcesStep` already drives kind picker from `EDITABLE_SOURCE_KINDS` via `labeledOptions(t, 'source.kind', EDITABLE_SOURCE_KINDS)` — adding the kind + i18n key is enough for registration (D-13).

---

### `App.tsx` + `ConsoleLayout.tsx` (route / nav)

**Analog:** `App.tsx` lines 17–30; `ConsoleLayout.tsx` lines 51–70.

```tsx
// App.tsx — insert after udfs, before audit
<Route path="udfs" element={<UdfsPage />} />
<Route path="geo-assets" element={<GeoAssetsPage />} />
<Route path="audit" element={<AuditPage />} />
```

```tsx
// ConsoleLayout selectedKey + navItems — after /udfs, before /audit
if (p.includes('/geo-assets')) return '/geo-assets';
{ key: '/udfs', testId: 'nav-udfs', label: t('nav.udfs'), icon: <FunctionOutlined /> },
{ key: '/geo-assets', testId: 'nav-geo-assets', label: t('nav.geoAssets'), icon: <EnvironmentOutlined /> },
{ key: '/audit', testId: 'nav-audit', label: t('nav.audit'), icon: <AuditOutlined /> },
```

---

### i18n locales (config)

**Analog:** `en.json` / `zh-CN.json` — flat dotted keys.

| Pattern to copy | Example keys |
|-----------------|--------------|
| Nav | `nav.udfs` → add `nav.geoAssets` |
| Page block | `udfs.title`, `udfs.subtitle`, `udfs.upload.*` → `geoAssets.*` per UI-SPEC Copywriting Contract |
| Source kind | `source.kind.geojson` → add `source.kind.geo_synthetic` |
| New editor | `source.geoSynthetic.*` (mode labels, assetIdWins, honesty strings) |

All Phase 22 copy must exist in **both** `en.json` and `zh-CN.json` (D-12). English strings locked in UI-SPEC Copywriting Contract.

---

### `ConsoleGeoAssetController.java` (controller, CRUD + new preview)

**Analog:** same file (Phase 21) — extend; do not invent a second controller.

**Existing surface** (lines 53–105):
```java
@RestController
@RequestMapping("/api/console/geo-assets")
@RequiredArgsConstructor
public class ConsoleGeoAssetController {
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<GeoAssetUploadView> upload(...);

    @GetMapping
    public R<List<GeoAssetSummaryView>> list();

    @GetMapping("/{id}/geojson")
    public ResponseEntity<byte[]> geoJson(@PathVariable UUID id);

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable UUID id);
}
```

**Add (discretionary paths; D-06/D-08 must hold):**
- `POST .../preview/synthetic` — body = mode config + seed + maxCount ≤ 500; service calls `GeoSyntheticGenerator.generateRows(..., geoAssetResolver)` and returns capped FeatureCollection / point list.
- Path/classpath preview helper — resolve via **same** `GeoResourceResolver` / `GeoAssetService.resolveUtf8` spine; return GeoJSON (never reimplement classpath/filesystem on the client).

**409 already wired:** `ConsoleApiAdvice.geoAssetInUse` (lines 48–55) returns `R.fail(message, GeoAssetInUsePayload(usages))` at HTTP 409. Frontend must surface `data.usages` in Modal (D-04).

**Preview service analog:** `GeoSyntheticGenerator.generateRows(GeoGenerationRequest, GeoAssetResolver)` in `data-generator-geo`.

---

### `package.json` (config)

**Analog:** STACK.md install block (no local map deps yet).

```bash
npm install react-map-gl@^8.1.0 maplibre-gl@^5.24.0
npm install @turf/bbox@^7.2.0 @turf/circle@^7.2.0 @turf/helpers@^7.2.0
```

Import MapLibre via `react-map-gl/maplibre` only — **never** `mapbox-gl`.

## Shared Patterns

### Console API envelope + role headers
**Source:** `data-generator-console-web/src/api/client.ts` (lines 7–55)  
**Apply to:** all `geoAssets` JSON endpoints  
```typescript
function consoleRoleHeaders(): Record<string, string> {
  return { 'X-Console-Role': getConsoleRole() };
}
// apiRequest → JSON R envelope; apiFormRequest → multipart without Content-Type
```

### Conflict (409) with structured usages
**Source:** `ConsoleApiAdvice.java` lines 48–55 + `GeoAssetInUsePayload`  
**Apply to:** Geo assets delete UI (D-04)  

Backend already returns usages. **Gap:** `parseApiResult` / `apiRequest` throw `Error(message)` and **drop** `data` and status. Planner should add a small typed error (e.g. `ApiRequestError` with `status` + `data`) used by `deleteGeoAsset`, then:

```tsx
Modal.error / Modal.info({
  title: t('geoAssets.delete.inUseTitle'),
  content: /* map usages to "name (id)" list */,
});
```

### Honesty Alert
**Source:** Ant Design `Alert` usage in `SourceFieldsForm` / `ScenarioCatalogModal`  
**Apply to:** top of every `GeoMapPreview` region when preview content shown (D-09..D-11); `type="warning"`, `showIcon`, not closable while active.

### Fail-fast errors → toast
**Source:** Udfs/Schedules `onError: (err: Error) => message.error(err.message)`  
**Apply to:** list/upload/delete/preview mutations — consume `R.message` (and 409 Modal for delete).

### Java public API docs
**Source:** existing `ConsoleGeoAssetController` copyright + class/method Javadoc  
**Apply to:** any new public controller/service methods (preview endpoints).

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `GeoMapPreview.tsx` (MapLibre layers / OSM basemap / Turf guides) | component | transform | No map library or WebGL preview in console today — follow UI-SPEC + STACK.md; reuse only Alert/Card/lazy-chunk shell patterns |
| Raw `GET .../geojson` client helper | service | request-response | All existing clients assume `R` JSON envelope; need dedicated non-envelope fetch |

## Metadata

**Analog search scope:** `data-generator-console-web/src/{app,api,components,i18n}`, `data-generator-service/.../api/console`, `data-generator-service/.../geo`, `data-generator-geo`, `data-generator-common/.../model/v2`, `.planning/research/{STACK,ARCHITECTURE}.md`  
**Files scanned:** ~25 primary + locale key samples  
**Pattern extraction date:** 2026-08-06  
**CodeGraph:** not initialized under repo root — used Read/Grep/Glob only  
