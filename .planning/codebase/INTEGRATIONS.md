# External Integrations

**Analysis Date:** 2026-06-17

## APIs & External Services

### AI / LLM Providers

The platform generates data via **AI sources** (`data-generator-reader-ai/`) using pluggable runtime bridges wired in `data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java`.

| Provider type | Integration | Client / bridge | Auth pattern |
|---------------|-------------|-----------------|--------------|
| **OLLAMA** | Local or remote Ollama HTTP API | `OllamaAiRuntimeBridge` → `OllamaApi` / `OllamaChatClient` | Base URL in template `provider.options`; no API key by default |
| **OPENAI** | OpenAI-compatible chat-completions | `OpenAiCompatibleRuntimeBridge` (Spring WebClient) | `provider.options.apiKey` or `apiKeySecretRef` resolved via `SecretResolver` |
| **AZURE_OPENAI** | Azure deployment endpoint | Same `OpenAiCompatibleRuntimeBridge` | `apiKeySecretRef` + deployment URL in `source.api` |
| **Composite** | Routes by provider type | `CompositeAiRuntimeBridge` | Delegates to registered bridges |

**Console APIs (catalog & governance):**
- `GET /api/console/ai/catalog` — Providers, parsers, prompt templates (`ConsoleAiCatalogController.java`)
- `GET /api/console/ai/usage` — Platform AI usage rollup
- `GET /api/console/ai/pricing` — Model pricing table
- `GET /api/console/ai/quota` — Quota status (platform + scoped)

**Rate limiting & quotas** (`data.generator.ai-runtime.*` in `DataGeneratorProperties.java`):
- In-memory or **JDBC-coordinated** rate limits (`JdbcAiRateLimiter`, table `ai_rate_limit_state`) when `distributedRateLimitEnabled=true`
- Daily quotas: calls, tokens, estimated USD cost; scoped overrides for `PROVIDER`, `TEMPLATE`, `TENANT`
- Live Ollama tests skip when `localhost:11434` unreachable (`OllamaAiRuntimeBridgeLiveIT`, `docs/jdk25-upgrade.md`)

**Spring AI:** BOM and `spring-ai-ollama-spring-boot-starter` are **commented out** in root `pom.xml`; runtime uses custom bridge implementations instead.

### Elasticsearch

- **Client:** `elasticsearch-rest-client` **7.17.8** (`data-generator-dependencies/pom.xml`)
- **Modules:** `data-generator-reader-elasticsearch/`, `data-generator-writer-elasticsearch/`
- **Usage:** Template V2 sources/sinks read indices and bulk-index documents
- **Connection:** Per-template or console-registered cluster config (hosts, index names); not hard-coded in core code
- **Tests:** In-process HTTP server stub (`EmbeddedElasticsearchHttpSupport`) — no external ES required in unit tests (`docs/testing-embedded-components.md`)

### Apache Kafka

- **Client:** `spring-kafka` (`data-generator-common/data-generator-core/pom.xml`)
- **Module:** `data-generator-writer-kafka/`
- **Multi-cluster config:** `spring.kafka.multiple` in `data-generator-service/src/main/resources/application.yaml`
  - `primary: command_kafka`
  - `clusters.<name>.bootstrap-servers` (example host in default yaml — override per environment)
  - Commented SASL/PLAIN producer properties for secured clusters
- **Template usage:** Kafka row sink in Calcite runtime (`KafkaRowSinkAdapter`, `TemplateV2RunnerKafkaEmbeddedTests`)
- **Tests:** `EmbeddedKafkaKraftBroker` via `EmbeddedKafkaTestSupport` (`data-generator-calcite/src/test/`)

### Internal Maven / Artifact Repositories

- **Nexus releases:** `http://172.25.20.192:8081/nexus/content/repositories/releases/` (`pom.xml` `<distributionManagement>`, `<repositories>`)
- **Nexus snapshots:** `http://172.25.20.192:8081/nexus/content/repositories/snapshots/`
- **Maven Central + Spring repos** — Public dependency resolution
- **Settings:** `.mvn/settings-jdk25.xml` — Disables Maven HTTP blocker for internal Nexus; sets `localRepository` path
- **Note:** HTTP (not HTTPS) internal URLs; builds require repo-local settings on developer machines

### Source Control (reference only)

- SCM URLs in root `pom.xml` point to internal Git host (`172.25.21.141/gensokyo/data-generator`) — not used at runtime.

---

## Data Storage

### Metadata database (platform state)

