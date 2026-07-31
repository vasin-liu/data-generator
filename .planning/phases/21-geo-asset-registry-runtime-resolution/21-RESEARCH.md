# Phase 21 Research: Geo Asset Registry + Runtime Resolution

**Researched:** 2026-07-31  
**Domain:** Brownfield Template V2 — metadata DB GeoJSON assets, `asset:{uuid}` runtime resolution, console REST (no map UI)  
**Confidence:** HIGH  
**CodeGraph:** not indexed at repo root — structural claims verified via direct reads of v2.2 geo paths, `SecretResolver`/`SecretService`, `ConsoleUdfController`, `UdfArtifactPO`, `GeoResourceResolver`, `GeoSyntheticRequestMapper`, `CoreConfig`, `TemplatePO`  
**Requirements:** GEO-05, GEO-08, GEO-09, GEO-10, GEO-11, GOV-01 (Phase 22 map UI deferred)

---

## User Constraints (LOCKED — from `21-CONTEXT.md`)

| ID | Decision |
|----|----------|
| **D-01** | Dedicated fields: `boundaryAssetId` / `networkAssetId` on `geo_synthetic`, `assetId` on `geojson`; mappers normalize to `asset:{uuid}` |
| **D-02** | Path + asset-id both set → **asset-id wins**; path ignored with clear validation message (fail-fast preferred) |
| **D-03** | Wire format `boundaryPath: asset:{uuid}` MUST resolve — same spine as dedicated fields |
| **D-04** | CLOB in metadata DB table `geo_asset`; no filesystem spill |
| **D-05** | New `ConsoleGeoAssetController` at `/api/console/geo-assets` — do **not** reuse `ConsoleUploadController` |
| **D-06** | Limits via `data.generator.geo-assets.*`; raise Spring multipart defaults; reject oversize/over-feature before persist (400/413-style `R.fail`) |
| **D-07** | Feature / FeatureCollection roots only; validate via `GeoJsonLoader`/JTS at ingest; store bbox + featureCount (+ optional geometry summary) |
| **D-08** | Hard delete; scan templates → **409** with usage hints if referenced |
| **D-09** | `GeoAssetResolver` (core interface) + `GeoAssetService` (service); inject into geo execute path; missing asset → `IllegalArgumentException` naming id |
| **D-10** | `GET /{id}/geojson` returns authoritative body for Phase 22 map |
| **D-11** | Audit upload + delete via `AuditService`; RBAC only when existing console-security flag enabled |

**Out of scope:** Console map / MapLibre / `geo_synthetic` editor (Phase 22); docs/harness row (Phase 23).

---

## Summary

Phase 21 adds a **durable GeoJSON asset registry** in the H2 metadata DB and extends the existing **`GeoResourceResolver` → `GeoJsonLoader` spine** with an `asset:{uuid}` branch. No new Maven module. Pattern mirrors **`SecretResolver` / `SecretService`**: interface in `data-generator-core`, JPA + REST in `data-generator-service`, optional collaborator injected into calcite source factories via **`CoreConfig` + `ObjectProvider`** (same as `AiSourceFactory`).

### Primary recommendation

1. **Persist** validated GeoJSON as CLOB in `geo_asset` (`GeoAssetPO` + `GeoAssetRepository`).
2. **Expose** multipart upload + list/get/delete under `/api/console/geo-assets`.
3. **Extend** `GeoResourceResolver.readUtf8(location, GeoAssetResolver)` — geo module stays Spring-free.
4. **Thread** resolver through `GeoJsonLoader` → `GeoSyntheticGenerator` / `GeoJsonRowSource` via factory/row-source constructors.
5. **Normalize** dedicated asset-id VO fields to `asset:{uuid}` in `GeoSyntheticRequestMapper` (+ small mapper for `geojson`).
6. **Scan** `template.content_json` / `content_yaml` on delete for references → `409 CONFLICT`.

---

## 1. Exact Files to Create / Modify

### 1.1 CREATE — `data-generator-common/data-generator-core`

| File | Purpose |
|------|---------|
| `src/main/java/org/gensokyo/data/geo/GeoAssetResolver.java` | Runtime interface: `String resolveUtf8(String assetId)`; throws `IllegalArgumentException` when unknown (mirror `SecretResolver.resolveRequired`) |

