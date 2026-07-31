# Pitfalls Research

**Domain:** Brownfield Template V2 — GeoJSON asset upload (metadata DB), asset-id references, console map preview for assets and `geo_synthetic`  
**Researched:** 2026-07-31  
**Confidence:** HIGH

## Critical Pitfalls

### Pitfall 1: Huge GeoJSON blowing H2 / metadata DB

**What goes wrong:**
Operators upload city-scale road networks or national boundary files (tens–hundreds of MB). Rows land as full-text CLOB/BLOB in the file H2 metadata DB (`jdbc:h2:file:../db/data-generator`). Startup slows, backup/restore balloons, single upload OOMs the JVM during parse+store, and concurrent runs contend on the same file DB lock (Windows dual-JVM staging already sensitive).

**Why it happens:**
v2.2 deliberately deferred upload; `GeoResourceResolver` reads whole files into memory (`readAllBytes` / `Files.readString`). The natural “mirror UDF/secret persistence” pattern is inline DB storage without size caps. H2 file DB is the default metadata store with no external object store in scope.

**How to avoid:**
- Enforce upload **max bytes** and **max feature count** at the API gate (reject before persist).
- Store **validated, normalized** GeoJSON only; optionally persist a **bounds summary** + feature count for list/preview without re-parsing.
- Prefer **CLOB for text GeoJSON** with streaming validation where possible; avoid duplicating the same bytes in template YAML and asset table.
- Document operator limits in console copy; align test fixtures with small bundled files (`data-generator-calcite/src/test/resources/geo/`).
- Consider filesystem spill + DB metadata (name, id, checksum, bounds) if a single CLOB row is still too heavy — but do not silently fall back to unbounded inline storage.

**Warning signs:**
- No `maxUploadBytes` / multipart limit in controller or Spring config.
- ITs only use tiny fixtures; no test for rejected oversize upload.
- H2 file grows faster than template/UDF tables after manual console testing.
- `GeoJsonLoader` or upload path loads entire payload multiple times (validate → store → preview).

**Phase to address:**
Phase 21 (GEO-05 — asset upload + metadata DB persistence)

---

### Pitfall 2: Path vs asset-id dual resolution bugs

**What goes wrong:**
Templates mix `boundaryPath: classpath:geo/...`, `boundaryPath: /abs/path/from/ConsoleUploadController`, and new `boundaryAssetId: abc-123`. Runtime resolves the wrong source: asset-id treated as filesystem path, path refs broken after restart (upload dir vs DB), or asset-id works in console but not in `TemplateV2Runner` because `GeoResourceResolver` was never extended. v2.2 classpath ITs stay green while operator asset-id templates fail in production.

**Why it happens:**
GEO-03 locked path-only resolution via `GeoResourceResolver`. v2.3 adds a second reference shape without a single resolution spine. `ConsoleUploadController` already writes ephemeral filesystem paths under `../uploaded-sources` — easy to conflate with durable asset-id storage. `GeoSyntheticSourceVO` has `boundaryPath` / `networkPath` only today.

**How to avoid:**
- Introduce **one resolver** (extend `GeoResourceResolver` or adjacent service) with explicit precedence: `assetId` → DB lookup; else `classpath:` / filesystem path (unchanged v2.2 behavior).
- **Mutual exclusion** in mapper/validator: per field, asset-id OR path, not both ambiguously populated.
- Persist asset-id in template YAML; never persist console upload absolute paths as the long-term reference.
- Regression: all v2.2 path/classpath ITs unchanged; **add** asset-id ITs that never touch filesystem paths.
- Template validator should fail fast on unknown asset-id at publish/run (mirror UDF registry checks).

**Warning signs:**
- `GeoSyntheticRequestMapper` reads path strings only; no asset repository injection.
- Console wizard writes returned filesystem path into YAML when user picked “hosted asset.”
- Different code paths for preview API vs `GeoSyntheticRowSource`.
- Docs mention asset-id but runtime still calls `Path.of(trimmed)` for non-classpath strings.

