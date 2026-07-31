---
phase: 20
slug: pipeline-proof-docs-p1
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-07-31
updated: 2026-07-31
---

# Phase 20 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Retroactive Nyquist hygiene 2026-07-31: transcribed from `20-VERIFICATION.md` (15/15 truths) and `20-UAT.md` (complete) — no new tests.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Maven Surefire) for pipeline IT; PowerShell file-verify for docs/matrix tasks |
| **Module** | `data-generator-calcite` (IT); `.planning/test-matrix.yaml` + `docs/` (static) |
| **Config file** | none dedicated — in-process `TemplateV2Runner` registry (no `@SpringBootTest`) |
| **Quick run command** | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am "-Dtest=TemplateV2RunnerGeoSyntheticSourceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` |
| **Full regression command** | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am "-Dtest=TemplateV2RunnerGeoSyntheticSourceTests,TemplateV2RunnerGeoSourceTests,GeoSyntheticRowSourceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` |
| **Docs/matrix verify** | PowerShell / file content checks — **not Maven** |
| **Estimated runtime** | ~15–30s (4 pipeline ITs); ~5s static doc/matrix checks |

---

## Sampling Rate

- **After Plan 20-01 tasks:** Run `TemplateV2RunnerGeoSyntheticSourceTests` slice
- **After Plan 20-02 tasks:** Run PowerShell content checks from Per-Task Verification Map (no Maven)
- **After Plan 20-03 tasks:** YAML P0 count invariant + `docs/test-feature-matrix.md` grep
- **Before `/gsd-verify-work`:** Pipeline IT (4/4) + matrix P1 row + P0 count == 15
- **Max feedback latency:** 30 seconds (Maven slice); static checks immediate

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 20-01-01 | 01 | 1 | GEO-02 | T-20-01 | Dedicated `TemplateV2RunnerGeoSyntheticSourceTests` + registry helpers | compile | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am -DskipTests compile test-compile` | ✅ | ✅ green |
| 20-01-02 | 01 | 1 | GEO-02 | T-20-02 | BOUNDARY_POINTS + LINE_SAMPLE pipeline → expected/non-empty rows | IT | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am "-Dtest=TemplateV2RunnerGeoSyntheticSourceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` | ✅ | ✅ green |
| 20-01-03 | 01 | 1 | GEO-02 | — | BBOX + CIRCLE pipeline; geojson + RowSource regression | IT | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am "-Dtest=TemplateV2RunnerGeoSyntheticSourceTests,TemplateV2RunnerGeoSourceTests,GeoSyntheticRowSourceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` | ✅ | ✅ green |
| 20-02-01 | 02 | 1 | GEO-04 | T-20-03 | `docs/geospatial-overview.md` lists `geo_synthetic` and distinguishes types | docs | file-verify: `Select-String docs/geospatial-overview.md -Pattern geo_synthetic` | ✅ | ✅ green |
| 20-02-02 | 02 | 1 | GEO-04 | — | Minimal V2 YAML with `type: geo_synthetic` in dedicated/overview docs | docs | file-verify: `docs/geo-synthetic-v2-source.md` YAML examples | ✅ | ✅ green |
| 20-02-03 | 02 | 1 | GEO-04 | — | Output formats columns/wkt/geojson; SQL companion = existing `V2_GEO_*` | docs | file-verify: output format sections present | ✅ | ✅ green |
| 20-03-01 | 03 | 2 | TEST-10, GEO-02 | T-20-04 | `geo-synthetic` row tier P1, status covered; P0 count remains 15 | static | file-verify: `.planning/test-matrix.yaml` | ✅ | ✅ green |
| 20-03-02 | 03 | 2 | TEST-10 | — | `docs/test-feature-matrix.md` regenerated with geo-synthetic P1 row | docs | file-verify: matrix doc row | ✅ | ✅ green |
| 20-03-03 | 03 | 2 | TEST-10 | T-20-05 | P0 merge gate unchanged; linked test classes on disk | static | file-verify: P0=15; classes exist | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Evidence: `20-VERIFICATION.md` (2026-07-30) — `TemplateV2RunnerGeoSyntheticSourceTests`: Tests run: 4, Failures: 0; docs + matrix verified. Plans 20-02/20-03 use file-verify, not Maven — by design.*

---

## Wave 0 Requirements

- [x] Phase 19 `GeoSyntheticSourceFactory` / `GeoSyntheticRowSource` / `GeoSyntheticRequestMapper`
- [x] `TemplateV2RunnerGeoSourceTests` pattern — cloned registry helper
- [x] `.planning/test-matrix.yaml` + `scripts/generate-test-matrix-doc.ps1`
- [x] P0 gate frozen at 15 rows — `scripts/verify-harness.ps1` unchanged
- [x] `docs/geospatial-overview.md` baseline — extended, not replaced

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| — | — | — | Pipeline IT automated; docs/matrix tasks use scripted file-verify (not operator walkthrough) |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 30s (Maven IT slice)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** complete (retroactive hygiene 2026-07-31)

## Validation Audit 2026-07-31

| Metric | Count |
|--------|-------|
| Gaps found | 0 |
| Resolved | 0 (already COVERED in VERIFICATION + UAT) |
| Escalated | 0 |
| Action | Frontmatter + map statuses set to compliant/green from `20-VERIFICATION.md` / `20-UAT.md`; no new test files |

---

_Backfill provenance: Nyquist State B hygiene — transcription from existing green evidence only. Closes Phase 19 D-14 (GEO-02 pipeline), GEO-04, and TEST-10 without P0 inflation._
