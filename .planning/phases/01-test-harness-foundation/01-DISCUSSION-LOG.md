# Phase 1: Test Harness Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-17
**Phase:** 1-Test Harness Foundation
**Areas discussed:** Matrix shape, Harness entry, Fixture library, Coverage summary, Playwright E2E, Matrix generation tool, CI workflow, Contributor docs, JSON schema, linked_tests format, Conditional skips, Root pom module

---

## Matrix Shape & Granularity

| Option | Description | Selected |
|--------|-------------|----------|
| Capability-first | Rows by reader/writer/transform/console capability | ✓ |
| Test-class-first | One row per test class or spec | |
| Hybrid | Capability primary, multiple linked_tests | |

| Option | Description | Selected |
|--------|-------------|----------|
| YAML source | `.planning/test-matrix.yaml` + generated docs | ✓ |
| Markdown only | Hand-maintained `docs/test-feature-matrix.md` | |
| JSON source | `.planning/test-matrix.json` | |

| Option | Description | Selected |
|--------|-------------|----------|
| Coarse v1 (~30–50 rows) | Per adapter class + core console flows | ✓ |
| Medium (~80–120 rows) | Per submodule + per verify script | |
| Fine (200+ rows) | Per test class | |

**User's choice:** Capability-first, YAML source, coarse granularity, Maven module owner tags.
**Notes:** Standard column set; console API/UI split rows; semi-auto draft from codebase maps; script generates Markdown docs.

---

## Harness Entry Script

| Option | Description | Selected |
|--------|-------------|----------|
| verify-harness.ps1 | New unified entry orchestrating verify-*.ps1 | ✓ |
| Extend verify-console.ps1 | Add matrix to existing console script | |

| Option | Description | Selected |
|--------|-------------|----------|
| Embedded fast default | Matrix-linked Maven + summary; skip Podman/Ollama/Docker | ✓ |
| Full console default | Always Podman + Playwright | |

| Option | Description | Selected |
|--------|-------------|----------|
| Matrix-linked Maven | Run only tests referenced in linked_tests | ✓ |
| Orchestrate verify-*.ps1 | Call feature-pack scripts per matrix rows | ✓ |

**User's choice:** New `verify-harness.ps1`, fast embedded default, matrix-scoped Maven, orchestrate existing scripts.

---

## Fixture Library

| Option | Description | Selected |
|--------|-------------|----------|
| New test-fixtures module | Top-level `data-generator-test-fixtures` test-jar | ✓ |
| calcite test/resources | Fixtures only in calcite module | |

| Option | Description | Selected |
|--------|-------------|----------|
| YAML + builders | Template YAML + minimal static Java helpers | ✓ |
| Both SQL and file seeds | H2 SQL + CSV/JSON for file readers | ✓ |
| One per adapter class | Align examples with coarse matrix | ✓ |
| Shared YAML for Playwright | E2E uploads same templates via API | ✓ |

**User's choice:** New module, scenario-based naming, `FixtureTemplates.load` + `H2Seed.apply` API.

---

## Coverage Summary

| Option | Description | Selected |
|--------|-------------|----------|
| JSON primary | `target/test-matrix-summary.json` | ✓ |
| Test-result-driven status | Infer covered/partial/pending from test results | ✓ |
| Explicit linked_tests | Class names + spec#test references | ✓ |
| Fail on pending (Phase 1) | No — only fail on test failures; P0 gate in Phase 5 | ✓ |

---

## Playwright E2E

| Option | Description | Selected |
|--------|-------------|----------|
| -IncludeE2e flag | E2E opt-in, not default | ✓ |
| Podman or local dual mode | -UsePodman (default) / -UseLocalService | ✓ |
| Extend existing specs | template-workflow + api.console | ✓ |
| Two UI matrix rows | template-edit and job-trigger separate | ✓ |

---

## CI & Documentation

| Option | Description | Selected |
|--------|-------------|----------|
| harness-verify.yml | New workflow for fast path | ✓ |
| PR + main trigger | Fast harness on both | ✓ |
| E2E nightly separate | Podman E2E not on every PR | ✓ |
| docs/test-harness.md + AGENTS.md | Full how-to + short entry | ✓ |

---

## JSON Schema & Module Placement

| Option | Description | Selected |
|--------|-------------|----------|
| Standard schema | generatedAt, gitCommit, totals, rows with linkedResults | ✓ |
| Simple Maven class names | e.g. ChunkedPipelineTests | ✓ |
| Playwright file#test | e2e/specs/foo.spec.ts#test title | ✓ |
| skipped-conditional | Separate from pending for Docker/Ollama skips | ✓ |
| Top-level pom module | Sibling to calcite | ✓ |

---

## Claude's Discretion

- Draft matrix generation heuristics and Maven module inference for simple class names.
- CI workflow JDK/setup details (mirror existing console-verify patterns).

## Deferred Ideas

- P0 merge gate blocking on pending rows (Phase 5)
- JaCoCo line coverage
- Exhaustive fine-grained matrix
- Full-repo mvn test as harness default