**Phase to address:**
Phase 21 (GEO-05 — resolver + VO fields + runtime wiring)

---

### Pitfall 3: XSS / SVG / HTML smuggling in GeoJSON properties

**What goes wrong:**
GeoJSON `properties` contain `<script>`, `<img onerror=…>`, or SVG payloads. Console map popups render property values as HTML; React `dangerouslySetInnerHTML` or Leaflet popup HTML sinks execute in the operator browser. Audit logs or template preview echo raw property strings. Even “internal” consoles get compromised session tokens.

**Why it happens:**
GeoJSON is untrusted user upload. v2.2 `includeProperties: true` copies properties to output columns (`prop.*` prefix in `GeoSyntheticGenerator`). Map preview naturally wants rich labels from properties. Security focus elsewhere (UDF sandbox, secrets) — geo properties treated as passive data.

**How to avoid:**
- **Never** render property values as HTML in map popups; use text nodes only.
- Escape or strip HTML-like values in preview API responses; cap property key/value length.
- Optional: reject uploads whose properties match HTML/SVG patterns at ingest (warn, don’t silently sanitize geometry).
- Align with existing console pattern: structured JSON in Ant Design components, not raw HTML injection.
- Document that `includeProperties` in **runtime output** is operator-controlled and may flow to downstream sinks — separate from map preview sanitization.

**Warning signs:**
- Map component uses `innerHTML`, `dangerouslySetInnerHTML`, or Leaflet `bindPopup` with unescaped template strings.
- Preview endpoint returns raw GeoJSON blob to client with no Content-Security-Policy consideration.
- No test fixture with `<script>alert(1)</script>` in a property.

**Phase to address:**
Phase 22 (GEO-07 — console map) + Phase 21 ingest validation (defense in depth)

---

### Pitfall 4: Map library SSR / Vite bundling issues

**What goes wrong:**
Adding Leaflet/MapLibre/OpenLayers breaks `npm run build`: `window is not defined`, CSS import failures, dynamic `import()` chunks missing from embedded `classpath:static/console/` bundle, or Podman E2E fails because map tiles hit external CDN blocked in CI. Bundle size jumps; `frontend-maven-plugin` build exceeds CI timeout.

**Why it happens:**
Console is a **client-only** Vite SPA (React 19, no SSR today) — but map libs often assume DOM at module top level. Default `vite.config.ts` has no `ssr` shims. No map dependency exists in `package.json` yet. Tile layers pull network deps unsuitable for air-gapped installs.

**How to avoid:**
- **Lazy-load** map routes/components (`React.lazy`) so Codemirror/home pages don’t pay map bundle cost.
- Import map CSS explicitly in the component chunk; verify `tsc && vite build` in CI (`console-verify.yml`).
- Default preview to **local GeoJSON layer only** (no external tile dependency) or bundled static tiles for tests.
- Guard `window`/`document` access inside `useEffect`, not module scope.
- Run `scripts/verify-console.ps1` after adding deps — mandatory per console-verify rule.

**Warning signs:**
- Map import at top level of `App.tsx` or shared layout.
- Build passes locally but fails in Maven `frontend-maven-plugin` Node phase.
- Playwright specs timeout waiting for tile CDN.
- `chunkSizeWarningLimit` exceeded with no code-splitting plan.

**Phase to address:**
Phase 22 (GEO-07 — console map UI)

---

### Pitfall 5: Breaking the `geojson` / `geo_synthetic` type split

**What goes wrong:**
Asset upload work collapses `type: geojson` (read-only Feature/FeatureCollection source) and `type: geo_synthetic` (generated points) into one VO, one factory, or one console wizard. Existing `GeoJsonSourceFactory` / `GeoJsonSourceVO` regress; docs contradict v2.2 GEO-04; operators cannot tell read vs synthesize. Harness `geo-synthetic` row breaks or falsely covers geojson read paths.

**Why it happens:**
Both types use `GeoResourceResolver` and GeoJSON files. Upload UI tempts a single “geo file” bucket. Phase 19 explicitly split factories (`GeoSyntheticSourceFactory` does not support `GeoJsonSourceVO`).

