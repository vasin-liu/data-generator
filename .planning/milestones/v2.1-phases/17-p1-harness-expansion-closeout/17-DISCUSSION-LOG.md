# Phase 17: P1 Harness Expansion + Closeout - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-29
**Phase:** 17-P1 Harness Expansion + Closeout
**Mode:** --auto (all gray areas auto-selected; recommended defaults)
**Areas discussed:** P1 row inventory, Row linkage strategy, HTTP row packaging, Multi-JVM row handling, P0 gate invariants, Docker-gated IT handling, Docs & milestone closeout, Out of scope

---

## P1 row inventory

| Option | Description | Selected |
|--------|-------------|----------|
| Add rows for all v2.1 phases (12–16) | Includes Dameng, resolver docs, Nyquist — scope creep | |
| Add exactly 3 capability rows + verify existing DIST row (recommended) | TEST-09: HTTP execute, multi-JVM, RBAC; DIST row already exists from Phase 15 | ✓ |
| Single combined "v2.1-hardening" row | Loses capability-first matrix semantics (D-01) | |

**Auto-selected:** Two new rows (`exec-http-managed-catalog`, `exec-http-postgres-dialect`, `rbac-enable-path`) plus verify existing `dist-multi-jvm-worker`
**Notes:** TEST-09 lists three proof paths; multi-JVM already wired Phase 15 plan 02.

---

## Row linkage strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Script-primary for all v2.1 proofs | Would require new verify-http script; HTTP proof already has ITs | |
| Maven IT linkage for HTTP/RBAC; script-primary for multi-JVM (recommended) | Matches Phase 15 pattern; embedded-first for IT-capable proofs | ✓ |
| Link only verify scripts (no Maven classes) | Harness would not run ITs on default path | |

**Auto-selected:** Maven IT classes for HTTP and RBAC rows; empty `linked_tests` + script notes for `dist-multi-jvm-worker`
**Notes:** `verify-rbac-enable.ps1` remains supplementary UAT, not harness Maven aggregation.

---

## HTTP row packaging

| Option | Description | Selected |
|--------|-------------|----------|
| Single row covering EXEC-01 + EXEC-02 | Blurs evidence bars; PG row needs Docker gate handling | |
| Two rows: H2 managed catalog + PG dialect upsert (recommended) | Aligns Phase 12 D-08 separate ITs | ✓ |
| Promote EXEC-01 to P0 | Violates Pitfall 8 and TEST-09 | |

**Auto-selected:** Two P1 rows with distinct linked_tests
**Notes:** `ManagedJdbcCatalogHttpExecuteIT` (H2) vs `ManagedJdbcCatalogHttpPostgresUpsertIT` (Testcontainers).

---

## Multi-JVM row handling

| Option | Description | Selected |
|--------|-------------|----------|
| Duplicate row with new id | Redundant with Phase 15 `dist-multi-jvm-worker` | |
| Verify/update existing row only (recommended) | Phase 15 already added P1 row; TEST-09 satisfied | ✓ |
| Link `DistributedSplitRoleIntegrationTests` as multi-JVM proof | Single-JVM stub; mislabels proof (Phase 15 D-08) | |

**Auto-selected:** Retain existing row; update notes/status only if needed

---

## P0 gate invariants

| Option | Description | Selected |
|--------|-------------|----------|
| Expand P0 to include v2.1 proofs | Breaks merge gate stability; Pitfall 8 | |
| Keep 15 P0 rows unchanged; P1 only (recommended) | TEST-09 explicit requirement | ✓ |
| Change verify-harness to ignore linked Maven failures on P1 | Would weaken detection of regressions in linked ITs | |

**Auto-selected:** No changes to P0 set or harness gate semantics
**Notes:** Post-wiring verification: `p0.total == 15`, `p0.pass == true`.

---

## Docker-gated IT handling

| Option | Description | Selected |
|--------|-------------|----------|
| Require Docker for EXEC-02 row to show `covered` in CI | Would block developers without Docker on P1 (acceptable) but adds friction | |
| Allow `skipped-conditional` for PG IT; document in notes (recommended) | Phase 12 review accepted pattern | ✓ |
| Add Surefire XML skip detection to harness now | Optional; defer unless trivial | |

**Auto-selected:** Document skipped-conditional; non-blocking P1
**Notes:** Do not fail merge gate when PG IT skipped without Docker.

---

## Docs & milestone closeout

| Option | Description | Selected |
|--------|-------------|----------|
| Matrix yaml only | Operators lack interpretation guidance | |
| Matrix + test-harness.md + AGENTS.md + REQUIREMENTS/ROADMAP/MILESTONES/STATE (recommended) | ROADMAP success criteria #3–#4 | ✓ |
| Full v2.1 milestone archive in same phase | Heavier; optional follow-on | |

**Auto-selected:** Regenerate test-feature-matrix.md; extend test-harness.md and AGENTS.md; update planning state files
**Notes:** Full `milestones/v2.1-*` archive is planner discretion (D-16).

---

## Out of scope

| Option | Description | Selected |
|--------|-------------|----------|
| Expand matrix to all console flows | TEST-V2 deferred | ✓ (reject) |
| Re-implement Phase 12–16 proofs | Wiring only unless broken linkage found | ✓ (reject) |
| Combined RBAC + multi-JVM row | Future hardening | ✓ (defer) |

**Auto-selected:** Strict TEST-09 wiring + closeout docs only

---

## Claude's Discretion

- Exact row status after first harness run
- Optional `ConsoleUdfAuthorizationFilterTest` in RBAC linked_tests
- Plan wave split and optional RETROSPECTIVE note

## Deferred Ideas

- P0 promotion of v2.1 proof rows — future milestone
- Surefire skip-detection hardening in verify-harness.ps1
- Full v2.1 milestone archive tree
- TEST-V2 exhaustive matrix
- Combined multi-JVM + RBAC proof
