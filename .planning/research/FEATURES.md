# Feature Research

**Domain:** Synthetic-data platform hardening (Template V2 / datasource / harness)
**Researched:** 2026-07-25
**Confidence:** HIGH

## Feature Landscape

Hardening milestone — capabilities below are **proof, reliability, and documentation** closures on already-shipped v2.0 surfaces. Product features already shipped (managed catalog, snap hot-reload, streaming/upsert, dialects, 15-row P0 harness) are table stakes that must **not** be rebuilt.

### Already Shipped (Do Not Rebuild)

| Capability | Where it lives | v2.0 proof bar |
|------------|----------------|----------------|
| Managed datasource catalog | `data-generator-datasource`, `DataSourceConfigService`, console DS APIs | `ManagedJdbcCatalogSinkE2eIT` (in-process `TemplateV2Runner`) |
| Snapshot hot-reload + `snap:` execute routing | `ExecutionSnapshotConnectionCatalog`, `DefaultRuntimeJdbcEndpointResolver` | Phase 07.1 + `JdbcSnapshotExecutePathIT` |
| Streaming CSV/JSON + JDBC upsert | Calcite chunked/streaming pipelines, dialect SQL builder | P0 rows `v2-streaming-*`, `v2-jdbc-upsert-pg-mysql` |
| Five-engine dialects (DM/KB/HG/PG/CK) | `JdbcSinkSqlBuilder`, console presets | P0 dialect rows; Kingbase evidence pack |
| P0 harness merge gate | `.planning/test-matrix.yaml` → `verify-harness.ps1` → `harness-verify.yml` | 15 P0 rows, `p0.pass=true` |

### Table Stakes (Users Expect These — This Milestone)

For a **hardening** release, “users” are operators and maintainers who already trust v2.0 features and now expect the weak spots closed. Missing these = milestone feels incomplete even though product features exist.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| HTTP execute-path proof (managed catalog / dialect) | In-process runner proof leaves a gap: operators run via `/task/run` or console `/api/templates/{id}/run`, not `TemplateV2Runner` in a test. Audit flow #1 accepted this limit. | MEDIUM | Extend or complement `ManagedJdbcCatalogSinkE2eIT` with MockMvc/`@SpringBootTest` HTTP enqueue → async completion → sink `COUNT(*)` (or dialect preset path). Depends on existing `TaskController.run`, `TaskExecutionService`, catalog save, snap binding. Prefer H2/embedded-first; dialect proof can reuse PG/Kingbase-proxy patterns already in CI. |
| Dameng live IT green path + Nyquist hygiene | P0 Dameng is MERGE-unit only; `ChunkedPipelineDamengUpsertIT` is a placeholder that `Assumptions.abort`s even when `-Ddm.it=true`. Operators with licensed DM need a documented green path. Nyquist gaps (phases 07, 07.1, 08) erode planning confidence. | MEDIUM–HIGH (live IT); LOW (Nyquist docs) | Wire real DM when image/host available via `DamengTestSupport`; keep opt-in (`-Ddm.it=true` / `DG_DM_IT=true`) so default CI stays green. Nyquist = backfill `*-VALIDATION.md` / `nyquist_compliant` for 07/07.1/08 — hygiene, not new product code. |
| Resolver ownership documentation (no merge) | Dual resolvers (`JdbcCatalogResolver` catalog-side vs `DefaultRuntimeJdbcEndpointResolver` V2 execute-path) already documented in class Javadoc; maintainers still risk “which one do I call?” and accidental consolidation PRs. | LOW | Docs + call-site inventory only. Explicitly **no** code merge of the two resolvers in v2.1. |
| Multi-JVM worker E2E one path | Distributed coordinator/worker exists (`DistributedJobService`, `DataGeneratorWorkerApplication`, `docs/staging-distributed-deployment.md`, `e2e-distributed-podman.ps1`) but was deferred as DIST-01 from v2.0 requirements. One harness-linked path closes the “does multi-JVM actually work?” doubt. | MEDIUM | Pick **one** happy path (coordinator enqueue → worker lease → SUCCESS). Reuse existing scripts/ITs; link a matrix row (likely P1). Do not expand to full AC-1..AC-7 staging checklist. |
| RBAC testable enable (default off) | `ConsoleSecurityProperties.enabled=false` by default; staging/e2e YAML and `ConsoleAuthorizationIntegrationIT` already exist. Operators need a clear “turn on + verify” path without breaking local/dev defaults. | LOW–MEDIUM | Document staging/e2e enablement; ensure filter IT remains green when enabled; optionally matrix/P1 linkage. **Do not** flip default to on. |
| Focused P1 harness expansion | Phase 10 made new RW/dialect rows P0-only; new proof paths (HTTP catalog, distributed, RBAC enable) need tracked non-blocking coverage. | LOW–MEDIUM | Add focused P1 rows for new proof paths only. P0 remains the merge gate (15 rows); do not inflate P0 with optional/live-only evidence. |