**How to avoid:**
- Keep **separate** `SourceVO` subtypes, factories, and console flows per GEO-04.
- Asset-id refs apply to **both** where path applied before — but types remain distinct.
- Docs: `geojson` = emit features from file; `geo_synthetic` = generate point rows from boundary/line/bbox/circle modes.
- Do not rename `boundaryPath` on geojson or add synthesis modes to `GeoJsonSourceVO`.
- Re-run `GeoJsonSourceFactory` regression tests when touching resolver.

**Warning signs:**
- Single `GeoSourceVO` or merged `@JsonSubType`.
- Console menu item “Geo source” without read vs synthetic choice.
- `geo-synthetic` matrix row linked_tests start including geojson-only classes without intent.

**Phase to address:**
Phase 21 (GEO-05 — shared asset store, separate source types) + Phase 23 (docs/harness)

---

### Pitfall 6: Preview ≠ runtime sampling mismatch

**What goes wrong:**
Console map shows points along a boundary, but pipeline run yields different count, seed, or spacing because preview uses simplified client-side math while runtime uses `GeoSyntheticGenerator` / JTS. Preview samples 50 fixed points; runtime respects `count`, `seed`, `minDistanceMeters`, `sample.strategy`. Operators trust preview, ship template, get support tickets.

**Why it happens:**
Implementing full JTS sampling in TypeScript duplicates Java logic. Shortcuts: preview plots raw geometry vertices or random browser `Math.random` instead of seeded Java generator. BBOX/CIRCLE modes may preview bbox/circle shape but not actual point set.

**How to avoid:**
- Preview API should call **server-side** preview endpoint that reuses `GeoSyntheticGenerator.generatePoints` (or subset) with the same request mapping as `GeoSyntheticRowSource` — capped row count for UI.
- For **asset browse** (static GeoJSON), preview shows geometry only — no synthetic points unless config is supplied.
- Display explicit “preview sample (max N points, seed S)” vs “run output” disclaimer if full count is expensive.
- IT: same seed + config → preview endpoint matches RowSource first K coordinates within epsilon.

**Warning signs:**
- Preview implemented entirely in frontend with haversine/bbox math copied from Java.
- No shared `GeoSyntheticRequestMapper` on preview path.
- BBOX/CIRCLE preview draws rectangle/circle but not point scatter from seed.

**Phase to address:**
Phase 22 (GEO-07 — map preview) with Phase 21 backend preview endpoint

---

### Pitfall 7: RBAC off by default vs new upload surface

**What goes wrong:**
Geo asset upload API ships on `/api/console/...` with **no** permission gate when `console-security.enabled=false` (default). Any network client who can reach `:9876` uploads arbitrary GeoJSON, fills DB, or probes paths. When RBAC **is** enabled, new endpoints lack `ConsolePermission` mapping — VIEWER can upload, or OPERATOR cannot. Inconsistent with UDF upload expectations (`ConsoleUdfController` / UDF filter patterns).

**Why it happens:**
v2.1 shipped RBAC enable-path with default-off (SEC-01). New endpoints often copy `ConsoleUploadController` (no role checks). Upload feels like “template helper,” not governed artifact.

**How to avoid:**
- Add explicit permission (e.g. extend `ConsolePermission` or reuse `TEMPLATE_EDIT` / new `GEO_ASSET_ADMIN`) checked in controller **when security enabled**.
- Mirror `ConsoleUdfAuthorizationFilter` pattern or central filter rules for `/api/console/geo-assets/**`.
- Document: default-off ≠ safe on untrusted networks; staging enable recipe includes new routes.
- IT: `ConsoleAuthorizationIntegrationIT`-style case for asset upload with/without role header.
- Do **not** flip default RBAC on (SEC-02 still deferred).

**Warning signs:**
- New controller has no `@PreAuthorize` / filter permission table entry.
- Only frontend hides upload button; API remains open.
- RBAC enable profile tests don’t cover geo asset routes.

