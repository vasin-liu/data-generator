# V1 retirement boundary (orchestration / staging-free)

> **Historical (pre-2026-05-29).** This document captured the V1 retirement boundary while staging (M2) was unavailable and policy **S1** exempted W3 orchestration templates from wave-freeze dates. It remains useful as census evidence and migration context. For greenfield V2-only policy after the 2026-05-29 program, see [`docs/superpowers/specs/2026-05-29-v2-only-full-capability-design.md`](../superpowers/specs/2026-05-29-v2-only-full-capability-design.md).

Operator and product summary while **staging (M2) is unavailable**. Numeric evidence: [`reports/builtin-orchestration-census.md`](reports/builtin-orchestration-census.md) (regenerate via `BuiltinTemplateMigrationCensusTest`).

## Three buckets

| Bucket | Builtin census (2026-05-22) | Policy |
|--------|-------------------------------|--------|
| **Migratable (W1/W2)** | 57 / 59 templates **not** `COMPATIBILITY_ONLY`; 43 with `recommendedPath: spel`, 14 `sql` | May receive SQL (+ SpEL) draft/compare; promote after **M2** evidence + sign-off |
| **W3 exempt** | 2 / 59 (`COMPATIBILITY_ONLY`, `orchestration_legacy`) — PAUSE / LOG blockers | **S1:** remain on V1; **no** wave-freeze date |
| **Production unknown** | Not in classpath scan | **M2:** `inventory/refresh` + batch compare on `db-{id}` |

## What SpEL 2b solved (M1)

- Field `SCRIPT` with plain / `SPEL` language → `SpelTransformVO` in draft **and** JDBC compare (`buildDraftForCompare`).
- CI: `BuiltinClasspathTemplateRegressionTests`, `BuiltinClasspathTemplateMigrationWorkflowTests`, `StagingSimulatedPromoteWorkflowTests` (parking/11).

## What SpEL 2b did **not** solve (W3)

- **PAUSE** — thread sleep / timing semantics in iterator or field pipelines.
- **LOG** — logging side effects in iterator pipelines.
- **SHARED** — cross-row shared state (none in current builtin census).
- **JAVASCRIPT** — GraalJS script stages (none in current builtin census).

These stay `COMPATIBILITY_ONLY` per `V1TemplateMigrationAnalyzer`. Do **not** promote via migration REST without an explicit future orchestration design.

## Honest retirement narrative

1. **Wave freeze (W1/W2)** can proceed for templates **without** orchestration blockers, using M1 CI substitutes until M2 promotes production rows.
2. **Wave freeze (W3)** is **documentation-only** — maintain exemption list; no calendar freeze.
3. **P4** (`v1-execution.enabled=false`) requires M2 promote + family sign-off; W3-exempt templates may stay on V1 indefinitely under S1.

## Regenerate census

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am `
  "-Dtest=BuiltinTemplateMigrationCensusTest#writesBuiltinOrchestrationCensusReport" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Commit updated `docs/migration/reports/builtin-orchestration-census.md` when builtin templates change.

## References

- [`M2-production-catalog-handoff.md`](M2-production-catalog-handoff.md) — staging-ready production census steps
- [`compatibility-only-templates.md`](compatibility-only-templates.md)
- [`wave-freeze-schedule.md`](wave-freeze-schedule.md)
- [`retirement-readiness.md`](retirement-readiness.md)
- Spec: `docs/superpowers/specs/2026-05-22-w3-orchestration-blocking-assessment-design.md`
