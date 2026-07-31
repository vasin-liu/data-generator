# Architecture Research

**Domain:** Brownfield Template V2 / Spring Boot data-generator — v2.3 Geo Assets & Map Preview (GEO-05 + GEO-07)
**Researched:** 2026-07-31
**Confidence:** HIGH

> CodeGraph is not initialized under this repo root (no `.codegraph/`). Structural claims below come from direct source reads of v2.2 shipped geo paths, `SecretService` / `ConsoleSecretController`, `ConsoleUploadController`, and v2.3 planning artifacts (`PROJECT.md`, `PITFALLS.md`, `STACK.md`).

## Standard Architecture

### System Overview

v2.3 adds **operator-hosted GeoJSON assets** (metadata DB) and a **console map preview** on top of the v2.2 `geo_synthetic` / `geojson` runtime stack. It does **not** introduce a new Maven module. Work splits across:

- **Service layer** — persist assets, expose `/api/console/geo-assets`, wire resolver into runtime
- **Geo / Calcite layer** — extend path resolution (`classpath:` / filesystem unchanged; add `asset:`)
- **Console SPA** — asset CRUD UI + MapLibre preview panel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Presentation (data-generator-console-web)                                    │
│  /console/geo-assets  │  Map preview panel (assets + geo_synthetic config)   │
│  React Query → /api/console/geo-assets/**                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ Console REST (data-generator-service /api/console/**)                        │
│  ConsoleGeoAssetController  │  ConsoleUploadController (unchanged — ephemeral)│
│  ConsoleApiAdvice → R<T> envelope  │  ConsoleAuthorizationFilter (opt-in)   │
├─────────────────────────────────────────────────────────────────────────────┤
│ Application services                                                         │
│  GeoAssetService (CRUD + resolveUtf8)  │  AuditService (upload/delete)     │
│  GeoAssetPO + GeoAssetRepository (H2 metadata DB CLOB)                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Domain / execution (data-generator-calcite + data-generator-geo)             │
│  GeoSyntheticSourceFactory / GeoJsonSourceFactory (inject GeoAssetResolver)  │
│  GeoSyntheticRowSource → GeoSyntheticGenerator → GeoJsonLoader                 │
│  GeoResourceResolver: classpath: | filesystem | asset:{uuid}  ← bridge         │
├─────────────────────────────────────────────────────────────────────────────┤
│ Infrastructure                                                               │
│  H2 file metadata DB (same as secrets, templates, UDFs)                      │
│  GeoJsonLoader + JTS validation at ingest and runtime                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| `GeoAssetPO` | Persist one uploaded GeoJSON asset (id, name, CLOB body, bounds metadata, timestamps) | JPA `@Entity` on `geo_asset` table; mirror `SecretEntryPO` / `UdfArtifactPO` CLOB patterns |
| `GeoAssetRepository` | CRUD by asset UUID | Spring Data JPA, `findById(UUID)` |
| `GeoAssetService` | Upload validation, list summaries, get GeoJSON body, delete, runtime resolve | `@Service`; parse/validate via `GeoJsonLoader` before persist; implements `GeoAssetResolver` |
| `GeoAssetResolver` | Runtime lookup: asset-id → UTF-8 GeoJSON text | Interface in `data-generator-core` (like `SecretResolver`); service bean in `CoreConfig` |
| `ConsoleGeoAssetController` | Operator REST: multipart upload, list, get GeoJSON, delete, synthetic preview | `/api/console/geo-assets`; follow `ConsoleSecretController` + `ConsoleUdfController` multipart |
| `GeoResourceResolver` (extended) | Single resolution spine for all geo I/O | Existing static methods + `asset:` prefix delegates to `GeoAssetResolver` |
| `GeoSyntheticSourceVO` / `GeoJsonSourceVO` | Template YAML binding | Add optional `boundaryAssetId` / `networkAssetId` / `assetId` **or** encode as `asset:{uuid}` in existing `*Path` fields |
| `GeoSyntheticRequestMapper` | Map VO → `GeoGenerationRequest` | Mutual exclusion: path **or** asset-id per field; normalize to `asset:` location string |
| `GeoAssetsPage` + map panel | Browse assets, upload, preview synthetic config | Lazy-loaded MapLibre route; React Query cache |
| `ConsoleUploadController` | Ephemeral wizard file paths | **Unchanged** — not GEO-05 storage; do not conflate |

## Recommended Project Structure

No new top-level modules. Touch points:

```
data-generator-common/data-generator-core/
├── src/main/java/org/gensokyo/data/
│   ├── geo/GeoAssetResolver.java              # NEW — runtime resolve interface
│   └── model/v2/
│       ├── GeoSyntheticSourceVO.java          # MOD — boundaryAssetId / networkAssetId (or asset: in path)
│       └── GeoJsonSourceVO.java               # MOD — optional assetId / asset: path

data-generator-geo/
├── src/main/java/org/gensokyo/data/geo/io/
│   └── GeoResourceResolver.java               # MOD — asset: prefix + injectable resolver param

data-generator-calcite/
├── src/main/java/org/gensokyo/data/calcite/source/
│   ├── GeoSyntheticSourceFactory.java         # MOD — accept GeoAssetResolver
│   ├── GeoSyntheticRequestMapper.java         # MOD — asset-id fields + validation
│   ├── GeoJsonSourceFactory.java              # MOD — same resolver injection
│   └── GeoJsonRowSource.java                  # unchanged read path once location resolves

data-generator-service/
├── src/main/java/org/gensokyo/data/
│   ├── geo/GeoAssetService.java               # NEW
│   ├── model/po/GeoAssetPO.java               # NEW
│   ├── repository/GeoAssetRepository.java     # NEW
│   ├── api/console/ConsoleGeoAssetController.java  # NEW
│   └── config/CoreConfig.java                 # MOD — resolver bean + factory wiring
├── src/main/resources/
│   ├── application.yaml                       # MOD — multipart limits, geo-assets.* props
│   └── db/schema.sql                          # MOD — geo_asset DDL (lockstep with PO)
└── src/test/java/...                          # NEW — REST IT, asset-id pipeline IT

data-generator-console-web/
├── src/app/pages/GeoAssetsPage.tsx            # NEW
├── src/app/geo/GeoMapPreview.tsx              # NEW — lazy MapLibre
├── src/app/geo/geoAssetApi.ts                 # NEW — React Query client
├── src/app/App.tsx                            # MOD — route /console/geo-assets
└── package.json                               # MOD — maplibre-gl, react-map-gl, @turf/*

.planning/test-matrix.yaml                     # MOD — optional P1 row geo-assets (not P0)
docs/geo-synthetic-v2-source.md                # MOD — asset-id YAML examples
```

### Structure Rationale

- **core interface, service impl:** Keeps `data-generator-calcite` and `data-generator-geo` free of JPA; matches `SecretResolver` / `SecretService` split.
- **geo module resolver extension:** All GeoJSON reads already funnel through `GeoResourceResolver` → `GeoJsonLoader`; one extension point avoids preview-vs-run drift.
- **service owns HTTP + persistence:** Console controllers and metadata DB live here; no geo asset tables in calcite.
- **console-web owns visualization only:** Map rendering and Turf overlays stay client-side where possible; server serves authoritative GeoJSON bytes.

## Architectural Patterns

### Pattern 1: Metadata DB asset registry (mirror secrets + UDF bytes)

**What:** Operators upload GeoJSON once; platform assigns a stable UUID (`assetId`); templates reference `asset:{uuid}` or dedicated YAML fields. Bytes live in the H2 metadata DB (CLOB), not on ephemeral filesystem paths.

**When to use:** All GEO-05 hosted assets. Path refs (`classpath:`, absolute fixture paths) remain for ITs and dev fixtures only.

**Trade-offs:** Single backup surface with templates/secrets; H2 file DB grows with large uploads — mitigate with size/feature caps at ingest.

**Example (template YAML — dual reference shapes, one resolver):**
```yaml
sources:
  pts:
    type: geo_synthetic
    mode: BOUNDARY_POINTS
    count: 100
    boundaryAssetId: 550e8400-e29b-41d4-a716-446655440000   # preferred in console wizard
    # OR boundaryPath: asset:550e8400-e29b-41d4-a716-446655440000  # wire format for resolver
```

### Pattern 2: Single resolution spine (`GeoResourceResolver` + `GeoAssetResolver`)

**What:** Every geo read — runtime pipeline, map asset preview, synthetic boundary overlay — calls the same resolver chain:

```
location string
  → if classpath: …     → classloader (v2.2 unchanged)
  → if asset:{uuid} …   → GeoAssetResolver.resolveUtf8(uuid)
  → else                → filesystem path (v2.2 unchanged)
```

**When to use:** Always. Preview API must not duplicate DB lookup logic.

**Trade-offs:** Requires threading `GeoAssetResolver` into stateless factories (`GeoSyntheticSourceFactory`); follow `AiSourceFactory(ObjectProvider<…>)` pattern in `CoreConfig`.

**Example (conceptual wiring):**
```java
// CoreConfig — same pattern as aiSourceFactory
@Bean
V2SourceFactory geoSyntheticSourceFactory(ObjectProvider<GeoAssetResolver> assets) {
    return new GeoSyntheticSourceFactory(assets.getIfAvailable());
}

// GeoResourceResolver — geo module stays Spring-free
public static String readUtf8(String location, GeoAssetResolver assets) throws IOException {
    if (location.startsWith("asset:")) {
        return assets.resolveUtf8(location.substring("asset:".length()));
    }
    // existing classpath / file logic
}
```

### Pattern 3: Console REST envelope + list-without-payload

**What:** `ConsoleGeoAssetController` returns `R<T>`; list endpoints expose `GeoAssetSummary` (id, name, featureCount, bounds, updatedAt) **without** full GeoJSON body — mirror `SecretService.listSummaries()` / `ConsoleSecretController`.

**When to use:** All operator CRUD. Full GeoJSON only on `GET /{id}/geojson` (map + download).

**Trade-offs:** Extra round-trip for map load; acceptable for operator-scale asset counts.

### Pattern 4: Hybrid map preview (server GeoJSON + client synthetic overlay)

**What:**

| Preview need | Source | Rationale |
|--------------|--------|-----------|
| Uploaded asset geometry | **Server** `GET /api/console/geo-assets/{id}/geojson` | Authoritative bytes from DB; same resolver as runtime |
| `geo_synthetic` boundary/network underlay | **Server** — fetch linked asset GeoJSON or resolve path via preview helper | Must match runtime asset-id resolution |
| BBOX / CIRCLE mode overlay | **Client** — `@turf/bbox`, `@turf/circle` from template form state | No server round-trip; instant as operator edits fields |
| Synthetic point dots (optional) | **Server** `POST …/preview/synthetic` with capped `count` (e.g. ≤500) | Reuses `GeoSyntheticGenerator`; avoids shipping geo algo to browser |

**When to use:** GEO-07 equal depth — asset browse **and** template geo_synthetic config preview.

**Trade-offs:** Server synthetic preview adds API surface but guarantees parity with pipeline output; pure client generation would drift from JTS/Haversine semantics.

**Recommendation:** Ship server GeoJSON GET for assets + client Turf for BBOX/CIRCLE; add capped server synthetic preview endpoint for point dots (not client-side reimplementation of `GeoSyntheticGenerator`).

### Pattern 5: Harness tiering unchanged

**What:** GEO-05/07 proofs land as **P1** matrix rows (`geo-assets`); P0 gate stays at 15 rows per `PROJECT.md`.

**When to use:** All v2.3 verification linkage.

**Trade-offs:** Merge not blocked by map E2E flake; intentional.

## Data Flow

### Request Flow — asset upload (GEO-05)

```
Operator (console Geo Assets page)
    ↓ POST /api/console/geo-assets (multipart GeoJSON + optional name)
ConsoleAuthorizationFilter (skip if RBAC off)
    ↓
ConsoleGeoAssetController.upload
    ↓
GeoAssetService.create
    → read bytes, enforce max-bytes / max-feature-count (DataGeneratorProperties)
    → GeoJsonLoader parse + validate (JTS geometry sanity)
    → compute bounds summary metadata (for list API)
    → GeoAssetPO persist (CLOB + metadata_json)
    → AuditService.record(UPLOAD, GEO_ASSET, id)
    ↓
R.ok({ assetId, name, featureCount, bounds, updatedAt })   // no duplicate of full body in response body if large
```

### Request Flow — template run with asset-id (GEO-05 runtime)

```
Operator run (existing spine — unchanged queue/snap/runner)
    ↓
TemplateV2Runner
    ↓
GeoSyntheticSourceFactory.create(name, GeoSyntheticSourceVO)
    ↓
GeoSyntheticRowSource constructor
    → GeoSyntheticRequestMapper.toRequest (asset-id → asset: location)
    → GeoSyntheticGenerator.generateRows
        → GeoJsonLoader.loadFeature / loadGeometry
            → GeoResourceResolver.readUtf8(location, geoAssetResolver)
                → asset: → GeoAssetService.resolveUtf8 → DB CLOB
    ↓
Calcite rows → transform → sink
```

### Request Flow — map preview (GEO-07)

```
Asset browse tab:
  GET /api/console/geo-assets           → summaries
  GET /api/console/geo-assets/{id}/geojson → FeatureCollection for MapLibre source

geo_synthetic config preview (template editor side panel):
  BOUNDARY_POINTS / LINE_SAMPLE:
    resolve boundaryAssetId/networkAssetId → same GET geojson endpoint
  BBOX / CIRCLE:
    client Turf polygon/circle from form bbox[] / center[] / radiusMeters
  Point preview (optional):
    POST /api/console/geo-assets/preview/synthetic { GeoSyntheticSourceVO fragment, maxCount: 500 }
      → GeoSyntheticGenerator (shared code path) → GeoJSON FeatureCollection of points
    ↓
MapLibre GL layer (lazy-loaded component, OSM raster basemap)
```

### State Management

```
GeoAssetPO (H2 metadata DB)
    ↔ GeoAssetRepository
    ↔ GeoAssetService (transactional CRUD + resolve cache optional later)

Console SPA:
  TanStack React Query
    ↔ /api/console/geo-assets (list + detail geojson)
    ↔ local form state for geo_synthetic preview overlays (Turf)
```

### Key Data Flows

1. **Upload → DB → asset-id:** Multipart → validate → persist → console stores `assetId` in template YAML (never `ConsoleUploadController` absolute path).
2. **List → map:** Summaries for table UI; full GeoJSON fetched on row select / preview open.
3. **Template YAML → run:** `assetId` / `asset:` → `GeoAssetResolver` → same bytes as upload validation.
4. **Preview ↔ run parity:** Map underlay and synthetic preview endpoint must call `GeoResourceResolver` + `GeoSyntheticGenerator`, not ad-hoc parsers.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| Single-node / dev (default) | H2 file DB + inline CLOB; 16 MiB upload cap; no tile server |
| Tens of assets, district-scale GeoJSON | Current design sufficient; list API uses bounds metadata only |
| Hundreds of MB assets / many concurrent uploads | **Out of v2.3 scope** — would need filesystem spill + checksum metadata or external object store; do not silently expand H2 CLOB |

### Scaling Priorities

1. **First bottleneck:** H2 metadata DB bloat from unbounded uploads — enforce `data.generator.geo-assets.max-bytes` and `max-feature-count` at API gate.
2. **Second bottleneck:** Map bundle size — lazy-load MapLibre route; keep home/templates pages free of map chunk.

## Anti-Patterns

### Anti-Pattern 1: Reuse `ConsoleUploadController` for GEO-05

**What people do:** Point geo upload at `/api/console/uploads/file`, persist returned absolute path in template YAML.

**Why it's wrong:** Ephemeral `../uploaded-sources` paths; no asset-id; lost on redeploy; breaks distributed worker hosts; contradicts metadata DB decision in `PROJECT.md`.

**Do this instead:** Dedicated `ConsoleGeoAssetController` + DB persistence; templates store `assetId` or `asset:{uuid}`.

### Anti-Pattern 2: Dual resolution (preview vs runtime)

**What people do:** Map preview reads DB directly; pipeline still uses path-only `GeoResourceResolver`.

**Why it's wrong:** Asset-id works in UI but fails at run; v2.2 classpath ITs stay green while operator templates fail.

**Do this instead:** One resolver spine; preview endpoints and `GeoSyntheticRowSource` share `GeoResourceResolver.readUtf8(…, geoAssetResolver)`.

### Anti-Pattern 3: Collapse `geojson` and `geo_synthetic` types

**What people do:** Single “geo file” source type or factory for upload milestone.

**Why it's wrong:** v2.2 explicitly split read (`GeoJsonSourceFactory`) vs synthesize (`GeoSyntheticSourceFactory`); harness `geo-synthetic` row targets generation only.

**Do this instead:** Shared **asset registry**; separate source types unchanged. Both may **reference** the same asset-id for boundary/network/path fields.

### Anti-Pattern 4: Client-side full synthetic generation for preview

**What people do:** Reimplement boundary/line/circle algorithms in TypeScript for map preview.

**Why it's wrong:** Drifts from JTS/Haversine/seed semantics in `GeoSyntheticGenerator`; double maintenance.

**Do this instead:** Server capped preview endpoint for points; client Turf only for BBOX/CIRCLE **overlays** (visual bounds, not row generation).

### Anti-Pattern 5: P0 promotion of geo-assets

**What people do:** Add map Playwright to P0 merge gate.

**Why it's wrong:** Tile/network flake and UI timing inflate 15-row gate; contradicts v2.2/v2.3 P0 freeze.

**Do this instead:** P1 matrix row + `verify-console.ps1`; keep P0 at 15.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| OpenStreetMap raster tiles | URL template in MapLibre basemap | Preview only; attribution required; may fail air-gapped — map still renders GeoJSON layer |
| H2 metadata DB | Existing file DB + `db/schema.sql` DDL | Same backup as secrets/templates; watch CLOB growth |
| Podman / Playwright | `verify-console.ps1` after GEO-07 | Upload + map smoke spec |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| console-web ↔ service | `/api/console/geo-assets/**` JSON/multipart | Vite dev proxy `/api` → `:9876` unchanged |
| service ↔ geo module | `GeoJsonLoader` / `GeoResourceResolver` calls | No JPA in geo module |
| service ↔ calcite | `GeoAssetResolver` injected into `V2SourceFactory` beans | Tests without service use classpath paths only (`getIfAvailable()`) |
| `ConsoleUploadController` ↔ geo assets | **None** | Wizard file uploads for CSV/JSON sources remain separate |
| `SecretService` pattern ↔ `GeoAssetService` | Parallel CRUD/list/resolve | Secrets resolve credentials; geo assets resolve GeoJSON text — do not merge tables |
| Audit | `AuditService.record` on upload/delete | Mirror datasource/UDF governance visibility |

## New vs Modified (v2.3)

| Area | New | Modified |
|------|-----|----------|
| **GEO-05 persistence** | `GeoAssetPO`, `GeoAssetRepository`, `GeoAssetService`, `GeoAssetResolver`, `ConsoleGeoAssetController`, `geo_asset` DDL | `application.yaml` multipart + `data.generator.geo-assets.*` |
| **GEO-05 runtime** | Asset-id pipeline ITs | `GeoResourceResolver`, `GeoSyntheticSourceVO`, `GeoJsonSourceVO`, `GeoSyntheticRequestMapper`, `GeoSyntheticSourceFactory`, `GeoJsonSourceFactory`, `CoreConfig` |
| **GEO-07 console** | `GeoAssetsPage`, `GeoMapPreview`, map API client, Playwright spec | `App.tsx` route, nav/i18n, template editor geo preview hook |
| **GEO-07 preview API** | `GET …/{id}/geojson`, optional `POST …/preview/synthetic` | None on existing `/task` or template run APIs |
| **Docs / harness** | P1 row `geo-assets` (proposed) | `geo-synthetic-v2-source.md`, `geospatial-overview.md` |
| **Unchanged** | — | `ConsoleUploadController`, `ConsoleWebConfig` (SPA fallback only), `TemplateV2Runner` orchestration, P0 gate count |

## Suggested Build Order (Phases 21+)

Dependency-aware phasing aligned with `PITFALLS.md` and `PROJECT.md` equal-depth delivery:

```
Phase 21 — GEO-05 Backend (asset registry + runtime resolution)
Phase 22 — GEO-07 Console (map UI + preview APIs consumption)
Phase 23 — Docs, harness P1, milestone closeout
```

### Phase 21 — GEO-05: DB-backed assets + asset-id resolution

1. **Schema + model** — `GeoAssetPO`, repository, `db/schema.sql` entry, `DataGeneratorProperties` limits.
2. **`GeoAssetService`** — upload validate (size, feature count, `GeoJsonLoader`), CRUD, `resolveUtf8`, audit hooks.
3. **`GeoAssetResolver` interface + `CoreConfig` bean** — service implements interface.
4. **`ConsoleGeoAssetController`** — `POST` multipart upload, `GET` list, `GET /{id}/geojson`, `DELETE`; MockMvc ITs.
5. **Resolver bridge** — extend `GeoResourceResolver` for `asset:` prefix; inject into geo factories.
6. **VO + mapper** — `boundaryAssetId` / `networkAssetId` (and/or `assetId` on `GeoJsonSourceVO`); mutual exclusion validation in `GeoSyntheticRequestMapper`.
7. **Runtime ITs** — `TemplateV2Runner` with H2-stored fixture asset-id; regression: all v2.2 classpath path ITs unchanged.

*Verify:* Upload → list → template YAML with asset-id → run SUCCESS → rows; unknown asset-id fails at validate/run with clear error.

### Phase 22 — GEO-07: Console map + preview

1. **Frontend deps** — `maplibre-gl`, `react-map-gl/maplibre`, `@turf/bbox`, `@turf/circle` (lazy route).
2. **`GeoAssetsPage`** — upload form, asset table (summaries), map panel on selection.
3. **Template editor integration** — geo_synthetic / geojson source steps: asset picker (dropdown from list API), live preview side panel.
4. **Preview behavior** — asset layer via `GET /{id}/geojson`; BBOX/CIRCLE via Turf; optional synthetic points via `POST /preview/synthetic`.
5. **Playwright** — upload fixture, assert map layer visible, synthetic bbox overlay smoke.
6. **Run `verify-console.ps1`**.

*Verify:* Operator can upload, browse on map, configure `geo_synthetic` with asset-id, see boundary + bbox/circle preview without running full job.

### Phase 23 — Docs + harness

1. Update `geo-synthetic-v2-source.md` + `geospatial-overview.md` with asset-id YAML and console map pointers.
2. Add P1 matrix row `geo-assets` linked to service IT + optional Playwright.
3. Milestone audit; **do not** expand P0.

```
21a schema + GeoAssetService + REST        → verify: upload/list/get/delete IT
21b GeoResourceResolver + factory wiring   → verify: asset-id TemplateV2Runner IT
21c VO/mapper/validator                    → verify: v2.2 classpath ITs still green
22a map deps + GeoAssetsPage               → verify: npm run build
22b editor preview + synthetic overlay     → verify: Playwright smoke
22c verify-console.ps1                     → verify: full pipeline green
23  docs + P1 harness row                  → verify: p0.pass unchanged (15 rows)
```

## Sources

- `.planning/PROJECT.md` — v2.3 scope (GEO-05, GEO-07), metadata DB decision, P0 freeze
- `.planning/research/PITFALLS.md` — phase mapping, dual-resolution and upload anti-patterns
- `.planning/research/STACK.md` — MapLibre/react-map-gl, `asset:` convention, multipart limits
- `.planning/milestones/v2.2-REQUIREMENTS.md` — GEO-03 path-only baseline, deferred upload/map
- `docs/geo-synthetic-v2-source.md`, `docs/geospatial-overview.md` — v2.2 runtime shape
- Code: `SecretService`, `SecretEntryPO`, `ConsoleSecretController`, `ConsoleUploadController`, `GeoResourceResolver`, `GeoJsonLoader`, `GeoSyntheticSourceFactory`, `GeoSyntheticRequestMapper`, `GeoSyntheticRowSource`, `GeoJsonSourceVO`, `CoreConfig`, `ConsoleWebConfig`
- Harness: `.planning/test-matrix.yaml`, `scripts/verify-harness.ps1`, `scripts/verify-console.ps1`

---
*Architecture research for: data-generator v2.3 Geo Assets & Map Preview*
*Researched: 2026-07-31*
