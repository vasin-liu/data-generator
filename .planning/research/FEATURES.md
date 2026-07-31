# Feature Research

**Domain:** GeoJSON asset library + Template V2 binding + console map preview (milestone v2.3)
**Researched:** 2026-07-31
**Confidence:** HIGH

## Feature Landscape

v2.3 extends the **shipped v2.2 geo stack** (`geo_synthetic` four modes, path/`classpath:` assets via `GeoResourceResolver`, read-only `geojson` source, P1 `geo-synthetic` harness row). Operators today upload GeoJSON through `ConsoleUploadController` and paste absolute filesystem paths into templates — durable, referencable assets and visual confirmation are missing. GEO-05 and GEO-07 ship at **equal depth**; GEO-06 polygon synthesis, DATA-01 common-data CRUD, and P0 gate inflation stay out.

### Already Shipped (Do Not Rebuild)

| Capability | Where it lives | v2.2 proof bar |
|------------|----------------|----------------|
| Four-mode `geo_synthetic` source | `GeoSyntheticSourceVO`, `GeoSyntheticRowSource`, `GeoSyntheticGenerator` | `TemplateV2RunnerGeoSyntheticSourceTests` (boundary, line, bbox, circle) |
| Path/`classpath:` GeoJSON resolution | `GeoResourceResolver.readUtf8` | GEO-03; calcite fixtures under `src/test/resources/geo/` |
| Read-only `geojson` source | `GeoJsonSourceVO`, `GeoJsonRowSource` | Unchanged behavior; `SourceFieldsForm` editor kind |
| Ephemeral file upload for template paths | `ConsoleUploadController` → `../uploaded-sources/` | Returns absolute path string; not metadata DB |
| UDF/datasource governance patterns | `ConsoleUdfController`, `DataSourceConfigService`, `AuditService`, `ConsoleSecurityProperties` | JDBC persistence, audit, optional header RBAC (default off) |
| Console template editor (non-geo_synthetic) | `SourceFieldsForm`, `draftUtils` `EDITABLE_SOURCE_KINDS` | `geojson` supported; **`geo_synthetic` not in editor yet** |

---

## 1. Asset Library CRUD

### Table Stakes (Users Expect These)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Multipart GeoJSON upload | Operators already upload via `/api/console/uploads/file`; expect hosted assets to survive restart and be shareable across templates | MEDIUM | Mirror UDF multipart + metadata DB (H2 file metadata like secrets/templates). Store bytes or filesystem path keyed by stable **asset-id** (snowflake/UUID). Reject empty files. |
| List + get by asset-id | Template editor and map need a picker; operators need to find prior uploads | LOW–MEDIUM | Paginated list with name, size, uploadedAt, optional bbox/featureCount. GET returns GeoJSON body or signed download URL for map layer. |
| Delete | Unused assets accumulate; operators expect cleanup | LOW | Hard delete or soft-delete; **must** check template references or return 409 with usage list. Audit on delete. |
| Basic metadata | Browse without opening every file | LOW | filename, contentLength, uploadedAt, actor; derive bbox + feature count at ingest via existing `GeoJsonLoader`. |
| Max file size limit | GeoJSON can be huge; platform already caps script/UDF sizes | LOW | `data.generator.*` property (e.g. `geo-assets.max-bytes`); enforce at upload; clear 413/400 message. |
| GeoJSON validation on upload | Invalid JSON breaks runs at pipeline time | MEDIUM | Parse with `GeoJsonLoader.loadFeatureCollection`; accept Feature or FeatureCollection roots only. Fail fast with line/column hint if possible. |

