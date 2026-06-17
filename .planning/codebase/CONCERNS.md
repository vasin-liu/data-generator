# Codebase Concerns

**Generated:** 2026-06-17  
**Scope:** Technical debt, known issues, fragile areas, security posture, performance risks, and test gaps across the `data-generator` monorepo (Java 25 / Maven / Spring Boot 4.x / React console).

This document captures risks observed from source inspection, upgrade-phase notes, gap matrices, and marker searches (`TODO`/`FIXME`/`HACK`, conditional tests, SNAPSHOT dependencies). It is intended for planning and prioritization—not as a live issue tracker.

---

## Severity overview

| Area | Risk | Primary impact |
|------|------|----------------|
| Console RBAC default-off | **High** (deployment) | Open `/api/**` unless staging profile applied |
| Internal Kafka/ES starters on Boot 3 APIs | **High** (runtime) | Messaging/search autoconfig may break on Boot 4 classpath drift |
| `spring-ai` SNAPSHOT + Ollama-conditional tests | **Medium** | Non-reproducible builds; AI paths under-tested in CI |
| Calcite in-memory SQL evaluator (`CalciteRowTransformer`) | **Medium** | SQL semantics diverge from real engines; large change blast radius |
| HTTP Nexus / SCM URLs | **Medium** (supply chain) | MITM risk; Maven HTTP blocker workaround required locally |
| Docker/Ollama-gated integration tests | **Medium** (CI signal) | Green `mvn test` without exercising MySQL cursors, PostGIS, live AI |
| V2 capability gaps (sinks, materialization, distributed) | **Medium** (product) | Operators hit documented limits; partial parity vs V1 expectations |
| Large monolithic classes (engine + console) | **Low–Medium** (maintainability) | Harder refactors; regression risk in hot paths |

---

## Build, dependencies, and release hygiene

### SNAPSHOT and milestone coordinates

- **Project version** is `3.0.0-SNAPSHOT` in `pom.xml` (`revision` / `data-generator.version`). All modules inherit this; releases cannot be immutable until a release version is cut and tagged.
- **`spring-ai.version` is `1.0.0-SNAPSHOT`** in `pom.xml`, imported via `spring-ai-bom`. Impact: upstream API/behavior can shift between builds; `AGENTS.md` explicitly flags snapshot upgrades as team-aligned work. **Fix:** Pin to a released Spring AI GA when the team's Boot 4 stack stabilizes; run full `.\mvnw-jdk25.ps1 test` and AI verification scripts after bump.
- **`eclipse-collections.version` is `12.0.0.M3`** (milestone, not final) in `pom.xml`. Impact: pre-release API surface. **Fix:** Move to GA `12.0.0` when available and regression-test collection-heavy paths.
- Sample plugin `samples/template-v2-pf4j-plugin/pom.xml` also uses `1.0.0-SNAPSHOT` / `3.0.0-SNAPSHOT`; keep sample versions aligned with parent BOM when publishing examples.

### Internal Gensokyo stack version

- `gensokyo.version` is **`2.7.0`** in `pom.xml`. Phase 8 docs (`docs/phase-8-messaging-search-alignment/README.md`) document that **`org.gensokyo.boot:kafka-spring-boot-starter:2.7.0`** and **`es-spring-boot-starter:2.7.0`** still depend on Boot 3 property classes (`KafkaProperties`, `ElasticsearchProperties`) removed in Boot 4. Wrapper modules were aligned (`KafkaWriterConfig.java`, `EsWriterConfig.java`, `EsReaderConfig.java`), but **runtime autoconfig from those starters remains a compatibility boundary**. Impact: Kafka/ES datasource wiring may fail silently or at context refresh when starter internals are exercised. **Fix:** Upgrade or replace internal starters for Boot 4; add an integration test that loads real starter autoconfig (not only wrapper `@AutoConfiguration` tests).

### HTTP artifact repository and SCM

- Root `pom.xml` `distributionManagement` and `scm` use **`http://172.25.20.192:8081/...`** and **`http://172.25.21.141/...`**. Impact: cleartext artifact and source resolution on internal networks; requires `.mvn/settings-jdk25.xml` mirror hack to disable Maven's default HTTP blocker (`maven-default-http-blocker` with `blocked=false`). **Fix:** Migrate Nexus and Git to HTTPS; remove HTTP blocker workaround once mirrors are TLS-only.

### JDK 25 toolchain noise