**Phase to address:**
Phase 21 (GEO-05 — API) + Phase 22 (console gating mirrors API)

---

### Pitfall 8: P0 gate accidental expansion

**What goes wrong:**
New geo-asset upload ITs, map Playwright specs, or asset-id pipeline tests are marked `tier: P0` in `.planning/test-matrix.yaml`. `verify-harness.ps1` / `harness-verify.yml` blocks merges on Playwright/map flakes, H2 asset fixtures, or Podman tile timing. Team skips harness or marks rows skipped-conditional — worse than no coverage.

**Why it happens:**
v2.0–v2.2 habit: important feature ⇒ P0. v2.2 TEST-10 explicitly froze P0 at **15 rows**; geo-synthetic is **P1**. Map E2E is slower and flakier than Calcite ITs.

**How to avoid:**
- New rows: `geo-assets` / `console-geo-map` at **P1** (or P2 for Playwright-only) unless proven CI-default-fast.
- Keep existing `geo-synthetic` P1; extend `linked_tests`, do not promote to P0.
- Maven IT for asset-id resolution + upload validation as P1; Playwright map smoke supplementary.
- Explicit review gate: any `tier: P0` diff in v2.3 PR requires milestone scope re-open.

**Warning signs:**
- `test-matrix.yaml` diff adds P0 row count > 15.
- harness-verify starts requiring Playwright for geo map.
- PR description claims “P0 coverage for geo assets.”

**Phase to address:**
Phase 23 (harness + milestone closeout — TEST-11)

---

### Pitfall 9: Reusing ephemeral `ConsoleUploadController` paths as asset storage

**What goes wrong:**
GEO-05 implemented by pointing asset upload at existing `/api/console/uploads/file`, returning absolute paths under `../uploaded-sources`. Assets lost on redeploy, paths differ per host, DB metadata empty, asset-id story abandoned. Dual resolution pitfall (Pitfall 2) guaranteed.

**Why it happens:**
`ConsoleUploadController` already exists for template file sources — fastest wire-up. Looks like “upload works” in demo.

**How to avoid:**
- GEO-05 deliverable is **metadata DB persistence + asset-id**, not filesystem path return.
- Deprecate or clearly separate “template scratch upload” vs “registered geo asset.”
- Runtime resolver reads asset-id from DB, not wizard path strings.

**Warning signs:**
- No new JPA entity / schema migration for geo assets.
- Upload API response is `{ "path": "/abs/..." }` only, no `assetId`.
- SUMMARY claims GEO-05 complete without DB table.

**Phase to address:**
Phase 21 (GEO-05)

---

### Pitfall 10: Unbounded geometry complexity (not just file size)

**What goes wrong:**
Small file on disk but millions of vertices (simplified poorly, or TopoJSON-style density) passes byte limit yet hangs JTS parse, map preview, and boundary point generation. CPU DoS on shared service.

**Why it happens:**
Byte limits alone don’t cap vertex count. `GeoJsonLoader` and map simplification may run on full resolution.

**How to avoid:**
- Validate feature/vertex counts at ingest; reject or require simplification server-side.
- Preview endpoint uses simplified geometry (Douglas-Peucker or max vertices) distinct from runtime fidelity rules — document delta.
- Timeout guards on parse/generate for upload and preview.

**Warning signs:**
- Only `file.getSize()` checked.
- Preview fetches full asset for world-scale polygon every pan/zoom.