- **Engine:** **H2** by default — file-backed `jdbc:h2:file:../db/data-generator` (`application.yaml`)
- **Schema:** `classpath:db/schema.sql` on datasource init
- **ORM:** Spring Data JPA + MyBatis-Plus entities/repositories in `data-generator-service/`
- **Stores:** Templates (draft/published), task executions, run reports, cron schedules, audit events, **secret registry**, AI quota/rate-limit state, distributed job queue
- **Dev console:** Spring H2 console at `/h2` when enabled (`spring.h2.console.enabled=true`)
- **Tests:** In-memory H2 with `MODE=PostgreSQL` (`application-phase7-test.yaml`, `application-e2e-rbac.yaml`)

### Business / pipeline JDBC datasources

Dynamic named pools under `spring.datasource.dynamic.datasource.*` (`application.yaml`). Operators also register datasources via console `GET/POST /api/datasources` (`ConsoleAuthorizationFilter` — mutations require `DATASOURCE_ADMIN` when RBAC enabled).

**Supported drivers (versions in root `pom.xml`, bundled under `jdbc-bundled/` in package):**

| Engine | JDBC driver artifact | Typical URL pattern |
|--------|---------------------|---------------------|
| MySQL | `mysql-connector-j` 8.0.31 | `jdbc:mysql://host:3306/db` |
| PostgreSQL | `postgresql` 42.5.1 | `jdbc:postgresql://host:5432/db` |
| ClickHouse | `clickhouse-jdbc` 0.6.2 | `jdbc:clickhouse://host:8123/db` |
| Dameng (DM) | `dm-jdbc` 1.8 | `jdbc:dm://host:port?schema=...` |
| Kingbase8/9 | `kingbase8` | `jdbc:kingbase8://...` |
| HighGo | `HgdbJdbc` 6.2.4 | PostgreSQL-compatible |
| H2 | `h2` 2.2.224 | Embedded / file / mem |

**Pool:** Alibaba Druid on all pools; validation query `SELECT 1`.

**Template references:** Inline `dataSource` blocks or `dataSourceId` pointing to console-registered names; passwords via `passwordSecretRef` when governance enabled (`data.generator.governance.reject-plaintext-passwords-in-templates`).

### File / object I/O (no cloud SDK)

- **Local filesystem** — CSV, Excel, JSON readers/writers; console upload API `POST /api/console/uploads/file` (`ConsoleUploadController.java`)
- **Geo files** — GeoJSON, shapefile via GeoTools (`data-generator-geo/`, `data-generator-iterator-geo/`)
- **No S3/Azure Blob/GCS** client dependencies in root BOM

### Caching

- **No Redis integration** in current codebase (`docs/testing-embedded-components.md` — Redis module not present)
- In-memory metadata cache sized by `data.generator.meta-cache-maximum-size` (`DataGeneratorProperties`)

### Distributed execution coordination

- **Shared metadata DB** — Coordinator enqueues jobs; workers claim leases (`application-distributed-coordinator.yaml`, `application-distributed-worker.yaml`)
- Config: `data.generator.distributed.*` (enabled, worker-id, lease-seconds, heartbeat, max-attempts, requeue-on-failure)
- Worker entry: `DataGeneratorWorkerApplication` + `DG_SERVICE_ROLE=worker` (`service.env.example`)
- **No separate message broker** required for distributed mode (DB-backed queue)

---

## Authentication & Identity

### Console RBAC (header-based, not OAuth/JWT)

- **Implementation:** `ConsoleAuthorizationFilter` + `ConsoleSecurityProperties` (`data-generator-service/src/main/java/org/gensokyo/data/security/`)
- **Enable:** `data.generator.console-security.enabled=true` (default **false** in dev; **true** in `application-staging.yaml`, `application-e2e-rbac.yaml`)
- **Headers:**
  - `X-Console-Role` (configurable via `role-header`) — Maps to `ConsoleRole` enum (VIEWER, EDITOR, OPERATOR, ADMIN, …)
  - `X-Console-Actor` (optional, `actor-header`) — Audit actor identity
- **Scope:** All `/api/**` requests when enabled; legacy `/template/`, `/task/` paths are outside this filter
- **Ingress pattern:** Production/staging expects reverse proxy or API gateway to inject role headers; console provides in-app role picker for staging (`docs/operator-console-usage.md`)
- **No OAuth2, SAML, LDAP, or session cookies** in codebase

### Secret registry (credential storage)

- **API:** `/api/secrets` — `ConsoleSecretController.java` (list names, upsert, delete; values never returned on GET)
- **Storage:** JPA entity `SecretEntryPO` in metadata H2/DB (`SecretService.java`, `SecretEntryRepository`)
- **Resolution:** `SecretResolver` interface (`data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/secret/SecretResolver.java`)
  - JDBC: `passwordSecretRef` on inline datasources
  - AI: `apiKeySecretRef` on OpenAI/Azure providers (`OpenAiCompatibleRuntimeBridge.java`)
