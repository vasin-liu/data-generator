---
phase: 18
slug: geo-generator-modes
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-07-31
updated: 2026-07-31
---

# Phase 18 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Retroactive Nyquist hygiene 2026-07-31: transcribed from `18-VERIFICATION.md` (4/4 must-haves, 45/45 tests green) — no new tests.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Maven Surefire) |
| **Module** | `data-generator-geo` |
| **Config file** | none dedicated — pure unit tests in `data-generator-geo/src/test/` |
| **Quick run command** | `.\mvnw-jdk25.ps1 -pl data-generator-geo -Dtest=GeoGenerationRequestValidationTests test` |
| **Full suite command** | `.\mvnw-jdk25.ps1 -pl data-generator-geo -am test` |
| **Estimated runtime** | ~15–30s (full module); ~5s focused validation slice |

---

## Sampling Rate

- **After every task commit:** Run the task's `-Dtest=` or compile verify from the Per-Task Verification Map
- **After every plan wave:** Re-run wave test classes (`GeoGenerationRequestValidationTests` after wave 1; `BboxPointGeneratorTests,GeoSyntheticGeneratorTests` after wave 2; full module after wave 3)
- **Before `/gsd-verify-work`:** Full suite command must be green (45/45 recorded 2026-07-30)
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 18-01-01 | 01 | 1 | GEO-02 | T-18-01 | `GeoGenerationMode` adds BBOX/CIRCLE without altering boundary/line semantics | unit | `.\mvnw-jdk25.ps1 -pl data-generator-geo -Dtest=GeoSyntheticGeneratorTests test` | ✅ | ✅ green |
| 18-01-02 | 01 | 1 | GEO-02 | — | `GeoGenerationRequest` exposes flat bbox/circle WGS84 fields | compile | `.\mvnw-jdk25.ps1 -pl data-generator-geo -DskipTests compile` | ✅ | ✅ green |
| 18-01-03 | 01 | 1 | GEO-02 | T-18-02 | `validateBbox()` / `validateCircle()` fail fast with field-named `IllegalArgumentException` | unit | `.\mvnw-jdk25.ps1 -pl data-generator-geo -Dtest=GeoGenerationRequestValidationTests test` | ✅ | ✅ green |
| 18-02-01 | 02 | 2 | GEO-02 | T-18-03 | `BboxPointGenerator` seeded uniform sampling + minDistance retry capped at 10_000 | unit | `.\mvnw-jdk25.ps1 -pl data-generator-geo -Dtest=BboxPointGeneratorTests test` | ✅ | ✅ green |
| 18-02-02 | 02 | 2 | GEO-02 | — | `GeoSyntheticGenerator` dispatches BBOX to `BboxPointGenerator` | compile | `.\mvnw-jdk25.ps1 -pl data-generator-geo -DskipTests compile` | ✅ | ✅ green |
| 18-02-03 | 02 | 2 | GEO-02 | — | BBOX integration: in-domain, reproducibility; boundary/line tests unchanged | unit | `.\mvnw-jdk25.ps1 -pl data-generator-geo -Dtest=GeoSyntheticGeneratorTests test` | ✅ | ✅ green |
| 18-03-01 | 03 | 3 | GEO-02 | T-18-04 | `CirclePointGenerator` area-uniform polar sampling + Haversine acceptance gate | unit | `.\mvnw-jdk25.ps1 -pl data-generator-geo -Dtest=CirclePointGeneratorTests test` | ✅ | ✅ green |
| 18-03-02 | 03 | 3 | GEO-02 | T-18-05 | CIRCLE dispatch; four-mode switch exhaustive | compile | `.\mvnw-jdk25.ps1 -pl data-generator-geo -DskipTests compile` | ✅ | ✅ green |
| 18-03-03 | 03 | 3 | GEO-02 | T-18-05 | Full module regression (45 tests, 0 failures) | unit | `.\mvnw-jdk25.ps1 -pl data-generator-geo -am test` | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Evidence: `18-VERIFICATION.md` (2026-07-30) — Tests run: 45, Failures: 0, Errors: 0.*

---

## Wave 0 Requirements

- [x] Existing `GeoSyntheticGenerator` + boundary/line path — baseline before BBOX/CIRCLE
- [x] `GeoHaversine` distance helper — reused for minDistance and CIRCLE radius gate
- [x] `GeoSyntheticGeneratorTests` boundary/line regression baseline — must stay green through all waves
- [x] `GeoGenerationMode` BOUNDARY_POINTS + LINE_SAMPLE — unchanged semantics

*No greenfield test harness — Phase 18 extends the existing `data-generator-geo` module only.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| — | — | — | All phase behaviors have automated unit/integration verification with deterministic seeds |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (existing geo module baseline)
- [x] No watch-mode flags
- [x] Feedback latency < 30s (focused slice)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** complete (retroactive hygiene 2026-07-31)

## Validation Audit 2026-07-31

| Metric | Count |
|--------|-------|
| Gaps found | 0 |
| Resolved | 0 (already COVERED in VERIFICATION) |
| Escalated | 0 |
| Action | Frontmatter + map statuses set to compliant/green from `18-VERIFICATION.md`; no new test files |

---

_Backfill provenance: Nyquist State B hygiene — transcription from existing green evidence only. GEO-02 pipeline half deferred to Phases 19–20 as designed._