- `docs/jdk25-upgrade.md` notes remaining **`sun.misc.Unsafe::staticFieldBase`** warning from Maven's embedded Guice—not project code. Mitigations exist (`.mvn/jvm.config`, Surefire JVM args, Mockito javaagent, ClassGraph upgrade). **Residual risk:** future JDK removals may require another Surefire/Maven bump. Track Maven wrapper (`3.9.11`) and Guice updates.

---

## Security-sensitive areas

### Console RBAC is disabled by default

- `ConsoleSecurityProperties.java` sets **`enabled = false`** by default with comment "trusted intranet."
- `ConsoleAuthorizationFilter.java` skips all `/api/**` checks when disabled; enforcement uses header `X-Console-Role` only—no session/JWT/OAuth.
- RBAC is **on** only in profile overlays: `application-staging.yaml`, `application-e2e.yaml`, `application-e2e-rbac.yaml`, `application-distributed-staging.yaml`, `application-e2e-distributed.yaml`.
- **Impact:** Production deployments that omit `spring.profiles.active=staging` (or explicit `data.generator.console-security.enabled=true`) expose template publish, task run, secret admin, and audit APIs without role checks. **Fix:** Document secure-by-default for external-facing installs; consider flipping default to `enabled=true` with a `dev` profile for local open access; add startup warning when security is disabled and `server.port` is not loopback.

### Secret resolution and governance

- `SecretResolver.java` + `PassthroughSecretResolver.java`: default bean throws on `resolveRequired(secretRef)` unless a real registry is wired (`CoreConfig.java` `@ConditionalOnMissingBean`).
- `TemplateGovernanceSupport.java` rejects plaintext `apiKey` and JDBC `password` when governance flags demand `apiKeySecretRef` / `passwordSecretRef`.
- `OpenAiCompatibleRuntimeBridge.java` sends `Authorization: Bearer` from resolved API key; tests in `OpenAiCompatibleRuntimeBridgeTests.java` cover secret-ref path.
- **Impact:** Slim/test contexts and misconfigured production beans fail at runtime rather than at template publish—good for safety, but operators may see late failures. **Fix:** Ensure console publish validation always runs governance checks; integration test that `PassthroughSecretResolver` is never the bean in staging profile.

### JavaScript transform sandbox

- `docs/js-transform-sandbox.md` documents GraalJS limits (no IO/network/host access, 65 536-byte script cap, 5 000 ms default timeout per row). `JsTransformFactory` enforces this.
- **Residual risk:** CPU exhaustion via tight loops within timeout; per-row invocation cost at scale. **Fix:** Add global row-budget metrics; consider stricter default timeout for untrusted tenants.

### Legacy REST surface

- `docs/superpowers/specs/2026-05-26-react-console-embedded-design.md` marks legacy REST (`/template`, `/task`, `/datasource`, …) as **deprecated but retained** for scripts/CI. **Impact:** Dual API surface increases attack/review scope. **Fix:** Track deprecation timeline; restrict legacy paths when console security is enabled.

---

## Performance and scalability fragile areas

### Calcite SQL execution model (skeleton, in-memory)

- `docs/calcite-implementation-status.md`: Calcite used for **parse + validate**; row execution is **repository-local in-memory evaluation** via `CalciteRowTransformer.java` (~1 375 lines).
- **Impact:** Large joins/aggregations materialize full row lists in heap; semantics may differ from PostgreSQL/MySQL for edge cases. Any SQL feature added must be implemented twice (validator + evaluator). **Fix:** Treat as known ceiling; add execution-shape guards/classifier limits; long-term migrate hot paths to push-down or cap row counts in `executionPolicy`.

### Source/sink streaming gaps

- Documented limitations in `docs/calcite-implementation-status.md`: CSV/JSON sources lack streaming; JDBC sink has **no upsert/merge**; CSV/JSON sinks lack streaming writer mode.
- **Impact:** Large file templates OOM or slow; idempotent reload scenarios need manual truncate. **Fix:** Prioritize streaming adapters behind existing `CsvParser`/`JsonParser` injection points; add dialect-specific upsert options per `docs/superpowers/specs/2026-06-07-v1-to-v2-native-gap-matrix.md` (Postgres/MySQL partial, ClickHouse plain insert only).

### Geospatial memory and accuracy

- `docs/geospatial-phase1-usage.md`: GeoJSON read **fully into memory** (~50 MB soft limit); WGS84 only; engineering accuracy (not survey-grade).
- **Impact:** Large boundaries blow heap; lat/lon linear interpolation on long edges. **Fix:** Stream features or tile boundaries; document max fixture size in validator.