- **RBAC:** Secret mutations require `SECRET_ADMIN` permission (`ConsoleAuthorizationFilter`)
- **Governance:** Plaintext passwords in templates rejected when `data.generator.governance.reject-plaintext-passwords-in-templates=true`
- **No `.env` files** in repository; packaged deployments use `conf/service.env` (from `service.env.example`) for JVM/port/profile overrides, not application secrets

### Kafka / Elasticsearch auth

- **Kafka:** SASL config supported via Spring Kafka producer properties (commented example in `application.yaml` — `security.protocol`, `sasl.mechanism`, `sasl.jaas.config`)
- **Elasticsearch:** Per-cluster HTTP basic auth typically configured in template/console datasource records (adapter layer in reader/writer modules)

---

## Monitoring & Observability

**Error tracking / APM:** No Sentry, Datadog, or OpenTelemetry dependencies in root POM.

**Logging:**
- **Logback** via Spring Boot (`logback-spring.xml`)
- Rolling files: `../logs/${spring.application.name}_all.log`, `_error.log`
- Console appender threshold INFO; `%X{TRACE_ID}` in pattern

**Health:**
- `HealthController` — `/healthz` endpoint (used by `service.env.example` keepalive scripts)

**Run reports & metrics:**
- Task execution metrics, AI call tracing (`AiCallMetric`, `RunReportCollector`) persisted with job records
- Console job detail surfaces AI usage tables

**Audit:**
- `GET /api/console/audit` — Template publish and governance events (requires `AUDIT_READ` when RBAC on)

---

## CI/CD & Deployment

### CI pipeline (GitHub Actions)

| Workflow | File | Trigger | Stack |
|----------|------|---------|-------|
| Maven test | `.github/workflows/maven-test.yml` | push/PR to `master`, `main`, `feature-*` | JDK 25 Temurin, Maven cache, builds `data-generator-console-web` then full `mvn test` |
| Console verify | `.github/workflows/console-verify.yml` | path-filtered push/PR on console/service paths | JDK 25 + Node 22 + Podman + PowerShell; `scripts/verify-console.ps1` |

**Local verification scripts** (`scripts/`):
- `verify-console.ps1` — Unit + frontend build + Podman Playwright E2E
- `verify-ai-p1.ps1` … `verify-ai-p10.ps1` — AI feature slices
- `e2e-podman.ps1` — Containerized E2E against packaged service
- `mvnw-jdk25.ps1` — Windows JDK 25 + settings wrapper

### Deployment / hosting model

- **Artifact:** Maven assembly archive (tar.gz/zip), not container-first in repo (Podman used for **test** environments only)
- **Process model:** Shell/PowerShell starters in `data-generator-service/script/`; `DG_DAEMON`, `DG_HEAP_*`, `DG_SPRING_PROFILES_ACTIVE` from `service.env.example`
- **Static console:** Served from embedded `classpath:static/console/` at `/console/*` (`ConsoleWebConfig` in service module)
- **Reverse proxy:** Expected for TLS termination and RBAC header injection in staging/production (documented in `docs/operator-console-usage.md`, not implemented in-app)

### Artifact publishing

- **distributionManagement** in `pom.xml` → internal Nexus releases/snapshots (HTTP)
- Coordinates: `org.gensokyo.data.generator:*:3.0.0-SNAPSHOT`

---

## Environment Configuration

### Development

| Concern | Configuration source |
|---------|---------------------|
| Service port | `server.port` in `application.yaml` (9876) |
| Metadata DB | H2 file `../db/data-generator` or mem in tests |
| Business DBs | Optional entries in `application.yaml` (environment-specific; many samples commented) |
| Kafka | `spring.kafka.multiple.clusters.*` — point to dev broker or rely on embedded tests |
| AI (Ollama) | Default `http://localhost:11434` in templates/tests when live ITs run |
| Console dev | Vite proxy `/api` → backend (`vite.config.ts`); no secrets file required |
| Maven | `.mvn/settings-jdk25.xml` for Nexus HTTP + local repo path |

**Secrets location:** Console secret API → metadata DB; **no committed `.env`** (confirmed: zero `.env*` files in repo). Do not commit real passwords from sample `application.yaml` entries.

### Staging

- Profile: `spring.profiles.active=staging` (`application-staging.yaml`)
- Port **8080**, RBAC enabled, publish governance on, schedules enabled
- Packaged under `conf/application-staging.yaml` in assembly (`assembly.xml`)
- Smoke: `scripts/staging-blite-smoke.ps1`