**Phase to address:**
Phase 21 (ingest validation) + Phase 22 (map simplification)

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Inline CLOB for full GeoJSON | Simple CRUD like templates/secrets | H2 bloat, slow list | MVP with strict size caps + migration note |
| Filesystem store + DB metadata only | Avoids large CLOB | Path drift, backup split-brain | If documented + checksum; not for v2.3 default without explicit decision |
| Client-only preview math | No backend endpoint | Preview/runtime mismatch (Pitfall 6) | Never for `geo_synthetic` config preview |
| External OSM/CDN tiles | Pretty basemap | CI/air-gap failures | Optional toggle; default geometry-only layer |
| Reuse `ConsoleUploadController` | Fast demo | No asset-id (Pitfall 9) | Never for GEO-05 DoD |
| Skip RBAC on new routes | Matches old upload controller | Open upload on intranet-exposed hosts | Never; add permission hooks even if default-off |
| Promote geo assets to P0 | Feels “done” | Merge gate flake | Never in v2.3 |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| `GeoResourceResolver` | Bolt asset-id parsing ad hoc in RowSource | Central resolver; path + asset-id |
| H2 file metadata DB | Store multi-MB assets unbounded | Size/feature limits; monitor file growth |
| `GeoSyntheticSourceVO` | Break v2.2 path fields | Add optional asset-id fields; path still works |
| `GeoJsonSourceVO` | Conflate with synthetic | Asset-id on read path only; type unchanged |
| `ConsoleUploadController` | Treat as GEO-05 storage | Separate registered asset API |
| UDF upload pattern | Copy PF4J governance for GeoJSON | Lighter validate+persist; no publish lifecycle unless scoped |
| Map preview | Duplicate Java sampling in TS | Server preview endpoint sharing mapper/generator |
| Vite embedded static | Map lib breaks build | Lazy routes; verify Maven frontend phase |
| RBAC default-off | Assume private network | Permission checks when enabled; document exposure |
| Harness | Playwright-only P0 for map | P1 Maven IT for resolver; Playwright supplementary |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Full GeoJSON in list API | Console asset table slow | List returns metadata + bounds only; detail fetches body | > ~100 assets or > 1 MB each |
| Re-parse on every preview pan | CPU spike | Cache parsed geometry per asset-id; simplify for map | City boundary + repeated previews |
| Synchronous upload parse in HTTP thread | Gateway timeout | Stream validate; async optional for huge (out of scope — reject instead) | Upload > few seconds |
| Map bundle in main chunk | Slow first console load | Code-split map page | Any operator opening console |
| H2 CLOB + frequent updates | File lock contention | Write-once assets; version immutably | Concurrent uploads + Windows file H2 |
| Preview generates full `count` | Preview API as slow as run | Cap preview sample (e.g. max 500 points) | `count: 100000` templates |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| HTML in GeoJSON properties rendered unsafely | Stored XSS in operator browser | Text-only popups; escape; ingest warnings |
| Open upload API (RBAC off) | Arbitrary file fill / DoS | Size limits + optional auth when enabled |
| Asset-id enumeration | Data exfil of hosted boundaries | Same permission as upload/list; no public GET |
| Path traversal via `networkPath` after upload | Read arbitrary server files | Resolver rejects `..`; asset-id only from DB |
| SVG/XML bombs in GeoJSON | Parser DoS | Byte + vertex limits; parse timeouts |
| Logging full GeoJSON on error | Sensitive geodata in logs | Log asset-id + bounds only |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Map works; run fails (preview mismatch) | Lost trust in console | Server-side preview; seed/count shown |
| Upload succeeds but template still uses path | Confusion after redeploy | Wizard binds asset-id; show hosted label |
| No feedback on oversize file | Opaque 500 error | Clear max MB + feature limit message |
| `geojson` vs `geo_synthetic` wizard merge | Wrong pipeline type | Explicit type picker per GEO-04 |
| Map empty in CI/Podman | “Broken” milestone sign-off | Geometry-only layer works offline |
| RBAC enabled, upload 403 with no hint | Operators stuck | Document required role header |

## "Looks Done But Isn't" Checklist