### Differentiators (Competitive Advantage)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Metadata DB + asset-id (not raw paths) | Synthetic-data platforms usually expect operators to manage files out-of-band; first-class assets align with secrets/datasource governance story | MEDIUM | Same trust posture as JDBC catalog and UDF registry — reproducible templates portable across hosts. |
| Derived spatial metadata at ingest | Speeds browse/preview without client-side parse | LOW | bbox, featureCount, geometry type summary — unusual for lightweight ETL tools. |
| Referential delete guard | Prevents silent template breakage | MEDIUM | Scan template JSON for `assetId` / `asset:` refs before delete — operator-credible vs filesystem orphans. |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Full GIS asset management (CRS reprojection, topology repair, versioning) | “Make it like QGIS/ArcGIS” | Scope explosion; JTS/GeoTools already bounded in `data-generator-geo` | Validate + store; document CRS assumptions; defer reprojection |
| Shapefile / GeoPackage / KML import | Broader format support | New parsers, licensing, UI complexity | GeoJSON-only v2.3; convert upstream |
| Unlimited upload size | “My boundary file is 500 MB” | OOM, slow map preview, metadata DB bloat | Configurable cap + documented split/chunk guidance |
| Public unauthenticated asset URLs | Easy map tile fetch | Security leak for operator boundaries | Authenticated console API; map fetches with session/proxy |
| Replace `ConsoleUploadController` entirely | One upload path | Breaks csv/json/excel path-based sources that rely on ephemeral upload | Keep path upload for non-geo sources; geo assets use dedicated API |

---

## 2. Template Binding (assetId on sources; path fallback)

### Table Stakes (Users Expect These)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| `assetId` (or `asset:` URI) on `geo_synthetic` boundary/network | GEO-05 requirement; path-only is v2.2 stopgap | MEDIUM | Extend `GeoSyntheticSourceVO` with `boundaryAssetId` / `networkAssetId` (or unified `asset:` prefix in path fields). Map in `GeoSyntheticRequestMapper`. |
| `assetId` on `geojson` source | Same asset library should serve read-only geo sources | LOW–MEDIUM | Parallel field on `GeoJsonSourceVO`; resolve in `GeoJsonRowSource` before `GeoResourceResolver`. |
| Path/`classpath:` fallback | Fixtures, CI, local dev — explicitly preserved in PROJECT.md | LOW | Resolver order: if assetId set → DB; else existing `GeoResourceResolver`. Never break v2.2 YAML. |
| Runtime resolution in calcite module | Runs must work on worker JVM without console | MEDIUM | New `GeoAssetResolver` in `data-generator-geo` or service layer injected into RowSource factories; workers need same metadata DB or shared storage. |
| Template validation: unknown asset-id | Fail before run, like unknown datasource | LOW–MEDIUM | Validate on save/publish in `TemplateEditorService` or lifecycle hook; message includes source name + field. |
| Console asset picker | Manual UUID entry is error-prone | MEDIUM | Dropdown/search in source form; part of GEO-07 equal depth (requires `geo_synthetic` added to `EDITABLE_SOURCE_KINDS`). |

### Differentiators (Competitive Advantage)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Dual reference model (asset-id + path) | Dev/prod parity: fixtures on classpath, production on hosted assets | LOW | Documented in geo docs; competitors often pick one model only. |
| `asset:` prefix convention | Consistent with `snap:` datasource snapshots | LOW | Optional sugar: `boundaryPath: asset:12345` vs separate field — pick one shape in design, not both. |
| Cross-source asset reuse | One district boundary for synthetic points and read-only geojson transform | LOW | Single library serves `geo_synthetic` + `geojson` + future GEO-06. |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Asset-id only (drop path support) | “Simplify resolver” | Breaks v2.2 templates, calcite IT fixtures, classpath tests | Keep path fallback indefinitely for fixtures |
| Inline GeoJSON in template YAML | Self-contained templates | Bloats template rows, governance/audit noise | asset-id reference |
| Auto-rewrite templates on upload | Magic path → asset migration | Silent mutation, audit confusion | Optional CLI/migration tool later; not v2.3 |
| Embedding asset bytes in run snapshot | Fully self-contained runs | Duplicates storage; large snapshot rows | Resolve asset at run time from metadata DB; hash in run lineage optional P1 |

---

## 3. Map Preview UX