### 1.2 CREATE — `data-generator-service`

| File | Purpose |
|------|---------|
| `src/main/java/org/gensokyo/data/model/po/GeoAssetPO.java` | JPA entity `@Table(name = "geo_asset")` — CLOB body + ingest metadata |
| `src/main/java/org/gensokyo/data/repository/GeoAssetRepository.java` | `JpaRepository<GeoAssetPO, UUID>` |
| `src/main/java/org/gensokyo/data/geo/GeoAssetService.java` | CRUD, upload validation, `implements GeoAssetResolver`, delete with reference scan |
| `src/main/java/org/gensokyo/data/geo/GeoAssetInUseException.java` | Carries `List<GeoAssetTemplateUsage>` for 409 response |
| `src/main/java/org/gensokyo/data/geo/GeoAssetReferenceScanner.java` | Finds template references to an asset id (JSON/YAML walk) |
| `src/main/java/org/gensokyo/data/geo/GeoAssetIngestSupport.java` | Parse/validate via `GeoJsonLoader`, compute bbox/featureCount/geometry summary, enforce limits |
| `src/main/java/org/gensokyo/data/api/console/ConsoleGeoAssetController.java` | REST surface `/api/console/geo-assets` |
| `src/main/java/org/gensokyo/data/api/console/dto/GeoAssetSummaryView.java` | List DTO (no body) |
| `src/main/java/org/gensokyo/data/api/console/dto/GeoAssetUploadView.java` | Post-upload summary + id |
| `src/main/java/org/gensokyo/data/api/console/dto/GeoAssetTemplateUsageView.java` | `{ templateId, templateName }` for 409 payload |
| `src/main/resources/db/schema.sql` | **Add** `geo_asset` DDL if file is maintained in-repo (see §2 note) |

**Tests (service module):**

| File | Purpose |
|------|---------|
| `src/test/java/org/gensokyo/data/geo/GeoAssetServiceTests.java` | Upload validation, limits, resolve |
| `src/test/java/org/gensokyo/data/geo/GeoAssetReferenceScannerTests.java` | Reference detection unit tests |
| `src/test/java/org/gensokyo/data/api/console/ConsoleGeoAssetControllerIT.java` | REST IT on embedded H2 (`application-phase7-test.yaml`) |

### 1.3 MODIFY — `data-generator-common/data-generator-core`

| File | Change |
|------|--------|
| `src/main/java/org/gensokyo/data/model/v2/GeoSyntheticSourceVO.java` | Add `boundaryAssetId`, `networkAssetId` (UUID strings); Javadoc: normalized to `asset:` at runtime |
| `src/main/java/org/gensokyo/data/model/v2/GeoJsonSourceVO.java` | Add `assetId`; keep `path` for classpath/filesystem |

### 1.4 MODIFY — `data-generator-geo`

| File | Change |
|------|--------|
| `src/main/java/org/gensokyo/data/geo/io/GeoResourceResolver.java` | Add `ASSET_PREFIX = "asset:"`; overload `readUtf8(String location, GeoAssetResolver assets)` |
| `src/main/java/org/gensokyo/data/geo/io/GeoJsonLoader.java` | Add resolver-param overloads for `loadFeatureCollection`, `loadGeometry`, `loadFeature` |
| `src/main/java/org/gensokyo/data/geo/GeoSyntheticGenerator.java` | Overload `generateRows(request, assets)`; pass resolver into loader calls |

**Tests:**

| File | Change |
|------|--------|
| `src/test/java/org/gensokyo/data/geo/io/GeoJsonLoaderTests.java` | `asset:` resolution with stub resolver |
| New `GeoResourceResolverTests.java` (optional) | classpath / file / asset branches |

### 1.5 MODIFY — `data-generator-calcite`

| File | Change |
|------|--------|
| `src/main/java/org/gensokyo/data/calcite/source/GeoSyntheticRequestMapper.java` | Asset-id → `asset:{uuid}`; dual path+asset → IAE; update `enforceModePaths` |
| New `GeoJsonLocationMapper.java` (optional) | `resolvePath(GeoJsonSourceVO)` → single location string |
| `src/main/java/org/gensokyo/data/calcite/source/GeoSyntheticSourceFactory.java` | Constructor `GeoSyntheticSourceFactory(GeoAssetResolver assets)` |
| `src/main/java/org/gensokyo/data/calcite/source/GeoJsonSourceFactory.java` | Same resolver injection |
| `src/main/java/org/gensokyo/data/calcite/source/GeoSyntheticRowSource.java` | Hold `GeoAssetResolver`; pass to generator |
| `src/main/java/org/gensokyo/data/calcite/source/GeoJsonRowSource.java` | Resolve location via mapper; pass resolver to loader |

