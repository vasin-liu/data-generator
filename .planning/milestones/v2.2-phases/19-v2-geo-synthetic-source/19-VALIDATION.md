---
phase: 19
slug: v2-geo-synthetic-source
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-07-31
updated: 2026-07-31
---

# Phase 19 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Retroactive Nyquist hygiene 2026-07-31: transcribed from `19-VERIFICATION.md` (18/18 truths, 25-test bundle green) — no new tests.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Maven Surefire) |
| **Modules** | `data-generator-common/data-generator-core`, `data-generator-calcite`, `data-generator-service` (CoreConfig compile) |
| **Config file** | none dedicated — calcite unit/RowSource tests; classpath fixtures under `data-generator-calcite/src/test/resources/geo/` |
| **Quick run command** | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -Dtest=GeoSyntheticRequestMapperTests test` |
| **Full suite command** | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am "-Dtest=GeoSyntheticRequestMapperTests,GeoSyntheticRowSourceTests,GeoSyntheticSourceFactoryTests,GeoJsonRowSourceTests,TemplateV2RunnerGeoSourceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` |
| **Estimated runtime** | ~20–60s (focused bundle); compile slices ~10s |

---

## Sampling Rate

- **After every task commit:** Run task compile verify or `-Dtest=` from Per-Task Verification Map
- **After wave 1 (19-01):** `data-generator-core` compile green; three VO files present
- **After wave 2 (19-02):** `GeoSyntheticRequestMapperTests` green (7 tests)
- **After wave 3 (19-03):** Full geo test bundle (25 tests) + `CoreConfig` compile
- **Before `/gsd-verify-work`:** Full suite command must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 19-01-01 | 01 | 1 | GEO-01 | T-19-01 | Independent `GeoSyntheticSourceOutputVO` (no GeoJson coupling) | compile | `.\mvnw-jdk25.ps1 -pl data-generator-common/data-generator-core -DskipTests compile` | ✅ | ✅ green |
| 19-01-02 | 01 | 1 | GEO-01 | — | `GeoSyntheticSampleVO` nested sample block | compile | `.\mvnw-jdk25.ps1 -pl data-generator-common/data-generator-core -DskipTests compile` | ✅ | ✅ green |
| 19-01-03 | 01 | 1 | GEO-01 | T-19-02 | `GeoSyntheticSourceVO` `geo_synthetic` type; seed defaults 0L | compile | `.\mvnw-jdk25.ps1 -pl data-generator-common/data-generator-core -DskipTests compile` | ✅ | ✅ green |
| 19-02-01 | 02 | 2 | GEO-01, GEO-03 | T-19-03 | Dedicated `GeoSyntheticRequestMapper`; `GeoIteratorRequestMapper` untouched | compile | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -DskipTests compile` | ✅ | ✅ green |
| 19-02-02 | 02 | 2 | GEO-01, GEO-03 | T-19-04 | Four-mode VO→request mapping; source-scoped invalid-config messages | unit | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -Dtest=GeoSyntheticRequestMapperTests test` | ✅ | ✅ green |
| 19-03-01 | 03 | 3 | GEO-01, GEO-03 | T-19-05 | `GeoSyntheticSourceFactory` + eager `GeoSyntheticRowSource` materialization | compile | `.\mvnw-jdk25.ps1 -pl data-generator-calcite,data-generator-service -am -DskipTests compile` | ✅ | ✅ green |
| 19-03-02 | 03 | 3 | GEO-01, GEO-03 | — | `CoreConfig.geoSyntheticSourceFactory()`; geojson bean unchanged | compile | `.\mvnw-jdk25.ps1 -pl data-generator-service -am -DskipTests compile` | ✅ | ✅ green |
| 19-03-03 | 03 | 3 | GEO-01, GEO-03 | T-19-06 | Four-mode RowSource + Factory smoke; geojson regression green (D-15) | unit+IT | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am "-Dtest=GeoSyntheticRowSourceTests,GeoSyntheticSourceFactoryTests,GeoJsonRowSourceTests,TemplateV2RunnerGeoSourceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Evidence: `19-VERIFICATION.md` (2026-07-30) — Phase 19 bundle: 25 tests, 0 failures. TemplateV2Runner geo_synthetic pipeline IT deferred Phase 20 (D-14).*

---

## Wave 0 Requirements

- [x] Phase 18 `GeoGenerationRequest` / `GeoSyntheticGenerator` four-mode API
- [x] `GeoJsonSourceFactory` / `GeoJsonRowSource` pattern — GEO-03 regression baseline
- [x] `GeoJsonSourceVO` + `@AutoService(SourceVO.class)` SPI pattern
- [x] `GeoRowSchemaSupport` schema inference
- [x] Classpath geo fixtures for boundary/line RowSource tests

*No TemplateV2Runner geo_synthetic pipeline IT in Wave 0 — scoped to Phase 20 per D-14.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| — | — | — | All Phase 19 Factory → RowSource behaviors covered by calcite unit/integration tests |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s (focused bundle)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** complete (retroactive hygiene 2026-07-31)

## Validation Audit 2026-07-31

| Metric | Count |
|--------|-------|
| Gaps found | 0 |
| Resolved | 0 (already COVERED in VERIFICATION) |
| Escalated | 0 |
| Action | Frontmatter + map statuses set to compliant/green from `19-VERIFICATION.md`; no new test files |

---

_Backfill provenance: Nyquist State B hygiene — transcription from existing green evidence only. Pipeline proof, GEO-04 docs, and TEST-10 deferred to Phase 20 as designed._