### Table Stakes (Users Expect These)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Asset library browse with map render | GEO-07 core; operators must see what they uploaded | MEDIUM–HIGH | New console page or drawer: list assets, select → map renders GeoJSON layer. Leaflet/MapLibre (OSM tiles) fits existing React/Ant Design stack without backend map server. |
| Fit-to-bounds on load | GeoJSON extent varies wildly | LOW | Compute from ingest bbox or client-side L.geoJSON bounds. |
| `geo_synthetic` config preview overlay | Operators configuring boundary/line/bbox/circle need visual confirmation | MEDIUM–HIGH | Parse draft source config; overlay polygon/line from asset, draw bbox rect or circle from YAML arrays. **Does not** require pipeline run. |
| Entry from template editor | Preview should be one click from source step | LOW–MEDIUM | “Preview on map” on geo_synthetic/geojson fields; deep-link with draft state or selected assetId. |
| Read-only preview | Scope control for v2.3 | LOW | No geometry editing on map (editing → GEO-06 / future). |

### Differentiators (Competitive Advantage)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Mode-aware synthetic overlay | Shows boundary vs line vs bbox vs circle differently | MEDIUM | Color/style per mode; labels for count/seed — rare in data-gen tools. |
| Seed + count display on preview | Reinforces v2.2 reproducibility story | LOW | Static annotation (“seed 42, 100 points”) without simulating all points on map. |
| Asset + config on same map | Upload boundary, immediately preview synthetic sampling intent | MEDIUM | Tight loop: upload → pick asset → tune geo_synthetic → see overlay — product UX win. |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Live animated point generation on map | “Show me the 100 points” | Client-side reimplementation of `GeoSyntheticGenerator`; seed parity risk | Static overlay + optional small sample (≤10) if needed |
| Full pipeline run preview on map | See actual job output | Conflates job center with config preview; needs run completion | Keep job output in existing job/report UX |
| Satellite/enterprise basemap dependency | Prettier maps | API keys, licensing, offline/air-gap failure | OSM/MapLibre default; optional tile URL config later |
| Map-based geometry drawing/editing | “Draw my boundary in UI” | GIS editor scope; overlaps GEO-06 | Upload GeoJSON externally; preview only |
| 3D / deck.gl for v2.3 | Visual wow | Heavy bundle, low operator value for point synthesis | 2D Leaflet/MapLibre sufficient |

---

## 4. Governance

### Table Stakes (Users Expect These)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Max upload size enforcement | Platform stability | LOW | Property-driven; mirror UDF/script limit patterns in `DataGeneratorProperties`. |
| GeoJSON structural validation | Prevent poison assets | MEDIUM | Shared validator used by upload API and optional template validate. |
| Audit events for upload/delete | Operators expect traceability (UDF/datasource precedent) | LOW | `AuditService.record` — e.g. `GEO_ASSET_UPLOAD`, `GEO_ASSET_DELETE`; detail: filename, size, assetId; **no** full GeoJSON body in audit JSON. |
| Filename sanitization | Path traversal via original filename | LOW | Reuse `ConsoleUploadController.sanitizeFileName` pattern. |
| RBAC when console security enabled | v2.1 shipped enable path; geo assets are operator data | LOW–MEDIUM | When `data.generator.console-security.enabled=true`, restrict upload/delete to editor/admin roles; read/list for viewer. Default **off** — do not flip. |

### Differentiators (Competitive Advantage)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Governance aligned with UDF/secrets | Single operator-trust model across artifact types | LOW | Same metadata DB, audit page, optional RBAC — differentiated vs ad-hoc file drops. |
| Delete with template reference check | Safer than filesystem unlink | MEDIUM | Surfaces which templates block deletion. |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Mandatory RBAC for geo assets | Security hardening | Breaks local dev; contradicts SEC-02 deferral | Opt-in via existing console security flag |
| Approval workflow before asset use | Enterprise governance | Blocks GEO-05/07 equal-depth delivery | Audit + RBAC; approval queue → future milestone |
| Antivirus scanning on upload | Enterprise security | Infra dependency, slow uploads | Size + validation limits; defer AV integration |
| Promote geo-assets to P0 harness row | Coverage parity | PROJECT.md explicitly freezes P0 at 15 | P1 row + `verify-console` / IT slice when stable |
| Store GeoJSON in audit detail | Forensics | Huge audit rows, PII in geometries | Store assetId + hash only |