### Differentiators (Competitive Advantage)

Hardening milestones rarely differentiate on features; differentiation here is **trust posture**.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| HTTP + catalog + dialect journey evidence | Competitors often stop at unit/SQL-builder proof; end-to-end HTTP→managed id→rows (and dialect-aware sink) is operator-credible. | MEDIUM | Builds on existing catalog + TaskController; evidence packaging matters more than new runtime code. |
| Opt-in Dameng live IT with documented green path | Domestic JDBC engines are a product differentiator already shipped; a **reproducible** live path (not placeholder abort) deepens that advantage without forcing licensed drivers into CI. | MEDIUM–HIGH | Depends on image/host availability; keep MERGE unit as P0 bar. |
| Dual-resolver ownership clarity | Transparent ownership docs reduce regression risk vs silent dual paths — unusual honesty for brownfield platforms. | LOW | Docs-only differentiator; merge would be a larger refactor milestone. |
| Harness-linked multi-JVM one-path | Many platforms claim distributed workers; few gate a minimal dual-JVM path into the coverage matrix. | MEDIUM | Scripts exist; harness linkage is the gap. |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Full JDBC resolver consolidation / merge | “Duplicate code smells bad”; one class feels cleaner. | Touches catalog module + execute-path snap semantics; high blast radius; easy to break DS-03 mid-flight isolation. Explicitly deferred. | Ownership docs + call-site inventory; revisit merge in a dedicated refactor milestone. |
| Default-on console RBAC | “Security should be on by default.” | Breaks local/dev and existing e2e assumptions; header-RBAC is intranet-oriented (`X-Console-Role`). | Keep `enabled=false`; document staging enable + keep `ConsoleAuthorizationIntegrationIT`. |
| Full staging distributed AC matrix (AC-1..AC-7) as milestone DoD | Completeness of `docs/staging-distributed-deployment.md`. | Scope explosion: lease steal, cancel races, heartbeat, requeue — weeks of flaky multi-JVM work. | **One** happy-path E2E + harness link; leave full AC as staging runbook. |
| Promote Dameng live IT to P0 merge gate | “Dialect parity with PG.” | Licensed driver / container cost; CI flake; placeholder currently aborts even when flagged. | Keep P0 = MERGE unit; live IT opt-in + docs; optional P1 row when green path exists. |
| ORCH / Redis / S3 / HTTP connectors | Natural “what’s next” after datasource platform. | Opens a new major feature lane; contradicts hardening goal. | Defer beyond v2.1 (already in PROJECT Out of Scope). |
| Exhaustive Nyquist / 100% UI matrix | Planning purity / coverage theater. | High cost, low operator value vs closing HTTP and distributed proof gaps. | Targeted VALIDATION backfill for 07/07.1/08; focused P1 only. |
| Rebuild managed catalog / streaming / dialects | “While we’re hardening, rewrite X.” | Reworks table stakes; burns the milestone. | Treat as existing; only add proof/docs around them. |

## Feature Dependencies

```
v2.0 Managed catalog + snap routing (shipped)
    └──requires──> HTTP execute-path proof (managed catalog / dialect)
                       └──enhances──> Focused P1 harness rows

v2.0 Dialect SQL builder + DamengTestSupport (shipped)
    └──requires──> Dameng live IT green path
                       └──enhances──> Optional P1 dameng-live row (not P0)

Dual resolvers (shipped coexistence)
    └──requires──> Resolver ownership docs + call-site inventory
                       └──conflicts──> Full resolver merge (anti-feature)

DistributedJobService + WorkerApplication + e2e-distributed-podman (shipped)
    └──requires──> Multi-JVM worker E2E one path
                       └──enhances──> Focused P1 harness row

ConsoleSecurityProperties + ConsoleAuthorizationFilter (shipped, default off)
    └──requires──> RBAC testable enable path + staging/e2e docs
                       └──conflicts──> Default-on RBAC

Nyquist VALIDATION gaps (07 / 07.1 / 08)
    └──enhances──> Dameng/docs hygiene workstream (same “closeout” theme)

P0 harness gate (shipped, 15 rows)
    └──conflicts──> Inflating P0 with live-only / multi-JVM flaky paths
    └──enhances──> Focused P1 expansion for new proofs
```

### Dependency Notes

- **HTTP execute-path proof requires managed catalog + TaskController:** Proof must go through HTTP enqueue/completion, not only `TemplateV2Runner`, or the accepted Phase 11 limit remains open.
- **Dameng live IT requires dialect builder + gate flag:** MERGE unit stays P0; live path is additive and opt-in.
- **Resolver docs conflict with merge:** Same milestone must not both document dual ownership and consolidate classes.
- **Multi-JVM one path requires existing distributed stack:** Prefer linking `e2e-distributed-podman.ps1` / dual-JVM smoke over inventing a third topology.
- **RBAC testable enable conflicts with default-on:** Enable path and docs only; default stays false.
- **P1 expansion enhances new proofs without raising merge bar:** P0 remains 15-row gate; new rows start as P1 (or stay unlinked until stable).

