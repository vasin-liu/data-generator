# Phase 8: RW Streaming & Upsert - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-27
**Phase:** 8-RW Streaming & Upsert
**Areas discussed:** Execution mode, CSV/JSON shape, JDBC upsert, Run report, Verification, Playwright scope, Console UX, Backward compatibility, Operator documentation
**Discussion language:** Chinese (user-facing); artifacts in English

---

## Execution Mode (RW-01 / RW-02)

| Option | Description | Selected |
|--------|-------------|----------|
| Explicit CHUNKED/STREAMING only | Large files require operator-set mode; IN_MEMORY unchanged | ✓ |
| Auto-promote by file size | Engine switches mode when file exceeds threshold | |
| CHUNKED only | STREAMING deferred | |
| Both CHUNKED + STREAMING first-class | Operators choose; both documented | ✓ |

**User's choice:** Explicit mode; both CHUNKED and STREAMING; default chunk 1000; per-chunk SQL transforms; IN_MEMORY large file = warn only; 10 MB / 100k row fixture bar; extend `scenario-e-streaming-jdbc` test path.
**Notes:** Transforms are batch-through-SQL per chunk, not cross-chunk streaming SQL.

---

## CSV/JSON Shape (RW-01 / RW-02)

| Option | Description | Selected |
|--------|-------------|----------|
| NDJSON + JSON array | Streaming element parser for both formats | ✓ |
| NDJSON only | Array files out of scope | |
| UTF-8 + optional BOM | No new encodings Phase 8 | ✓ |
| Per-chunk sink flush | Streaming writer to disk | ✓ |
| Excel streaming | In scope | |

**User's choice:** NDJSON + array; UTF-8/BOM; per-chunk flush; Excel out of scope; path semantics unchanged.

---

## JDBC Upsert (RW-03)

| Option | Description | Selected |
|--------|-------------|----------|
| `options.upsert` + `upsertKeys` | YAML-driven upsert configuration | ✓ |
| PG + MySQL only | Other dialects Phase 9 | ✓ |
| Fail-fast on invalid keys | Publish + run block | ✓ |
| Silent skip on conflict | | |

**User's choice:** PG/MySQL dialect SQL; fail-fast when keys missing or invalid.

---

## Run Report (RW-04)

| Option | Description | Selected |
|--------|-------------|----------|
| Per-sink metrics + errors in JSON + Job UI | rowsRead/Written/Upserted/Skipped + errors | ✓ |
| Mid-run progress in UI | Live counters during run | |
| Final summary only | Report after terminal state | ✓ |

**User's choice:** Full per-sink metrics; errors in RunReport and Job center; no mid-run progress Phase 8.

---

## Verification

| Option | Description | Selected |
|--------|-------------|----------|
| Playwright required | E2E not optional | ✓ |
| Embedded-only | Skip Playwright | |
| OOM IT -Xmx256m + 10 MB fixture | Prove no heap blow-up | ✓ |
| Upsert IT: H2 + Testcontainers PG/MySQL | Dialect smoke + real engines | ✓ |

**User's choice:** Playwright required; OOM and upsert IT bars as above.

---

## Playwright Scope (supplemental)

| Option | Description | Selected |
|--------|-------------|----------|
| 3 core scenarios | CSV CHUNKED success, PG upsert re-run, failed job errors | |
| 5+ scenarios | Above + JSON NDJSON, MySQL upsert, IN_MEMORY warn toast | ✓ |
| Claude discretion | Minimum one per RW requirement | |

**User's choice:** Five-plus scenarios covering streaming, both upsert dialects, failure report, and IN_MEMORY warn UX.

---

## Console Template UX (supplemental)

| Option | Description | Selected |
|--------|-------------|----------|
| Form hints + publish validation | executionPolicy hints; upsertKeys required when upsert true | ✓ |
| Docs link only | No console changes | |
| Full sink wizard | Multi-step upsert key picker | |

**User's choice:** Hints and validation without full wizard.

---

## Backward Compatibility (supplemental)

| Option | Description | Selected |
|--------|-------------|----------|
| Zero break | Small IN_MEMORY templates unchanged; large file warn only | ✓ |
| Template version bump | Migration comments for large files | |

**User's choice:** Zero behavior change for existing small-file templates.

---

## Operator Documentation (supplemental)

| Option | Description | Selected |
|--------|-------------|----------|
| Update jdbc-sink-guide + new streaming CSV/JSON guide + AGENTS verify | Full operator doc set | ✓ |
| Minimal inline only | ROADMAP/CONTEXT only | |
| Claude discretion | At least jdbc + streaming guides | |

**User's choice:** Update existing jdbc sink guide; add streaming CSV/JSON guide; AGENTS verify commands when UAT scripts exist.

---

## Claude's Discretion

- JSON format YAML knob vs auto-detection
- Internal pipeline class choice (Streaming vs Chunked) for CSV/JSON adapters
- RunReportVO field naming for backward-compatible console types
- Playwright spec file naming and UAT script layout

## Deferred Ideas

- ClickHouse and domestic JDBC upsert — Phase 9
- Excel streaming, mid-run progress UI, auto mode promotion, cross-chunk SQL
- Harness P0 matrix rows — Phase 10
- Non-UTF-8 encodings
