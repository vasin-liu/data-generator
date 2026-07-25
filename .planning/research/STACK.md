# Stack Research

**Domain:** synthetic-data platform hardening (v2.1 Hardening & Weak-Spot Closure)
**Researched:** 2026-07-25
**Confidence:** HIGH

> Brownfield subsequent-milestone research. Prefer the shipped v1.0/v2.0 stack. Recommend **no new production libraries**. Changes are proof paths, docs, opt-in IT wiring, PowerShell/Maven harness linkage, and Playwright header toggles already sketched in-repo.

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Java | 25 | Runtime / language | Enforced by `maven-enforcer-plugin`; no language bump for hardening |
| Maven Wrapper + `mvnw-jdk25.ps1` | 3.6.3+ / JDK 25 helper | Builds & focused `-pl … -am test` | Existing CI/local contract; Nexus via `.mvn/settings-jdk25.xml` |
| Spring Boot | 4.0.5 | Service, REST `/task/*` + `/api/*`, worker profile | Keep BOM pinned; hardening uses existing controllers/filters |
| Template V2 Calcite runtime | module `data-generator-calcite` | Pipeline execute path for managed DS + dialects | Already backs `TemplateV2Runner` and JDBC sinks |
| `data-generator-datasource` catalog | shipped v2.0 | Managed `dataSourceId` / `snap:` resolution | HTTP proof must exercise catalog → run, not invent a parallel catalog |
| Distributed worker entry | `DataGeneratorWorkerApplication` + `distributed-worker` profile | Multi-JVM lease/execute | Already packaged (`service.env.example` `DG_SERVICE_ROLE=worker`) |
| Header RBAC | `ConsoleSecurityProperties` (`data.generator.console-security.*`) | Opt-in console authorization | Staging overlay already enables it; default remains `enabled=false` |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Spring MockMvc / `@SpringBootTest` | Boot test starter | HTTP evidence for `/task/run` or console run API | Preferred over Playwright for managed-catalog + dialect execute-path proof (fast, deterministic) |
| H2 (test profile) | 2.2.224 (existing) | Metadata + managed JDBC sink in HTTP IT | `application-phase7-test.yaml` (`server.port: 0`) |
| JUnit 5 + Assumptions / `@EnabledIf` | Boot BOM | Gate Dameng live IT | Keep `DamengTestSupport` (`-Ddm.it=true` / `DG_DM_IT=true`) |
| `dm-jdbc` | 1.8 (root `dm.version`) | Dameng driver on classpath / jdbc-bundled | Already managed; do **not** add a second Dameng driver artifact |
| Testcontainers | 1.20.6 | Optional live DM (or PG-proxy patterns for other dialects) | Only if a licensed/available DM image or JDBC host is wired into `ChunkedPipelineDamengUpsertIT` (today a placeholder abort) |
| Playwright | ^1.49.1 (`@playwright/test`) | Console RBAC enable-path + staging/e2e docs | Reuse `e2e/helpers/api.ts` (`DG_E2E_GOVERNANCE_STAGING`, `X-Console-Role`) |
| Podman + `scripts/e2e-podman.ps1` | existing | Containerized console UAT | Optional for RBAC staging docs; not required for Maven HTTP IT |
| PowerShell verify scripts | `scripts/verify-*.ps1` | Slice orchestration + harness | Add a focused v2.1 verify script if needed; keep `verify-harness.ps1` as P0 merge gate |
| Harness matrix | `.planning/test-matrix.yaml` | P1 row linkage | Add focused P1 rows only; do **not** expand P0 for opt-in Dameng/multi-JVM |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| `.\mvnw-jdk25.ps1` | JDK 25-bound Maven | Quote `-Dsurefire.argLine` / `-Ddm.it=true` on PowerShell |
| `.\scripts\verify-harness.ps1` | Canonical merge gate (P0) | P1 failures tracked, non-blocking |
| `docs/testing-embedded-components.md` | Embedded-first norms | Extend with Dameng opt-in + multi-JVM recipe; no new infra product |
| `application-staging.yaml` | Documented RBAC-on overlay | Port 8080; `console-security.enabled=true`; headers `X-Console-Role` / `X-Console-Actor` |
| `application-distributed-*.yaml` | Coordinator / worker / staging distributed overlays | Pair with `DG_MAIN_CLASS=…WorkerApplication` for real second JVM |
| GSD Nyquist / VALIDATION.md | Phase hygiene backfill | Docs process only — not a library |
| CodeGraph (optional) | Call-site inventory speed | Audit noted missing `.codegraph/`; optional `codegraph init` — **not** a milestone dependency |

## Installation

