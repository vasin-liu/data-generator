# Embedded-first testing

Unit and integration tests in this repository should exercise **real components in-process**, not external staging services. Prefer embedded or in-memory substitutes for infrastructure.

## Principles

| Prefer | Avoid |
|--------|--------|
| In-memory **H2** (or test-scoped JDBC URL) for SQL / JDBC iterators and sinks | Connecting to shared dev MySQL/PostgreSQL |
| **Embedded Kafka** (`spring-kafka-test` / `@EmbeddedKafka`) or Testcontainers Kafka when broker semantics matter | Mockito-only `KafkaTemplate` for integration paths |
| **Embedded Redis** (e.g. embedded-redis, Testcontainers Redis) when cache semantics matter | `localhost:6379` in CI |
| **WireMock** or in-process HTTP stubs for REST clients | Calling real third-party APIs in unit tests |
| **Console / in-memory sinks** for pipeline output assertions | Writing to production Kafka topics |
| `@SpringBootTest` + `application-phase7-test.yaml` (H2 metadata DB) for service slices | Full production `application.yaml` |

**Mocks** are still appropriate for:

- Narrow unit tests of pure logic (classification rules, comparators, YAML parsing).
- Boundaries you intentionally do not own (e.g. a single method on a huge client).
- Fail-fast tests where embedded setup would dominate runtime without adding signal.

Do **not** replace an entire pipeline or dual-run executor with a stub when the test goal is to prove V1/V2 execution parity.

## Standard service test profile

`data-generator-service/src/test/resources/application-phase7-test.yaml`:

- Spring metadata DB: `jdbc:h2:mem:data-generator-phase7` with `classpath:db/schema.sql`
- `server.port: 0` (random port)

Reference this profile from `@SpringBootTest`:

```java
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
```

## JDBC fixtures

1. Define data in an H2 database (inline URL in template YAML or dynamic datasource id).
2. Create tables in `@BeforeEach` / test body via `NamedParameterJdbcTemplate` + `DynamicDataSourceContextHolder.push(dsId)` when using dynamic datasources.
3. Use `MODE=PostgreSQL` or `MODE=MySQL` on the H2 URL when dialect-specific SQL is required.

Example: `TemplateControllerMigrationCompareTests` (number iterator dual-run with real `PipelineTemplateRunExecutor`).

## Kafka

- **Fast unit tests:** `KafkaSinkFactoryTests` uses Mockito on `KafkaTemplate` for adapter contract checks.
- **Shared broker:** `EmbeddedKafkaTestSupport` (reference-counted `EmbeddedKafkaKraftBroker`) used by adapter and runner embedded tests.
- **Embedded broker:** `KafkaRowSinkAdapterEmbeddedTests` — real `KafkaTemplate` producer/consumer.
- **Runner E2E:** `TemplateV2RunnerKafkaEmbeddedTests` — iterator → SQL → Kafka on the shared broker.
- **Writer autoconfig:** `KafkaWriterAutoConfigurationTests` stays on `ApplicationContextRunner` with mocked registry (Spring wiring only).

## Redis / Elasticsearch

- **Redis:** no Redis writer/reader module exists in this repository yet; when added, use embedded-redis or Testcontainers and avoid `localhost:6379` in CI.
- **Elasticsearch (fast unit):** `ElasticsearchSinkFactoryTests` — Mockito on `RestClient`.
- **Elasticsearch (HTTP embedded):** `EmbeddedElasticsearchHttpSupport` starts an in-process `HttpServer` for `POST /_bulk`; `ElasticsearchRowSinkAdapterHttpEmbeddedTests` and `TemplateV2RunnerElasticsearchHttpEmbeddedTests` use a real `RestClient` (no Mockito on `performRequest`).

## AI / Ollama

Tests that call Ollama remain **conditional** (skip when `localhost:11434` is unreachable). See `docs/jdk25-upgrade.md`.

## Migration workbench tests

| Test | Style |
|------|--------|
| `MigrationCompareServiceTests` | Pure unit: stub executor for classification math only |
| `TemplateControllerMigrationCompareTests` | Integration: embedded H2 + real `PipelineTemplateRunExecutor` |
| `MigrationInventoryBootstrapTests` | Integration: temp inventory file + H2 repository |
| Controller inventory/draft/promote | Integration: `application-phase7-test.yaml`, temp paths for inventory/reports |
