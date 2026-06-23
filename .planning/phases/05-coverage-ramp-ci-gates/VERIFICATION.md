---
status: passed
phase: 05-coverage-ramp-ci-gates
verified: 2026-06-23
---

# Phase 5 Verification — Coverage Ramp & CI Gates

## Success criteria (ROADMAP)

| # | Criterion | Result |
|---|-----------|--------|
| 1 | Documented P0/P1/P2 tiers with explicit matrix completion targets | ✅ `tier` on all rows; COV-01 target in `docs/test-harness.md` |
| 2 | All P0 matrix rows pass in standard CI harness run | ✅ Harness: **P0 regression gate passed (7/7 green)** |
| 3 | Console API slice tests cover UDF and transform metadata endpoints | ✅ `ConsoleUdfControllerTest`, `ConsoleTransformCatalogControllerTest` expanded + matrix-linked |
| 4 | Contributors have documented merge criteria when P0 rows fail | ✅ `AGENTS.md` + `harness-verify.yml` P0 gate |

## Requirements

| ID | Plan | Status |
|----|------|--------|
| COV-01 | 05-01 | ✅ P0/P1/P2 tiers + documented target |
| COV-02 | 05-01 | ✅ All 7 P0 rows green |
| COV-03 | 05-02 | ✅ API slice expansion |
| COV-04 | 05-02 | ✅ Harness P0 gate + CI + AGENTS.md |

## Verification gate

```
.\scripts\verify-harness.ps1
→ P0 regression gate passed (7/7 green)
→ [SUCCESS] Harness verification passed.
→ exit 0
```

```
.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=ConsoleUdfControllerTest,ConsoleTransformCatalogControllerTest
→ BUILD SUCCESS
```

## P0 rows (all green)

- `calcite-scenario-v2`
- `udf-sql`, `udf-script`, `udf-java-plugin`
- `transform-json`, `transform-mask`, `transform-lookup`

## Notes

- `ConsoleUdfControllerTest` is new in the working tree (Phase 3 console surface); Phase 5 expanded slice assertions only.
- Uncommitted Phase 3 console implementation files remain outside this phase commit scope; harness green locally with working-tree sources.