---

## Feature Dependencies

```
v2.2 GeoSyntheticGenerator + GeoResourceResolver (shipped)
    └──requires──> GEO-05 asset storage + upload/list/get/delete API
                       └──requires──> GeoAssetResolver (assetId → bytes)
                            └──requires──> VO fields + mapper + RowSource wiring
                                 └──enhances──> Template validation (unknown assetId)

GEO-05 asset read API
    └──requires──> GEO-07 map asset browse/render
    └──requires──> Console asset picker (geo_synthetic editor)

GEO-07 map preview (synthetic overlay)
    └──requires──> GEO-05 for boundary/line assets
    └──requires──> geo_synthetic in console editor (not shipped in v2.2)
    └──enhances──> Operator trust in v2.2 four-mode config

ConsoleUploadController path upload (shipped)
    └──conflicts──> Replacing all uploads with asset-id-only model
    └──enhances──> Non-geo file sources continue unchanged

AuditService + ConsoleSecurityProperties (shipped)
    └──enhances──> GEO-05 governance (audit + optional RBAC)

P0 harness gate (15 rows, shipped)
    └──conflicts──> P0 promotion of geo-assets / map E2E
    └──enhances──> Optional P1 row after v2.3 proof stable

GEO-06 polygon synthesis (deferred)
    └──conflicts──> Map geometry editing in v2.3
    └──enhances──> Future map preview for polygon output modes

DATA-01 common-data CRUD (deferred)
    └──conflicts──> General reference-data library in same milestone
```

### Dependency Notes

- **GEO-07 requires GEO-05 read path:** Map cannot browse hosted assets without list/get API and stored GeoJSON; equal depth means upload API and map ship together, not map-first on paths only.
- **Template binding requires GeoAssetResolver in execute path:** Worker/coordinator JVMs must resolve asset-id the same way as console preview (shared metadata DB + blob store).
- **Console `geo_synthetic` editor is a GEO-07 dependency:** v2.2 shipped backend-only; v2.3 map preview without editor leaves YAML-only operators — violates equal depth intent.
- **Path fallback conflicts with asset-only breaking change:** Preserve GEO-03 semantics for fixtures and v2.2 templates.
- **Governance enhances GEO-05 but should not block MVP:** Size limit + validation + audit are table stakes; RBAC is conditional on existing flag.
- **P0 inflation conflicts with milestone charter:** Keep merge gate at 15; add P1 or console IT linkage if needed.

---

## MVP Definition

### Launch With (v2.3 — GEO-05 + GEO-07 equal depth)

- [ ] GeoJSON asset upload + metadata DB persistence + stable asset-id — GEO-05 core
- [ ] List / get / delete API with metadata (size, bbox, feature count) — GEO-05 core
- [ ] `assetId` resolution on `geo_synthetic` (boundary/network) and `geojson` with path fallback — GEO-05 binding
- [ ] Console asset library browse + map render — GEO-07 core
- [ ] `geo_synthetic` config map overlay (four modes) + seed/count annotation — GEO-07 core
- [ ] `geo_synthetic` source kind in template editor with asset picker — GEO-07 / equal depth
- [ ] Max file size + GeoJSON validation + audit on upload/delete — governance table stakes
- [ ] Embedded-first IT: upload → resolve in RowSource → pipeline row count smoke — trust proof

### Add After Validation (v2.3.x / early v2.4 if capacity)

- [ ] Referential delete guard across all stored templates — when template scan is complete
- [ ] Optional P1 harness row `geo-assets` — when IT stable; **not** P0
- [ ] Run lineage asset hash in `RunLineageSupport` — reproducibility audit trail
- [ ] Playwright map smoke in `verify-console.ps1` — when map bundle size acceptable

### Future Consideration (explicitly out of v2.3)