- [ ] **GEO-05 upload:** Often missing DB persistence — verify JPA entity + schema migration, not only filesystem path
- [ ] **Asset-id resolution:** Often missing runtime path — verify `TemplateV2Runner` IT with asset-id, not only REST upload
- [ ] **Path backward compat:** Often missing — verify v2.2 classpath/path ITs still green unchanged
- [ ] **geojson vs geo_synthetic:** Often blurred — verify separate factories, docs, and console flows
- [ ] **Map preview:** Often client-faked — verify preview API uses `GeoSyntheticGenerator` / shared mapper
- [ ] **XSS:** Often missing — verify property popup test with script payload does not execute
- [ ] **RBAC:** Often API-open — verify permission when `console-security.enabled=true`; default still off
- [ ] **Size limits:** Often missing — verify reject oversize / over-vertex upload with 400
- [ ] **Harness:** Often P0 — verify new rows P1; P0 count still 15
- [ ] **Console verify:** Often skipped — verify `scripts/verify-console.ps1` after map deps

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| H2 metadata DB bloated | MEDIUM | Purge assets; add limits; migrate large blobs to filesystem store |
| Path/asset-id dual bugs | MEDIUM | Introduce single resolver; template migration tool for hosted refs |
| Preview/runtime mismatch | LOW | Add server preview endpoint; deprecate client-only math |
| Map build broken | LOW | Lazy-load; fix CSS imports; remove CDN tile default |
| P0 promotion | HIGH | Demote rows to P1; restore 15-row gate |
| XSS found in preview | HIGH | Hotfix escape; audit all map HTML; add regression fixture |
| RBAC gap on upload | MEDIUM | Add permission + IT; document staging headers |

## Pitfall-to-Phase Mapping

Provisional v2.3 phases (21–23); adjust when `/gsd-new-milestone` formalizes roadmap.

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Huge GeoJSON / H2 bloat | Phase 21 | Upload reject IT; documented max size; metadata list without full body |
| Path vs asset-id resolution | Phase 21 | Dual IT: classpath path + asset-id; single resolver unit tests |
| Ephemeral upload path misuse | Phase 21 | API returns assetId; DB row exists; no `uploaded-sources` dependency |
| geojson / geo_synthetic split | Phase 21 + 23 | Factory regression + GEO-04 doc update |
| Geometry complexity DoS | Phase 21 | Vertex limit test; parse timeout |
| XSS / HTML in properties | Phase 21 + 22 | Ingest + popup escape tests; no dangerouslySetInnerHTML |
| Map Vite/bundle issues | Phase 22 | `npm run build` + verify-console green |
| Preview ≠ runtime | Phase 22 | Preview API matches generator sample IT |
| RBAC vs upload surface | Phase 21 + 22 | Authorization IT with enable profile |
| P0 gate expansion | Phase 23 | Matrix diff shows P1 only; `p0.pass` unchanged |

## Sources

- `.planning/PROJECT.md` — v2.3 scope (GEO-05, GEO-07), P0 freeze, metadata DB intent
- `.planning/milestones/v2.2-REQUIREMENTS.md` — GEO-03 path-only, deferred GEO-05/07
- `.planning/milestones/v2.2-MILESTONE-AUDIT.md` — deferred assets/map; TEST-10 P1 freeze
- `docs/geo-synthetic-v2-source.md` — geo_synthetic vs geojson; path-only v2.2
- `GeoResourceResolver.java` — classpath/filesystem read whole file
- `GeoSyntheticSourceVO` / `GeoJsonSourceVO` — distinct source types
- `GeoSyntheticGenerator.java` — runtime sampling + `prop.*` properties
- `ConsoleUploadController.java` — ephemeral filesystem uploads (not GEO-05)
- `ConsoleUdfController.java` — governed upload reference pattern
- `application.yaml` — H2 file metadata DB
- `db/schema.sql` — CLOB/BLOB patterns (`template`, `udf_artifact`)
- `.planning/test-matrix.yaml` — `geo-synthetic` P1; 15 P0 rows
- `data-generator-console-web/package.json` / `vite.config.ts` — no map lib yet; SPA embed
- `.planning/research/PITFALLS.md` (v2.1) — P0 promotion + RBAC default-off patterns
- `AGENTS.md` — harness merge gate semantics

---
*Pitfalls research for: v2.3 Geo Assets & Map Preview (data-generator)*  
*Researched: 2026-07-31*