```powershell
# No new Maven/npm packages required for v2.1 hardening defaults.

# Existing build/test (unchanged)
.\mvnw-jdk25.ps1 -v
.\mvnw-jdk25.ps1 -pl data-generator-service -am test

# Dameng live IT (opt-in only — needs DM JDBC host/image)
.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test `
  -Dtest=ChunkedPipelineDamengUpsertIT `
  -Ddm.it=true `
  -Dsurefire.failIfNoSpecifiedTests=false

# Or: $env:DG_DM_IT = 'true' then same Maven slice

# Harness (P0 gate; P1 tracked)
.\scripts\verify-harness.ps1

# Console E2E with staging RBAC headers (existing helper)
$env:DG_E2E_GOVERNANCE_STAGING = 'true'
# then Playwright / e2e-podman against staging profile
```

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| MockMvc / `@SpringBootTest` HTTP IT for `/task/run` or `/api/templates/{id}/run` | Playwright-only execute proof | Use Playwright only for UI journeys; Maven HTTP IT closes the v2.0 accepted “in-process runner” gap cheaper |
| Shared JDBC metadata DB + spawn second JVM (`DataGeneratorWorkerApplication`) | Keep single-JVM `Distributed*IntegrationTests` only | Single-JVM stays as fast unit/IT; multi-JVM E2E is the **one** new proof path — scripted ProcessBuilder/PowerShell, not a new framework |
| `DamengTestSupport` + Testcontainers/GenericContainer or external JDBC URL | Always-on CI Dameng container | Licensed image / cost; keep opt-in; document green path when host available |
| Header RBAC (`ConsoleSecurityProperties`) | Spring Security OAuth2 / Keycloak / JWT | Only if product later requires IdP; **out of scope** for v2.1 (default-off + testable enable) |
| Docs + call-site inventory for dual JDBC resolvers | Code merge of `JdbcCatalogResolver` + `DefaultRuntimeJdbcEndpointResolver` | Explicitly deferred; docs-only this milestone |
| Focused P1 matrix rows | Promote Dameng live / multi-JVM to P0 | Would break merge gate when DM/host unavailable; keep P0 = always-green MERGE unit + existing 15 rows |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| **New production libraries** (RestAssured, TestNG, Gatling, Micronaut test, etc.) | Adds dependency surface for proof-only work | Boot `MockMvc`, JUnit 5, existing AssertJ |
| **Spring Security / OAuth2 / session auth for console** | Product decision: RBAC stays header opt-in; default-off for local/dev | `ConsoleAuthorizationFilter` + staging profile + Playwright `consoleRoleHeaders` |
| **Default-on `console-security.enabled` in base `application.yaml`** | Breaks trusted-intranet local/dev and most e2e defaults | Keep default `false`; document staging/`DG_E2E_GOVERNANCE_STAGING` |
| **Kubernetes / Docker Compose / Nomad as the only multi-JVM story** | Overkill for one E2E; assembly already has coordinator/worker roles | Two local JVMs (or Podman compose **optional**) sharing H2 file/JDBC metadata |
| **Redis / message bus for worker coordination** | Jobs already lease via JDBC (`DistributedJobService`); no Redis module | Existing distributed job tables + worker poller |
| **Second Dameng JDBC driver / unofficial forks** | `dm-jdbc` 1.8 already in BOM + jdbc-bundled | Wire IT against that driver |
| **Mandatory Dameng in default CI / P0 `linked_tests`** | Licensed image; today’s IT is still a placeholder abort | Documented `-Ddm.it=true` green path; P0 stays MERGE SQL unit |
| **Full JDBC resolver consolidation / new DI framework** | Explicitly out of scope | Ownership docs + call-site inventory only |
| **Net-new connectors (Redis/S3/HTTP) or Boot upgrades** | Feature lane, not hardening | Stay on Boot 4.0.5 + existing adapters |
| **WireMock for `/task/run` execute path** | Would stub away the catalog→run wiring under test | Real Spring MVC + H2 managed DS (extend `ManagedJdbcCatalogSinkE2eIT` style to HTTP) |
| **New frontend auth libraries** | Headers already set in `src/api/client.ts` / `e2e/helpers/api.ts` | Env-gated header injection only |
| **Expanding P0 to 16+ rows for every proof** | Merge gate must stay always-green | Focused **P1** rows for HTTP run, multi-JVM, RBAC-on, Dameng opt-in |

## Stack Patterns by Variant

**If proving managed-catalog / dialect journeys through HTTP:**
- Use `@SpringBootTest` + `MockMvc` (or `TestRestTemplate` on random port) against `TaskController` / console run API with `application-phase7-test.yaml`.
- Seed managed DS via `DataSourceConfigService.save` (same as `ManagedJdbcCatalogSinkE2eIT`), then HTTP-trigger run — **not** only in-process `TemplateV2Runner`.
- Because: closes v2.0 accepted limit (“not HTTP `/task/run`”) without new HTTP test stacks.

**If Dameng live IT:**
- Keep gate `DamengTestSupport.damengItEnabled()`; replace placeholder `Assumptions.abort` with real MERGE upsert against GenericContainer **or** operator-supplied JDBC URL.
- Document exact env vars, image/host, and `verify-*.ps1` / Maven one-liner in `docs/` (template-v2 JDBC sink guide already mentions the flag).
- Because: licensed DM cannot be default CI; hygiene = green **when enabled**, not always-on.

**If one multi-JVM distributed worker E2E:**
- Process A: coordinator (`DataGeneratorApplication`, distributed coordinator profile / enqueue via `/task/run`).
- Process B: `DataGeneratorWorkerApplication` (`distributed-worker` profile, `DG_SERVICE_ROLE=worker`).
- Shared: file H2 or other JDBC metadata URL both JVMs can see (in-memory H2 will **not** work across processes).
- Link as **P1** harness row + PowerShell verify script; keep existing single-JVM `DistributedSplitRoleIntegrationTests` for fast feedback.
- Because: stack already has worker entry + lease runner; gap is process boundary proof, not a new distributed framework.

**If console RBAC testable enable path:**
- Maven: extend patterns from `ConsoleAuthorizationIntegrationIT` (`console-security.enabled=true` + filter on MockMvc).
- Playwright/Podman: `DG_E2E_GOVERNANCE_STAGING=true` → `e2e/helpers/api.ts` sends `X-Console-Role`.
- Docs: staging profile + e2e env flags; **do not** flip base default.
- Because: enable path already exists; v2.1 is docs + coverage, not a new security stack.

**If Nyquist / dual-resolver “stack” work:**
- Markdown VALIDATION.md backfill + ownership/call-site inventory (CodeGraph optional).
- Because: zero runtime dependency changes.

**If focused P1 harness expansion:**
- Edit `.planning/test-matrix.yaml` only; regenerate matrix doc; leave `harness-verify.yml` P0 gate unchanged.
- Because: P1 is tracked non-blocking by design (`docs/test-harness.md`).

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| Spring Boot 4.0.5 | Java 25, Maven 3.6.3+ | Do not downgrade for tests |
| Testcontainers 1.20.6 | Docker/Podman engine | Same pin as PG/MySQL/ClickHouse ITs; Dameng image availability is the constraint, not TC version |
| `dm-jdbc` 1.8 | Dameng server matching driver docs | Opt-in IT only |
| Playwright 1.49.x | Node ≥ 22 | Header helpers already in tree |
| H2 mem URL | Single JVM only | Multi-JVM worker E2E needs `jdbc:h2:file:…` (or shared external JDBC), not `jdbc:h2:mem:` |
| `console-security.enabled=false` (default) | Local e2e without headers | Staging / `DG_E2E_GOVERNANCE_STAGING` require headers |
| Internal Gensokyo Kafka/ES starters | Boot 3 APIs vs Boot 4 | Known pressure; **out of v2.1 scope** — do not “fix” via new messaging stack |

## Sources

- `.planning/PROJECT.md` — v2.1 goals, constraints, dual-resolver / Dameng / RBAC decisions
- `.planning/milestones/v2.0-MILESTONE-AUDIT.md` — accepted debt (HTTP run gap, Dameng opt-in, Nyquist, dual resolvers)
- `.planning/codebase/STACK.md` — inventory of shipped versions (Boot 4.0.5, TC 1.20.6, Playwright 1.49, dm-jdbc)
- `docs/testing-embedded-components.md` — embedded-first H2 / Testcontainers / Kafka norms
- `docs/test-harness.md` — P0 vs P1 semantics; Dameng P0 = MERGE unit only
- `DamengTestSupport` / `ChunkedPipelineDamengUpsertIT` — opt-in gate; IT still placeholder abort
- `ConsoleSecurityProperties` + `application-staging.yaml` + `ConsoleAuthorizationIntegrationIT` + `e2e/helpers/api.ts` — RBAC enable path
- `DataGeneratorWorkerApplication` + `DistributedSplitRoleIntegrationTests` + `service.env.example` — distributed worker stack
- `ManagedJdbcCatalogSinkE2eIT` — in-process managed-catalog proof to extend via HTTP
- AGENTS.md verify-script catalog — reuse PowerShell slices; no new CI product

---
*Stack research for: synthetic-data platform hardening (v2.1)*
*Researched: 2026-07-25*