- [ ] GEO-06 polygon / MultiPolygon synthetic generation + map output preview
- [ ] DATA-01 operator common-data / code-table CRUD
- [ ] Shapefile/GeoPackage import, CRS reprojection, map geometry editing
- [ ] Asset versioning, folders, approval workflows
- [ ] P0 promotion of geo-assets proofs

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Asset upload + DB + asset-id (GEO-05) | HIGH | MEDIUM | P1 |
| assetId binding + path fallback (geo_synthetic + geojson) | HIGH | MEDIUM | P1 |
| Map asset browse + render (GEO-07) | HIGH | MEDIUM–HIGH | P1 |
| geo_synthetic map config overlay (GEO-07) | HIGH | MEDIUM–HIGH | P1 |
| geo_synthetic console editor + asset picker | HIGH | MEDIUM | P1 |
| GeoJSON validation + max size + audit | HIGH (trust) | LOW–MEDIUM | P1 |
| RBAC when security enabled | MEDIUM | LOW | P1 (conditional) |
| Referential delete guard | MEDIUM | MEDIUM | P2 |
| P1 harness row `geo-assets` | MEDIUM (trust) | LOW | P2 |
| Run lineage asset hash | LOW–MEDIUM | LOW | P2 |
| Map geometry editing | HIGH later | HIGH | P3 (GEO-06 lane) |
| DATA-01 common-data library | HIGH later | HIGH | P3 (deferred) |
| P0 geo-assets promotion | LOW now | MEDIUM (CI cost) | P3 (anti-feature) |
| Full GIS asset management | LOW now | HIGH | P3 (anti-feature) |

**Priority key:**
- P1: Must have for v2.3 launch (GEO-05 + GEO-07 equal depth)
- P2: Should have in v2.3 when capacity allows
- P3: Explicitly deferred or anti-feature for this milestone

---

## Competitor Feature Analysis

Compared to **synthetic-data / ETL platforms** and **lightweight GIS tooling** — v2.3 competes on operator trust inside Template V2, not on full GIS suite breadth.

| Feature | Typical synthetic-data / Faker tools | Typical ETL (NiFi, Airbyte-like) | GIS web apps (GeoServer UI, Felt, etc.) | Our approach (v2.3) |
|---------|--------------------------------------|----------------------------------|----------------------------------------|---------------------|
| GeoJSON storage | None; user supplies paths | External blob (S3) + URL ref | Full layer catalog | Metadata DB + asset-id in-platform |
| Template binding | N/A | Connection/object refs | Layer ID in app config | `assetId` on V2 sources + path fallback |
| Map preview | Rare | Rare for config | Core product | Config/asset preview only; no edit |
| Reproducible geo synthesis | Uncommon | N/A | N/A | v2.2 seed modes + seed display on preview |
| Governance | N/A | RBAC on connections | Workspace ACL | Reuse console audit + optional header RBAC |
| Harness gate | Ad-hoc | CI on connectors | N/A | P1 optional; **P0 frozen at 15** |

---

## Sources

- `.planning/PROJECT.md` — v2.3 milestone goals, equal depth, out-of-scope (GEO-06, DATA-01, P0 freeze)
- `.planning/milestones/v2.2-REQUIREMENTS.md` — GEO-05..07 deferred requirements; GEO-01..03 shipped
- `.planning/milestones/v2.2-phases/19-v2-geo-synthetic-source/19-CONTEXT.md` — path-only assets, GeoResourceResolver
- `docs/superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md` — asset upload explicitly deferred from v2.2
- Code: `GeoResourceResolver`, `GeoSyntheticSourceVO`, `ConsoleUploadController`, `ConsoleUdfController`, `AuditService`, `SourceFieldsForm`, `draftUtils` (`geojson` only in editor)
- `.planning/test-matrix.yaml` — P0 15-row gate; P1 `geo-synthetic` row
- `docs/staging-console-rbac.md` — RBAC enable path (default off)

---
*Feature research for: data-generator v2.3 Geo Assets & Map Preview*
*Researched: 2026-07-31*
