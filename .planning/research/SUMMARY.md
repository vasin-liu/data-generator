# Project Research Summary

**Project:** data-generator  
**Domain:** Brownfield Template V2 platform hardening (proof, reliability, docs)  
**Researched:** 2026-07-25  
**Confidence:** HIGH

## Executive Summary

v2.1 is a **hardening** milestone on a mature Java 25 / Spring Boot 4 / Template V2 stack. Research agrees: add **no new production libraries**. Close weak spots with HTTP execute-path evidence, documented Dameng opt-in live IT, dual-resolver ownership docs, one multi-JVM worker E2E, RBAC enable-path (default stays off), and focused P1 harness rows.

The highest-risk failure mode is relabeling in-process `TemplateV2Runner` tests as HTTP `/task/run` proof. Second is promoting Dameng live IT or multi-JVM into the P0 merge gate. Keep `verify-harness.ps1` P0 semantics; treat live/distributed/RBAC as P1 or documented opt-in.

## Key Findings

### Recommended Stack

Reuse Boot MockMvc/`@SpringBootTest`, existing worker profile, header RBAC, Testcontainers, Playwright, harness YAML. Details: [STACK.md](STACK.md).

**Core technologies:**
- Spring Boot 4.0.5 + MockMvc HTTP ITs — prove `/task/run` or console run without new HTTP clients
- `DataGeneratorWorkerApplication` + shared file H2/JDBC — one real second-JVM path
- `DamengTestSupport` (`-Ddm.it=true`) — opt-in live IT; MERGE unit remains default CI
- `ConsoleSecurityProperties` + staging headers — testable enable; default `enabled=false`

### Expected Features

Details: [FEATURES.md](FEATURES.md).

**Must have (table stakes for v2.1):**
- HTTP execute-path proof (managed catalog / dialect)
- Dameng live IT green path + Nyquist hygiene backfill
- Resolver ownership docs + call-site inventory (no merge)
- Multi-JVM worker E2E one path
- RBAC testable enable (default off)
- Focused P1 harness expansion

**Should have (trust differentiators):**
- Credible HTTP→managed id→rows evidence packaging
- Harness-linked multi-JVM row (P1)

**Defer (beyond v2.1):**
- Full resolver merge, default-on RBAC, ORCH, Redis/S3/HTTP connectors
- Full staging distributed AC-1..AC-7 matrix
- Dameng live as P0 merge gate

### Architecture Approach

No new modules. Proofs attach to the shared execute spine: HTTP enqueue → `TaskExecutionService` snapshot → `WorkflowRunContext.bind` → `TemplateV2Runner` (local or leased worker). Document `JdbcCatalogResolver` vs `DefaultRuntimeJdbcEndpointResolver` ownership; do not merge. Details: [ARCHITECTURE.md](ARCHITECTURE.md).

**Major components:**
1. `TaskController` / console run support — HTTP proof entry
2. `DefaultRuntimeJdbcEndpointResolver` + catalog snapshot — execute-path `snap:` authority
3. `DistributedJobService` + worker app — multi-JVM lease path
4. Harness matrix — P1 rows without P0 inflation

### Critical Pitfalls

Details: [PITFALLS.md](PITFALLS.md).

1. **In-process runner labeled as HTTP** — require MockMvc/HTTP enqueue + job/sink assert
2. **Dameng live promoted to P0/default CI** — keep opt-in; MERGE unit stays merge bar
3. **RBAC default flipped on** — profile/IT only; protect local/e2e
4. **Wrong `snap:` assumptions** — separate unbound managed-id vs bound snapshot proofs
5. **Scope creep into ORCH/connectors/full AC matrix** — one happy path each theme

## Implications for Roadmap

Continue phase numbering from **12** (v2.0 ended at 11).

