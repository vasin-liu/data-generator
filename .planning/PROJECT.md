# data-generator

## What This Is

**data-generator** is a Java/Maven synthetic-data platform for operators who define Template V2 pipelines (sources → transforms → sinks) and run them via a Spring Boot service with an embedded React operator console. The codebase is a mature brownfield monorepo (`3.0.0-SNAPSHOT`) with pluggable readers, writers, iterators, stages, Calcite SQL transforms, GraalJS/Velocity/SpEL scripting, and AI-assisted generation.

This GSD milestone extends the platform with **operator-uploadable multi-form UDFs**, **stronger transform operators and SQL capabilities**, and a **quality-first automated test harness** whose coverage ramps in phases. Reader/Writer expansion, datasource abstraction overhaul, and template-level orchestration are explicitly deferred.

## Core Value

Operators can define, extend, and trust data-generation pipelines: register custom logic (UDFs), apply rich transforms, and verify behavior through an automated test harness before shipping.

## Requirements

### Validated

- ✓ Template V2 Calcite pipeline execution — existing (`data-generator-calcite/`, `TemplateV2Runner`)
- ✓ Pluggable readers/writers/iterators/stages via AutoService + factory dispatch — existing (`data-generator-core/`)
- ✓ Spring Boot REST service + embedded React console — existing (`data-generator-service/`, `data-generator-console-web/`)
- ✓ JDBC, Kafka, Elasticsearch, and file (CSV/JSON/Excel) sinks — existing (writer modules)
- ✓ GraalJS, Velocity, SpEL scripting transforms — existing (`data-generator-scripter-*`)
- ✓ PF4J plugin framework for transformers — existing (`data-generator-calcite/`, sample in `samples/template-v2-pf4j-plugin/`)
- ✓ AI-assisted generation (Ollama/OpenAI-compatible) — existing (`data-generator-reader-ai/`, console AI APIs)
- ✓ Operator console for templates, jobs, datasources, schedules — existing (`/api/*`, `/console/*`)
- ✓ V1 template execution retired at runtime — existing (`TaskController` rejects V1-only templates)

### Active

- [ ] Unified UDF registry supporting Java/PF4J plugins, script UDFs (GraalJS/Velocity), and SQL/Calcite UDFs with governance
- [ ] Console/API flow to upload, version, and publish UDF artifacts for use in templates
- [ ] Expanded built-in transform operators and SQL expression/type enhancements for Template V2
- [ ] Test harness foundation: feature matrix, synthetic data simulation fixtures, CI entry points
- [ ] Phased coverage ramp: embedded module tests, API slices, Playwright console paths tied to the matrix

### Out of Scope

- New Reader/Writer integrations (e.g. additional brokers, warehouses) — deferred to v2; current adapters sufficient for this milestone
- Datasource abstraction refactor (dynamic maintenance/extensibility overhaul) — deferred; avoid destabilizing `dynamic-datasource` and console datasource APIs mid-milestone
- Template-level orchestration (multi-template workflow, visual DAG, cross-template scheduling) — deferred to later version per product decision
- Exhaustive 100% UI/control coverage in v1 — harness-first with phased targets, not big-bang matrix completion
- Greenfield rewrite or Template V1 revival — V2 is the sole execution path

## Context

Brownfield codebase mapped 2026-06-17 in `.planning/codebase/`. Key architecture: multi-module Maven reactor, `DataGeneratorApplication` entry, Calcite-backed V2 runtime, console embedded at `classpath:/static/console/`.

Known pressure points (from `.planning/codebase/CONCERNS.md`):

- Console RBAC disabled by default (`ConsoleSecurityProperties.enabled = false`)
- Internal Gensokyo Kafka/ES starters on Boot 3 APIs vs Boot 4 runtime
- `spring-ai` SNAPSHOT and Ollama-gated tests
- HTTP internal Nexus/SCM URLs

User's six original goals were prioritized for v1 as: **UDF**, **testing harness**, **transform operators**, with **orchestration**, **Reader/Writer**, and **datasource refactor** deferred.

## Constraints

- **Tech stack**: Java 25, Maven, Spring Boot 4.x — no framework downgrade; build via `mvnw-jdk25.ps1` / `.mvn/settings-jdk25.xml`
- **Compatibility**: Extend existing Template V2 schema and PF4J/GraalJS paths; avoid breaking published console APIs without migration notes
- **Security**: UDF upload must be governed (no arbitrary classpath execution without sandbox/review hooks); align with `SecretResolver` / template governance patterns
- **Testing**: Prefer embedded-first tests (H2, embedded Kafka, WireMock) per `docs/testing-embedded-components.md`; Playwright/Podman for console E2E where live infra needed
- **Documentation**: Public Java APIs follow repository copyright/Javadoc rules (`.cursor/rules/java-copyright-class-javadoc.mdc`)

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| v1 delivery: quality-first (test harness before feature breadth) | User selected quality-priority scope; reduces regression risk while adding UDF/transform surface | — Pending |
| UDF v1: multi-form (Java PF4J + script + SQL) with unified registry | User wants uploadable custom logic across existing transform stacks | — Pending |
| Transform v1: operators + SQL enhancement (not flow-control DAG) | User scoped transform to built-in operators/SQL; branch/retry/parallel deferred | — Pending |
| Defer template orchestration to later version | Explicit user decision during questioning | — Pending |
| Defer Reader/Writer and datasource abstraction to v2 | Focus v1 on extensibility (UDF) + quality + transforms | — Pending |
| Test acceptance: harness + phased coverage (not exhaustive matrix in v1) | Pragmatic ramp vs big-bang UI coverage | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):

1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):

1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-06-17 after initialization*