### AI quota and rate limiting

- `AiQuotaService.java` (~562 lines) coordinates platform/tenant/template quotas, JDBC-backed daily usage, webhooks (`AiQuotaWebhookNotifier.java`).
- `JdbcAiRateLimiter.java` provides distributed coordination—complexity concentrated in service module.
- **Impact:** Hot-path DB contention under multi-JVM AI load; webhook failures may not block generation. **Fix:** Load-test quota increments; add metrics on quota check latency and webhook delivery.

### Console frontend complexity

- Largest TS modules: `data-generator-console-web/src/app/editor/workflowUtils.ts` (833 lines), `DatasourcesPage.tsx` (828), `draftUtils.ts` (714), `SourceFieldsForm.tsx` (659), `api/types.ts` (598).
- **Impact:** Editor regressions hard to spot; type drift between UI and REST DTOs. **Fix:** Split workflow/draft utilities by domain; generate OpenAPI types if feasible.

---

## Product / V2 engine gaps (documented partials)

From `docs/superpowers/specs/2026-06-07-v1-to-v2-native-gap-matrix.md`:

| Gap | Status | Notes |
|-----|--------|-------|
| Materialization `ONCE`/`ORDERED`/`LIMIT` vs V1 | **Partial** | Byte-for-byte V1 RNG explicitly N/A |
| JDBC dialect writers (COPY/bulk) | **Partial** | Upsert for PG/MySQL only |
| Parallel multi-sink | **Partial** | Independent targets only; see `sinkExecutionPolicy.parallelSinks` |
| PF4J custom transform | **Partial** | Sample in `samples/template-v2-pf4j-plugin/`; console scenarios `GF-JS`, `GF-SP` |
| Phase D inter-template pipeline | **Defer** | API reservations only |
| C2 multi-node distributed | **Defer** | After C Done + B-lite Done |

**Legacy runtime reads:** `V1SourcePolicyAdapter.java` and `SourcePolicyVO` still supported at runtime when old YAML present though console removed `SourcePolicyVO` authoring—migration footgun.

**V1 retirement remnants:** `DataGeneratorProperties.java` deprecated `v1-execution` flags; `Templates.java` still distinguishes V1 vs V2 parse errors; `TemplateEditorServiceV1RejectionTests.java` guards editor paths. **Fix:** Remove dead V1 parse branches once inventory confirms zero V1 templates in production metadata.

---

## Elasticsearch and Kafka technical debt

- `ElasticsearchWriter.java` uses legacy **`RestHighLevelClient`** stack (`elasticsearch.version` **7.17.8** in `pom.xml`). Phase 8 notes deprecated API usage at compile time.
- Fast tests mock `RestClient` (`ElasticsearchSinkFactoryTests`); HTTP embedded server tests exist (`EmbeddedElasticsearchHttpSupport`, `ElasticsearchRowSinkAdapterHttpEmbeddedTests`).
- **Impact:** ES 8+ client migration blocked; security/TLS defaults differ across client generations. **Fix:** Dedicated phase to adopt Java API Client; re-run `TemplateV2RunnerElasticsearchHttpEmbeddedTests`.

---

## AI subsystem concerns

### Inline TODOs and incomplete V1 reader behavior

- `data-generator-reader/data-generator-reader-ai/src/main/java/org/gensokyo/data/reader/AiReader.java` line 43: **`//TODO 提取停止符之间的内容`** (extract content between stop sequences). Impact: V1-stage `AiReader` may pass full model output to parser when stop tokens are configured. **Fix:** Implement stop-sequence trimming in reader or delegate to V2 `ai` source path exclusively.

### Conditional live tests

- `OllamaAiRuntimeBridgeLiveIT.java` and `AiTests.java` use `Assumptions.assumeTrue(false, ...)` when Ollama unreachable on `localhost:11434` (documented in `docs/jdk25-upgrade.md`, `docs/testing-embedded-components.md`).
- **Impact:** CI green without validating real Ollama integration. **Fix:** Optional CI job with Ollama service container; keep unit tests with WireMock for HTTP bridges (`OpenAiCompatibleRuntimeBridgeTests.java`).

### Spring AI SNAPSHOT coupling

- `OllamaApi.java`, `OllamaOptions.java` are large hand-maintained client layers in `data-generator-reader-ai` (300+ lines each), partially overlapping Spring AI starter usage in `pom.xml`.
- **Fix:** Consolidate on one client stack when Spring AI GA aligns with Boot 4.

---

## Database dialect debt