**Tests:**

| File | Change |
|------|--------|
| `src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRequestMapperTests.java` | asset-id normalization, dual-field errors, `asset:` passthrough |
| New `GeoJsonLocationMapperTests.java` | geojson asset-id vs path |
| New `TemplateV2RunnerGeoAssetSourceTests.java` | Pipeline IT with `asset:{uuid}` |

### 1.6 MODIFY — `data-generator-service` (config / advice)

| File | Change |
|------|--------|
| `src/main/java/org/gensokyo/data/config/DataGeneratorProperties.java` | Nested `GeoAssets geoAssets` with `maxBytes`, `maxFeatures` |
| `src/main/java/org/gensokyo/data/config/CoreConfig.java` | `@Bean GeoAssetResolver`; wire factories with `ObjectProvider<GeoAssetResolver>` |
| `src/main/java/org/gensokyo/data/api/console/ConsoleApiAdvice.java` | `@ExceptionHandler(GeoAssetInUseException.class)` → `409 CONFLICT` |
| `src/main/resources/application.yaml` | `spring.servlet.multipart.*`; `data.generator.geo-assets.*` |

### 1.7 DO NOT MODIFY

| File | Reason |
|------|--------|
| `ConsoleUploadController` | Ephemeral paths — D-05 |
| `data-generator-console-web/**` | Phase 22 |
| `.planning/test-matrix.yaml` P0 rows | TEST-11 Phase 23; P0 stays 15 |

---

## 2. Schema Sketch — `geo_asset`

Mirror `UdfArtifactPO` CLOB pattern and UUID identity (assigned at upload).

```sql
CREATE TABLE IF NOT EXISTS geo_asset (
    id               UUID         NOT NULL PRIMARY KEY,
    name             VARCHAR(256) NOT NULL,
    content_type     VARCHAR(64)  NOT NULL DEFAULT 'application/geo+json',
    geojson_clob     CLOB         NOT NULL,
    feature_count    INTEGER      NOT NULL,
    min_lon          DOUBLE       NOT NULL,
    min_lat          DOUBLE       NOT NULL,
    max_lon          DOUBLE       NOT NULL,
    max_lat          DOUBLE       NOT NULL,
    geometry_summary VARCHAR(512),
    content_sha256   VARCHAR(64),
    uploaded_by      VARCHAR(128),
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_geo_asset_updated_at ON geo_asset (updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_geo_asset_name ON geo_asset (name);
```

**JPA:** `@Column(columnDefinition = "CLOB", name = "geojson_clob")`; bbox from JTS envelope at ingest.

**DDL path:** Hibernate entity DDL is primary (`UdfArtifactPO` pattern); `application.yaml` references `classpath:db/schema.sql` — add DDL if maintained. [Gap: `schema.sql` not in workspace snapshot.]

**Defaults:** `max-bytes: 52428800` (50 MiB), `max-features: 10000`; multipart `55MB` / `56MB`.

---

## 3. API Sketch — `/api/console/geo-assets`

Pattern: `ConsoleUdfController` multipart + `R<T>`; list without body like `ConsoleSecretController`.

| Method | Path | Response | Notes |
|--------|------|----------|-------|
| `POST` | `/api/console/geo-assets` | `R<GeoAssetUploadView>` | multipart `file`, optional `name`; audit `GEO_ASSET_UPLOAD` |
| `GET` | `/api/console/geo-assets` | `R<List<GeoAssetSummaryView>>` | No geojson body |
| `GET` | `/api/console/geo-assets/{id}` | `R<GeoAssetSummaryView>` | Metadata only |
| `GET` | `/api/console/geo-assets/{id}/geojson` | Raw `application/geo+json` (preferred) | D-10 Phase 22 map |
| `DELETE` | `/api/console/geo-assets/{id}` | `R<String>` or `409` | Reference scan; audit `GEO_ASSET_DELETE` |