## MVP Definition

### Launch With (v2.1 hardening MVP)

Minimum closures that make the weak-spot milestone credible.

- [ ] HTTP execute-path proof for managed catalog (and at least one dialect-aware journey) — closes Phase 11 accepted HTTP limit
- [ ] Resolver ownership documentation + call-site inventory — no code merge
- [ ] RBAC testable enable path with default remaining off — staging/e2e docs + existing IT green
- [ ] Focused P1 harness rows for the new proof paths — P0 gate unchanged
- [ ] Nyquist hygiene backfill for phases 07 / 07.1 / 08 — documentation compliance

### Add After Core Proofs Green (still v2.1 if capacity)

- [ ] Dameng live IT non-placeholder green path (opt-in) — when DM image/host available
- [ ] Multi-JVM worker E2E **one** path linked into harness (P1) — reuse Podman/smoke scripts

### Future Consideration (beyond v2.1)

- [ ] Full JDBC resolver consolidation — dedicated refactor milestone after inventory
- [ ] Default-on RBAC or richer authN — product/security decision, not hardening
- [ ] Full distributed AC-1..AC-7 as CI gate — staging ops, not merge gate
- [ ] ORCH, Redis/S3/HTTP connectors — deferred feature lane
- [ ] Dameng live IT promoted to P0 — only if CI cost/flake acceptable

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| HTTP execute-path proof (managed catalog / dialect) | HIGH | MEDIUM | P1 |
| Resolver ownership docs (no merge) | HIGH (maintainer) | LOW | P1 |
| RBAC testable enable (default off) | MEDIUM–HIGH | LOW | P1 |
| Focused P1 harness expansion | HIGH (trust) | LOW–MEDIUM | P1 |
| Nyquist hygiene (07 / 07.1 / 08) | MEDIUM | LOW | P1 |
| Dameng live IT green path | MEDIUM–HIGH | MEDIUM–HIGH | P2 |
| Multi-JVM worker E2E one path | MEDIUM–HIGH | MEDIUM | P2 |
| Full resolver merge | LOW now | HIGH | P3 (anti-feature this milestone) |
| Default-on RBAC | LOW now | MEDIUM | P3 (anti-feature) |
| ORCH / new connectors | HIGH later | HIGH | P3 (out of scope) |

**Priority key:**
- P1: Must have for v2.1 launch / DoD
- P2: Should have in v2.1 when capacity/env allows
- P3: Explicitly deferred or anti-feature for this milestone

## Competitor Feature Analysis

Hardening is compared less to external vendors than to **internal proof depth** after a feature-heavy milestone.

| Capability | Typical ETL / synthetic platforms | This repo today | v2.1 approach |
|------------|-----------------------------------|-----------------|---------------|
| Managed connection → run proof | Often UI + unit only | In-process `ManagedJdbcCatalogSinkE2eIT` | Add HTTP `/task/run` (or console run) evidence |
| Dialect coverage | Unit SQL or one cloud engine | Five engines; DM MERGE-unit P0 | Keep P0 unit; document live DM green path |
| Connection resolver design | Usually single resolver | Dual by design (catalog vs execute + snap) | Document ownership; defer merge |
| Distributed workers | Often aspirational / manual | Code + Podman scripts exist; DIST-01 deferred | One harness-linked path |
| Console RBAC | Often always-on IdP | Header RBAC, default off | Testable enable; stay opt-in |
| Regression harness | Ad-hoc CI | 15-row P0 merge gate | Focused P1 for new proofs; do not bloat P0 |

## Sources

- `.planning/PROJECT.md` — v2.1 Hardening & Weak-Spot Closure goals and out-of-scope
- `.planning/milestones/v2.0-MILESTONE-AUDIT.md` — tech debt: HTTP limit, Dameng opt-in, dual resolvers, Nyquist partial
- `.planning/milestones/v2.0-REQUIREMENTS.md` — shipped DS/RW/TEST; deferred DIST-01 / ORCH / RW-07
- Code: `DefaultRuntimeJdbcEndpointResolver`, `JdbcCatalogResolver`, `ManagedJdbcCatalogSinkE2eIT`, `ChunkedPipelineDamengUpsertIT`, `DamengTestSupport`, `ConsoleSecurityProperties`, `ConsoleAuthorizationIntegrationIT`, `TaskController`, `DistributedJobService`
- Docs: `docs/test-harness.md`, `docs/staging-distributed-deployment.md`, scripts `verify-harness.ps1`, `e2e-distributed-podman.ps1`
- Matrix: `.planning/test-matrix.yaml` (15 P0; existing P1 console-api / transform-sql / reader-jdbc rows)

---
*Feature research for: data-generator v2.1 Hardening & Weak-Spot Closure*
*Researched: 2026-07-25*
)