### Production (expected pattern)

- Override `conf/application.yaml` or use profile-specific yaml in `conf/`
- `conf/service.env` for JVM, port, `DG_SPRING_PROFILES_ACTIVE`, worker role
- JDBC/Kafka/ES endpoints and credentials via dynamic datasource config + secret registry (not plaintext in templates)
- RBAC headers injected at ingress (`X-Console-Role`, `X-Console-Actor`)
- Logs under `../logs/` relative to package root

### E2E / test profiles

| Profile file | Purpose |
|--------------|---------|
| `application-phase7-test.yaml` | `@SpringBootTest` H2 slice |
| `application-e2e.yaml` | Podman/Playwright (RBAC off) |
| `application-e2e-rbac.yaml` | RBAC-enabled E2E |
| `application-e2e-distributed.yaml` | Distributed mode E2E |

---

## Webhooks & Callbacks

### Incoming

**None** — The service does not expose inbound webhook endpoints for third-party event delivery (no Stripe-style callback handlers). Task triggers are **REST** (`POST /task/run`, console run APIs) or **internal cron** (`data.generator.schedule.*`).

### Outgoing

**AI quota notification webhooks** (shipped AI P10):
- **Implementation:** `AiQuotaWebhookNotifier.java` — fire-and-forget HTTP POST via `java.net.http.HttpClient`
- **Config:** `data.generator.ai-runtime.quota.webhooks-enabled` + `quota.webhooks[]` in `DataGeneratorProperties.AiRuntimeQuota`
- **Endpoint fields:** `url`, optional `secretHeaderName` / `secretValue` for shared-secret auth, `events` (`WARN`, `EXCEEDED`)
- **Trigger:** `AiQuotaService` on daily quota warn threshold or hard exceed
- **Behavior:** Failures swallowed — quota enforcement does not fail when webhook endpoint is unreachable
- **Tests:** `AiQuotaWebhookNotifierTests.java`, `AiQuotaServiceIntegrationTests.dispatchesWebhookWhenQuotaWarns`

**No other outgoing webhook integrations** (no Slack/Teams/PagerDuty clients in dependencies).

---

## REST API Surface (integration entry points)

### Console APIs (`/api/**`)

| Area | Base path | Controller package |
|------|-----------|-------------------|
| Templates | `/api/templates` | `api/console/ConsoleTemplate*.java` |
| Datasources | `/api/datasources` | `api/console/ConsoleDatasourceController` |
| Jobs / executions | `/api/jobs` | `api/console/ConsoleJob*.java` |
| Schedules | `/api/console/schedules` | `ConsoleScheduleController` |
| Secrets | `/api/secrets` | `ConsoleSecretController` |
| AI catalog/usage | `/api/console/ai/*` | `ConsoleAiCatalogController` |
| Audit | `/api/console/audit` | Console audit controller |
| Runtime info | `/api/console` | `ConsoleRuntimeController` |
| Uploads | `/api/console/uploads` | `ConsoleUploadController` |

### Legacy REST (pre-console)

| Path | Controller |
|------|------------|
| `/template/*` | `controller/TemplateController`, `TemplateEditorController` |
| `/task/*` | `controller/TaskController`, `TaskExecutionController` |
| `/datasource/*` | `controller/DataSourceController` |
| `/healthz` | `controller/HealthController` |
| Distributed jobs | `controller/DistributedJobController` |

Frontend consumes `/api/*` via React Query clients in `data-generator-console-web/src/`.

---

## Integration Summary Matrix

| External system | Direction | Required at runtime | Config / discovery |
|-----------------|-----------|---------------------|-------------------|
| H2 / JDBC DBs | Read/write | **Yes** (metadata); pipelines need configured sinks/sources | `spring.datasource.dynamic.*`, templates |
| Kafka | Write (primary) | Optional per template | `spring.kafka.multiple.*`, template sink |
| Elasticsearch | Read/write | Optional per template | Console datasource / template cluster config |
| Ollama / OpenAI / Azure OpenAI | Read (generate) | Optional per AI source | Template `provider` + secrets API |
| Nexus Maven | Pull (build) | Build-time only | `pom.xml` repositories |
| Quota webhook URLs | Outbound POST | Optional | `data.generator.ai-runtime.quota.webhooks` |
| Ingress RBAC headers | Inbound HTTP headers | Staging/prod when security on | `data.generator.console-security.*` |

---

*Integration audit: 2026-06-17*
*Update when adding/removing external services, JDBC drivers, or AI providers*