- `data-generator-common/data-generator-database-core/src/main/java/org/gensokyo/data/database/dialect/impl/DB2105DialectImpl.java` line 25: **`//TODO: 根据DatabaseMetaData获取数据库厂商名和版本号`**. Impact: DB2 dialect detection may be static/incorrect for mixed versions. **Fix:** Implement `DatabaseMetaData`-driven product version routing in dialect factory.

---

## Test coverage gaps and CI blind spots

| Gap | Location | When skipped | Mitigation |
|-----|----------|--------------|------------|
| Ollama live AI | `OllamaAiRuntimeBridgeLiveIT.java`, `AiTests.java` | No daemon on :11434 | WireMock + optional Ollama job |
| MySQL chunked cursors | `ChunkedPipelineMySqlContainerTests.java` | `DockerTestSupport.dockerAvailable()` false | Document Docker requirement for release gate |
| Postgres chunked | `ChunkedPipelinePostgresContainerTests.java` | Same | Same |
| PostGIS containers | `PostGisQueryRowSourceContainerTests.java` | Docker unavailable | Same |
| Playwright E2E | `data-generator-console-web/e2e/specs/*.spec.ts` (27 specs) | `-SkipPlaywright` on all `scripts/verify-ai-p*.ps1`, `verify-execution-reliability.ps1` | Run `scripts/verify-console.ps1` on release candidates |
| H2 vs production JDBC | `docs/testing-embedded-components.md` | Always (by design for speed) | Require Testcontainers slice before major JDBC changes |

### Oversized test suites (fragile maintenance)

- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/TemplateV2RunnerTests.java` — **~1 633 lines**. Impact: long runtimes; difficult failure diagnosis. **Fix:** Split by scenario family (sources, transforms, sinks, workflow).

- `Pf4jTemplateSubtypeIntegrationTests.java` (~617 lines), `TemplateV2SupportTests.java` (~596 lines) in `data-generator-service` — similar split recommendation.

---

## Packaging and platform edge cases

- `docs/spring-boot-4-upgrade-plan.md` Phase 10: Windows **`tar` extraction** may fail for some non-ASCII archive entries during local validation; package creation itself succeeds.
- Spring Boot repackaging **skipped** in service module; distribution via `maven-assembly-plugin` (`assembly.xml`). **Impact:** Operators must follow documented assembly layout, not fat-jar defaults. **Fix:** Keep `docs/phase-10-packaging-runtime-validation/README.md` smoke steps in release checklist.

- Phase 9 open item: **Actuator / health probe defaults** not re-checked if Actuator is introduced later (`docs/spring-boot-4-upgrade-plan.md`).

---

## Operator console and governance

- Governance flags in `DataGeneratorProperties.java`: `rejectPlaintextPasswordsInTemplates` (default **true**), `require-published-for-task-run` enabled in staging profile.
- **Impact:** Dev/default profile may allow draft runs and looser credential rules—intentional for local dev, risky if copied to production YAML. **Fix:** Profile matrix in `docs/operator-console-usage.md` with explicit production checklist.

- Audit sanitization in `AuditDetailSanitizer.java`—ensure new sensitive fields are redacted when extending console APIs.

---

## Recommended prioritization (2026-06-17)

1. **Secure deployments:** Enforce console RBAC + governance in production profiles; audit default `application.yaml`.
2. **Boot 4 messaging/search:** Upgrade `gensokyo.boot` Kafka/ES starters or excise Boot 3 property dependencies.
3. **Dependency pinning:** Release `3.0.0`, Spring AI GA, Eclipse Collections GA.
4. **CI depth:** Docker-gated Testcontainers job + Playwright on release branch; optional Ollama job.
5. **Engine ceilings:** Document/enforce row limits on in-memory SQL; streaming sources/sinks roadmap.
6. **Maintainability:** Split `CalciteRowTransformer.java` and mega test classes; reduce `workflowUtils.ts`/`DatasourcesPage.tsx` size.
7. **Cleanup:** Resolve `AiReader.java` stop-sequence TODO; DB2 dialect metadata TODO; ES client modernization.

---

## Source markers searched

- **Java/TS `TODO`/`FIXME`/`HACK`:** 2 hits in Java (`AiReader.java`, `DB2105DialectImpl.java`); **0** in `data-generator-console-web` `*.ts`/`*.tsx`.
- **`@Disabled`:** none found in Java tests (conditional skip via `Assumptions` instead).

---

*This file should be refreshed after major phases (Boot upgrade, V2 gap packs, security hardening) or quarterly debt reviews.*