**409 payload:** `R.fail(msg, GeoAssetInUsePayload(List<GeoAssetTemplateUsageView>))` via new `ConsoleApiAdvice` handler.

---

## 4. Inject `GeoAssetResolver` without Spring in geo module

**Placement:** interface in `data-generator-core`; `GeoAssetService` in service; geo module imports interface only.

**Spine:**

```java
// GeoResourceResolver (Spring-free)
if (trimmed.startsWith("asset:")) {
    return assets.resolveUtf8(trimmed.substring("asset:".length()).strip());
}
// classpath: + filesystem unchanged
```

**Threading:** `GeoJsonLoader` overloads → `GeoSyntheticGenerator.generateRows(request, assets)` → row sources from factories.

**CoreConfig (mirror `AiSourceFactory`):**

```java
@Bean
V2SourceFactory geoSyntheticSourceFactory(ObjectProvider<GeoAssetResolver> geoAssets) {
    return new GeoSyntheticSourceFactory(geoAssets.getIfAvailable());
}
@Bean GeoAssetResolver geoAssetResolver(GeoAssetService s) { return s; }
```

**Mapper (`GeoSyntheticRequestMapper`):** `boundaryAssetId`/`networkAssetId` → `asset:{uuid}`; `asset:` in path passthrough (D-03); both path + asset-id → IAE naming source + field (D-02). Same for `GeoJsonSourceVO.assetId` vs `path`.

Worker/coordinator parity: shared file H2 metadata DB (GEO-11).

---

## 5. Template Reference Scan for 409 Delete

**Source:** `TemplateRepository.findActiveForCatalog()` → `TemplatePO.contentJson` + `contentYaml`.

**Preferred:** Parse to `TemplateV2VO`; walk sources:

- `GeoSyntheticSourceVO`: `boundaryAssetId`, `networkAssetId`, `boundaryPath`/`networkPath` with `asset:{id}`
- `GeoJsonSourceVO`: `assetId`, `path` with `asset:{id}`

**Fallback:** Substring `asset:{uuid}` + JSON field literals if parse fails.

**Delete:** `GeoAssetReferenceScanner.findUsages(id)` → non-empty throws `GeoAssetInUseException` → `409`.

---

## 6. Test Strategy

| Layer | Class | Module |
|-------|-------|--------|
| Mapper unit | `GeoSyntheticRequestMapperTests`, `GeoJsonLocationMapperTests` | calcite |
| Resolver unit | `GeoJsonLoaderTests` | geo |
| Reference scan | `GeoAssetReferenceScannerTests` | service |
| Service H2 | `GeoAssetServiceTests` | service |
| REST IT | `ConsoleGeoAssetControllerIT` | service |
| Pipeline IT | `TemplateV2RunnerGeoAssetSourceTests` | calcite or service |

**REST IT:** upload/list/get/delete; delete-with-template → 409; oversize → 400/413; invalid root → 400.

**Pipeline IT:** `geo_synthetic` + `boundaryAssetId`, `geojson` + `assetId`; unknown id → IAE; v2.2 classpath ITs unchanged with null resolver.

**Harness:** P1 only in Phase 23 (TEST-11); no P0 promotion.

---

## 7. Risks (CONTEXT + PITFALLS)

| Risk | Mitigation |
|------|------------|
| H2 blow-up / OOM (P1) | Limits + multipart caps; oversize IT |
| Dual resolution (P2) | Single spine; mapper normalization; asset pipeline IT |
| Type split collapse (P5) | Separate VOs/factories |
| Preview ≠ runtime (P6) | Shared resolver + GET geojson; preview Phase 22 |
| Open upload (P7) | RBAC when security enabled |
| P0 expansion (P8) | P1 tests only |
| Incomplete threading (R1) | Update all loader call sites in one PR |
| YAML-only templates (R3) | Scan both CLOB columns |

---

## Open Questions for Planner

1. D-02 throw vs warn when path + asset-id both set.
2. GET geojson raw body vs `R<String>`.
3. Whether `db/schema.sql` is maintained for `geo_asset`.

---

*Phase: 21-Geo Asset Registry + Runtime Resolution*  
*Research complete: 2026-07-31*