### Phase 12: HTTP Execute-Path Proof
**Rationale:** Closes the largest accepted v2.0 evidence gap first; unblocks credible catalog/dialect trust.  
**Delivers:** MockMvc/`@SpringBootTest` HTTP run IT (managed catalog → SUCCESS/COUNT); optional dialect variant via existing CI-friendly engine.  
**Addresses:** HTTP execute-path proof  
**Avoids:** Relabeling `ManagedJdbcCatalogSinkE2eIT` as HTTP

### Phase 13: Dameng Live Path + Nyquist Hygiene
**Rationale:** Dialect weak spot + planning hygiene; independent of HTTP.  
**Delivers:** Documented opt-in Dameng green recipe; wire or honestly document placeholder; VALIDATION backfill for 07/07.1/08.  
**Addresses:** Dameng + Nyquist  
**Avoids:** Dameng in P0

### Phase 14: Resolver Ownership Docs
**Rationale:** Low-cost clarity; inventories call sites after HTTP proof reveals real callers.  
**Delivers:** Ownership doc + call-site inventory (catalog vs execute-path).  
**Addresses:** Resolver docs  
**Avoids:** Code merge

### Phase 15: Multi-JVM Worker E2E
**Rationale:** Depends on stable execute spine; one harness-linked happy path.  
**Delivers:** Coordinator + worker second JVM SUCCESS path; P1 matrix row; script entry.  
**Addresses:** DIST one-path  
**Avoids:** Full AC-1..AC-7 as DoD

### Phase 16: RBAC Enable Path + Docs
**Rationale:** Security weak spot without changing defaults.  
**Delivers:** Staging/e2e enable docs; IT proving filter when enabled; default remains off.  
**Addresses:** RBAC testable enable  
**Avoids:** Default-on

### Phase 17: P1 Harness Expansion + Closeout
**Rationale:** Wire new proofs into matrix; keep P0 green; milestone docs.  
**Delivers:** Focused P1 rows for HTTP/catalog, multi-JVM, RBAC enable; AGENTS/verify notes; ROADMAP closure.  
**Addresses:** P1 expansion  
**Avoids:** P0 inflation

### Phase Ordering Rationale

- HTTP proof first — highest operator-credibility gap
- Dameng/Nyquist parallelizable after or beside HTTP (low coupling)
- Resolver docs after HTTP so inventory reflects real run-path callers
- Multi-JVM after spine confidence; RBAC independent but late to avoid e2e churn
- Harness closeout last so rows link finished tests

### Research Flags

- **Phase 12:** Confirm endpoint (`/task/run` vs `/api/templates/.../run`) and async poll pattern
- **Phase 13:** Dameng host/image availability may force “documented enable + MERGE” if live IT stays placeholder
- **Phase 15:** File-based shared metadata DB vs Podman script — pick one recipe in plan-phase

Phases with standard patterns (light plan research):
- **Phase 14, 16, 17:** Docs + existing filter/IT/harness patterns

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Brownfield inventory; no new libs |
| Features | HIGH | Aligned with audit + PROJECT scope |
| Architecture | HIGH | Source-read execute spine; CodeGraph absent |
| Pitfalls | HIGH | Maps to v2.0 accepted limits |

**Overall confidence:** HIGH

### Gaps to Address

- Dameng live IT may remain environment-gated — plan must define honest done criteria if no DM host
- CodeGraph optional for call-site inventory speed — docs can use grep/`rg` if not initialized
- Multi-JVM flakiness — prefer one deterministic scripted path over broad matrix

## Sources

### Primary (HIGH confidence)
- `.planning/PROJECT.md`, `.planning/milestones/v2.0-MILESTONE-AUDIT.md`
- In-repo controllers, resolvers, `DamengTestSupport`, staging YAML, harness scripts
- `AGENTS.md` / `docs/testing-embedded-components.md`

### Secondary (MEDIUM confidence)
- Existing distributed Podman/staging docs as starting recipes

### Tertiary (LOW confidence)
- Exact Dameng container availability in CI environments — validate at plan/execute

---
*Research completed: 2026-07-25*  
*Ready for roadmap: yes*
