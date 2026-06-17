# Testing Patterns — data-generator

**Mapped:** 2026-06-17  
**Scope:** Java unit/integration tests, embedded infrastructure, operator console Playwright E2E, verify scripts  
**Primary reference:** `docs/testing-embedded-components.md`, `AGENTS.md`, `.cursor/rules/console-verify.mdc`

---

## 1. Testing philosophy: embedded-first

Unit and integration tests should exercise **real components in-process**, not shared staging services. This is documented in `docs/testing-embedded-components.md`.

| Prefer | Avoid |
|--------|--------|
| In-memory **H2** for SQL/JDBC paths | Shared dev MySQL/PostgreSQL |
| **Embedded Kafka** (`EmbeddedKafkaKraftBroker`) or Testcontainers when broker semantics matter | Mockito-only `KafkaTemplate` for integration paths |
| **Embedded HTTP** for Elasticsearch bulk API | Mockito on `RestClient.performRequest` for integration paths |
| **WireMock** / in-process stubs for REST clients | Calling real third-party APIs in unit tests |
| Console / in-memory sinks for pipeline assertions | Writing to production Kafka topics |
| `@SpringBootTest` + `application-phase7-test.yaml` | Full production `application.yaml` |

**Mocks remain appropriate for:**

- Narrow unit tests of pure logic (classifiers, comparators, YAML parsing)
- Boundaries you do not own (single method on a large client)
- Spring wiring-only checks via `ApplicationContextRunner`
- Fail-fast tests where embedded setup dominates runtime without adding signal

**Do not** replace an entire pipeline or dual-run executor with a stub when the test goal is V1/V2 execution parity.

---

## 2. Framework stack

### 2.1 Java

| Tool | Version / config | Location |
|------|------------------|----------|
| **JUnit Jupiter** | JUnit 5 | All `src/test/java` modules |
| **Mockito** | `5.17.0`, javaagent via surefire `argLine` | Root `pom.xml` `<mockito.version>` |
| **Spring Boot Test** | `4.0.5` | `data-generator-service`, writer/reader autoconfig tests |
| **Spring Kafka Test** | Embedded KRaft broker | `data-generator-calcite` Kafka integration |
| **Testcontainers** | MySQL/PostgreSQL parity tests | `ChunkedPipelineMySqlContainerTests`, etc. |
| **Maven Surefire** | `3.5.3` | Unit tests (`*Test`, `*Tests`) |
| **Maven Failsafe** | `3.5.3` configured | Available for `*IT` if bound in module POMs |

Surefire configuration (root `pom.xml`):

```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>${mockito.agentLine}</argLine>
    </configuration>
</plugin>
```

### 2.2 Operator console (frontend)

| Tool | Purpose |
|------|---------|
| **TypeScript compiler** | Static type check (`npm run verify:unit` / `build`) |
| **Playwright** | E2E UI + API smoke (`@playwright/test` ^1.49) |
| **Vite** | Production bundle embedded into service JAR |

There are **no Vitest/Jest component unit tests** in `data-generator-console-web`. All automated frontend verification flows through **tsc + Playwright**.

Config: `data-generator-console-web/playwright.config.ts`

```typescript
const baseURL = (process.env.DG_E2E_BASE_URL ?? 'http://127.0.0.1:9876/console').replace(/\/?$/, '/');

export default defineConfig({
  testDir: './e2e/specs',
  fullyParallel: false,
  workers: 1,
  timeout: 60_000,
  use: { baseURL, ...devices['Desktop Chrome'] },
});
```

---

## 3. Test layout and naming

### 3.1 Directory structure

```
<module>/src/test/java/org/gensokyo/data/<area>/...
<module>/src/test/resources/          # YAML, SQL fixtures
data-generator-console-web/e2e/
  specs/                              # Playwright test files
  helpers/                            # Shared API/UI helpers
scripts/verify-*.ps1                  # Focused verification pipelines
```

### 3.2 Class naming conventions

| Pattern | Role | Examples |
|---------|------|----------|
| `<Class>Tests` | Default unit/integration class name | `ExecutionShapeClassifierTests`, `ChunkedPipelineTests` |
| `<Class>Test` | Legacy/slice naming (console controllers) | `ConsoleJobControllerTest`, `HealthControllerTest` |
| `<Feature>IT` | Spring context integration | `ConsoleWebEndpointIT`, `V2ScenarioTemplateIT` |
| `<Feature>LiveIT` | Conditional live external dependency | `OllamaAiRuntimeBridgeLiveIT` |
| `*TestSupport` / `*Support` | Shared test infrastructure (not tests) | `EmbeddedKafkaTestSupport`, `DockerTestSupport` |

**Prescription:** use `*Tests` for new Java test classes; use `*IT` when the test boots Spring or runs multi-layer HTTP/JDBC flows.

### 3.3 Method naming

Behavior-descriptive `camelCase`, often with underscores for readability:

```java
@Test
void list_returnsExecutions() throws Exception { ... }  // ConsoleJobControllerTest

@Test
void chunkedModeWritesAllRowsInBatches() { ... }         // ChunkedPipelineTests

@Test
void singleTableSelectIsRowLocal() { ... }               // ExecutionShapeClassifierTests
```

---

## 4. Standard service test profile

File: `data-generator-service/src/test/resources/application-phase7-test.yaml`

Key properties:

- Metadata DB: `jdbc:h2:mem:data-generator-phase7;MODE=PostgreSQL;...`
- Schema init: `classpath:db/schema.sql`
- `server.port: 0` (random port for integration tests)
- Additional dynamic datasources for migration/compare scenarios

Reference from `@SpringBootTest`:

```java
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
@Import(TaskControllerApiTests.TaskApiTestConfig.class)
class TaskControllerApiTests {
```

File: `data-generator-service/src/test/java/org/gensokyo/data/controller/TaskControllerApiTests.java`

Lighter IT variant (properties only):

```java
@SpringBootTest(properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class ConsoleWebEndpointIT {
```

File: `data-generator-service/src/test/java/org/gensokyo/data/config/ConsoleWebEndpointIT.java`

---

## 5. Test patterns by layer

### 5.1 Pure unit tests (no Spring)

No annotations beyond JUnit 5. Direct instantiation and assertions.

Example: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sql/ExecutionShapeClassifierTests.java`

```java
@Test
void groupByIsMaterializationRequired() {
    ExecutionShape shape = ExecutionShapeClassifier.classify(
            "SELECT status, COUNT(*) FROM orders GROUP BY status");
    Assertions.assertEquals(ExecutionShape.MATERIALIZATION_REQUIRED, shape);
}
```

### 5.2 Mockito + standalone MockMvc (console controllers)

Isolate controller HTTP layer; mock services; attach `ConsoleApiAdvice`.

Example: `data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleJobControllerTest.java`

```java
@ExtendWith(MockitoExtension.class)
class ConsoleJobControllerTest {

    @Mock
    private TaskExecutionService taskExecutionService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ConsoleJobController(taskExecutionService, ...))
                .setControllerAdvice(new ConsoleApiAdvice())
                .build();
    }

    @Test
    void list_returnsExecutions() throws Exception {
        when(taskExecutionService.list(any(), any())).thenReturn(List.of(row));
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
```

Also used in: `ConsoleTemplateControllerTest`, `ConsoleAuthorizationFilterTest`, `AiUsageServiceTests`.

### 5.3 `@SpringBootTest` integration (service slice)

- `@Autowired` real beans (repositories, controllers, services)
- `@AfterEach` cleanup of test data (`templateRepository.deleteAll()`)
- `@TestConfiguration` + `@Import` to replace heavy dependencies with test doubles

Example pattern in `TaskControllerApiTests`: captor/stub `TemplateV2Runner` via nested `@TestConfiguration`.

### 5.4 `ApplicationContextRunner` (autoconfiguration wiring)

Validates Spring Boot conditional beans without full application boot.

Example: `data-generator-writer/data-generator-writer-kafka/src/test/java/org/gensokyo/data/writer/KafkaWriterAutoConfigurationTests.java`

```java
private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(KafkaWriterAutoConfiguration.class);

@Test
void autoConfiguresWhenKafkaWriterOnClasspath() {
    contextRunner
            .withBean(DynamicKafkaTemplateRegistry.class, () -> Mockito.mock(...))
            .run(context -> assertThat(context).hasSingleBean(KafkaWriter.class));
}
```

Similar: `EsWriterAutoConfigurationTests`, `Pf4jRuntimeConfigTests`.

### 5.5 In-process pipeline integration (calcite)

Build real `TemplateV2RuntimeRegistry` with factory implementations; run `TemplateV2Runner` against H2.

Example: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineTests.java`

- Creates H2 tables inline
- Configures `TemplateV2VO` with `ExecutionPolicyVO.mode = "CHUNKED"`
- Asserts row counts and metrics on real JDBC sink

---

## 6. Embedded infrastructure patterns

### 6.1 H2 JDBC

- Default fast path for chunked pipelines, query sources, service repositories
- Use `MODE=PostgreSQL` or `MODE=MySQL` when dialect-specific SQL matters
- Dynamic datasource tests: `DynamicDataSourceContextHolder.push(dsId)` + `NamedParameterJdbcTemplate`

### 6.2 Embedded Kafka

Shared reference-counted broker: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/EmbeddedKafkaTestSupport.java`

```java
@BeforeAll
static void startKafka() {
    EmbeddedKafkaTestSupport.acquire();
}

@AfterAll
static void stopKafka() {
    EmbeddedKafkaTestSupport.release();
}
```

Used by: `KafkaRowSinkAdapterEmbeddedTests`, `TemplateV2RunnerKafkaEmbeddedTests`.

**Fast unit contract tests** still mock `KafkaTemplate`: `KafkaSinkFactoryTests`.

### 6.3 Embedded Elasticsearch (HTTP)

`EmbeddedElasticsearchHttpSupport` starts in-process `HttpServer` for `POST /_bulk`.

Used by: `ElasticsearchRowSinkAdapterHttpEmbeddedTests`, `TemplateV2RunnerElasticsearchHttpEmbeddedTests`.

**Fast unit:** `ElasticsearchSinkFactoryTests` mocks `RestClient`.

### 6.4 Testcontainers (Docker-gated)

Helper: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/DockerTestSupport.java`

```java
public static boolean dockerAvailable() {
    try {
        return DockerClientFactory.instance().isDockerAvailable();
    } catch (RuntimeException | LinkageError ex) {
        return false;
    }
}
```

Gated tests:

```java
@EnabledIf("org.gensokyo.data.calcite.support.DockerTestSupport#dockerAvailable")
@Testcontainers
class ChunkedPipelineMySqlContainerTests {
    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withUrlParam("useCursorFetch", "true");
}
```

Files: `ChunkedPipelineMySqlContainerTests.java`, `ChunkedPipelinePostgresContainerTests.java`, `PostGisQueryRowSourceContainerTests.java`.

---

## 7. Conditional and live tests

### 7.1 Ollama (AI runtime)

Tests calling a real Ollama broker **skip** when `localhost:11434` is unreachable.

Pattern in `OllamaAiRuntimeBridgeLiveIT.java`:

```java
@Test
void generateTracedAgainstLocalOllamaBroker() {
    assumeOllamaAvailable();
    // ...
}

private static void assumeOllamaAvailable() {
    try (Socket socket = new Socket()) {
        socket.connect(new InetSocketAddress("localhost", 11434), 1000);
    } catch (Exception ex) {
        Assumptions.assumeTrue(false, "Ollama is not available on localhost:11434");
    }
}
```

Unit tests for retry/classification logic without live Ollama: `OllamaAiRuntimeBridgeTests` (uses recording/flaky test doubles).

See also: `docs/jdk25-upgrade.md` for CI skip behavior.

### 7.2 Docker availability

Use `@EnabledIf("...DockerTestSupport#dockerAvailable")` — tests are **skipped**, not failed, when Docker is absent.

---

## 8. Console Playwright E2E

### 8.1 Spec organization

Under `data-generator-console-web/e2e/specs/`:

| File pattern | Coverage |
|--------------|----------|
| `api.*.spec.ts` | REST smoke against running service |
| `api.console.spec.ts` | Console facade endpoints (`/api/templates`, `/api/jobs`, …) |
| `api.health.spec.ts` | Health endpoint |
| `navigation.spec.ts`, `home.spec.ts`, `pages.spec.ts` | SPA routing |
| `theme.spec.ts`, `acceptance.spec.ts` | Dual-theme UI acceptance |
| `template-workflow.spec.ts` | Save-template business flow |
| `rbac.*.spec.ts` | Role-based API and UI |
| `execution-reliability.spec.ts`, `ai-source.spec.ts` | Feature-pack scenarios |
| `distributed*.spec.ts`, `workflow-*.spec.ts` | Advanced operator flows |

### 8.2 API helper pattern

`data-generator-console-web/e2e/helpers/api.ts`:

```typescript
export function expectApiSuccess(body: unknown) {
  expect(body).toMatchObject({ success: true });
}

export async function fetchTemplates(request: APIRequestContext) {
  return apiGetWithRole(request, '/api/templates');
}
```

Specs assert `success: true` and unwrap `data` — matching Java `R<T>` envelope.

Example spec: `data-generator-console-web/e2e/specs/api.console.spec.ts`

```typescript
test('GET /api/templates', async ({ request }) => {
  const { res, body } = await fetchTemplates(request);
  expect(res.ok()).toBeTruthy();
  expectApiSuccess(body);
  expect(Array.isArray(unwrapApiData(body))).toBe(true);
});
```

### 8.3 Environment variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `DG_E2E_BASE_URL` | `http://127.0.0.1:9876/console` | Playwright page base URL |
| `DG_E2E_API_URL` | `http://127.0.0.1:9876` | Raw API requests |

---

## 9. Verify scripts (focused pipelines)

All scripts live under `scripts/` and use `scripts/lib/repo-maven.ps1` → `Invoke-RepoMaven` (JDK 25 wrapper + `.mvn/settings-jdk25.xml`).

### 9.1 Console full pipeline

**`scripts/verify-console.ps1`**

| Phase | Action |
|-------|--------|
| 1 | `scripts/verify-console-unit.ps1` (optional `-IncludeWebBuild`) |
| 2 | `npm run build` in `data-generator-console-web` |
| 3 | `scripts/e2e-podman.ps1` — Podman container + Playwright |

Flags: `-SkipBuild`, `-SkipUnit`, `-SkipE2e`, `-KeepContainer`, `-SkipDistributedSplit`

### 9.2 Console unit slice

**`scripts/verify-console-unit.ps1`**

Runs a **fixed Surefire test list** on `data-generator-service` (-am):

```
HealthControllerTest, ConsoleWebEndpointIT, ConsoleStaticResourceIT,
Console*ControllerTest, ConsoleAuthorizationIntegrationIT,
TemplateObjectMapperFactoryTests, ConsoleRoleTests, TaskScheduleServiceTests, ...
```

Maven invocation pattern:

```powershell
Invoke-RepoMaven -RepoRoot $RepoRoot -pl data-generator-service -am `
    "-Dtest=$testList" `
    '-Dsurefire.failIfNoSpecifiedTests=false' `
    test
```

### 9.3 Feature-pack verify scripts

| Script | Maven focus | Optional Playwright spec |
|--------|-------------|--------------------------|
| `scripts/verify-execution-reliability.ps1` | Sink retry, run reports, scenario IT | `execution-reliability.spec.ts` |
| `scripts/verify-ai-p1.ps1` | AI catalog, Ollama bridge, scenario IT | `ai-source.spec.ts` |
| `scripts/verify-ai-p2.ps1` | Cost tracing, usage metrics | (live Ollama optional) |
| `scripts/verify-ai-p3.ps1` … `verify-ai-p10.ps1` | Progressive AI platform features | Feature-specific specs |

Common flags: `-SkipPlaywright`, `-SkipLive`, `-SkipPlaywright` on AI scripts.

Example from `scripts/verify-ai-p1.ps1`:

```powershell
$testList = @(
    'AiCatalogServiceTest',
    'ConsoleAiCatalogControllerTest',
    'OllamaAiRuntimeBridgeTests',
    'V2ScenarioTemplateIT',
    'V2ScenarioCatalogServiceTest'
) -join ','
Invoke-RepoMaven -RepoRoot $RepoRoot `
    -pl "data-generator-service,data-generator-calcite" -am `
    "-Dtest=$testList" '-Dsurefire.failIfNoSpecifiedTests=false' test
```

### 9.4 Podman E2E runner

**`scripts/e2e-podman.ps1`**

1. `podman build` from `data-generator-service/Containerfile`
2. Run container on host port `9876` (default)
3. Wait for health (`Wait-Health` polls until `"opcode": 0`)
4. `npm run e2e` / Playwright against container
5. Tear down container (unless `-KeepContainer`)

Requires **Podman** locally; CI mirrors via `.github/workflows/console-verify.yml`.

---

## 10. Running tests manually

### 10.1 Full repo

```powershell
.\mvnw-jdk25.ps1 test
```

### 10.2 Module-scoped

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am test
.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test
```

### 10.3 Single test class

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -Dtest=ConsoleJobControllerTest test
```

### 10.4 Console frontend only

```powershell
cd data-generator-console-web
npm run build          # tsc + vite
npm run e2e            # requires running service
npm run verify:all     # delegates to scripts/verify-console.ps1
```

---

## 11. Coverage

**No JaCoCo plugin** is configured in root or module `pom.xml` files. Coverage is **not** enforced as a build gate.

Quality is driven by:

- Broad unit/integration test suites per module (~150+ `*Tests`/`*Test`/`*IT` classes)
- Focused verify scripts for feature packs
- Playwright E2E for operator console
- Embedded-first integration tests for pipeline parity

If adding coverage tooling, coordinate at root `pom.xml` level — do not add per-module JaCoCo silently.

---

## 12. Test authoring checklist

1. **Choose the lightest style that proves the behavior** — pure unit before full `@SpringBootTest`.
2. **Place tests in the owning module** under `src/test/java` mirroring main package.
3. **Follow Java file conventions** — copyright, class Javadoc, `@since` on test types.
4. **Service integration:** use `application-phase7-test.yaml`; clean up data in `@AfterEach`.
5. **Console controllers:** standalone MockMvc + `ConsoleApiAdvice` + mocked services.
6. **Pipeline tests:** prefer real H2/registry; use Testcontainers only when H2 cannot model behavior (cursor fetch, PostGIS).
7. **External deps:** use `Assumptions` or `@EnabledIf` to skip, not fail, when Ollama/Docker absent.
8. **Console UI/API changes:** run `scripts/verify-console.ps1` before claiming done.
9. **Feature-pack work:** run the matching `scripts/verify-ai-pN.ps1` or `scripts/verify-execution-reliability.ps1`.

---

## 13. Migration workbench test styles (reference)

From `docs/testing-embedded-components.md`:

| Test | Style |
|------|--------|
| `MigrationCompareServiceTests` | Pure unit — stub executor for classification math |
| `TemplateControllerMigrationCompareTests` | Integration — H2 + real `PipelineTemplateRunExecutor` |
| `MigrationInventoryBootstrapTests` | Integration — temp inventory file + H2 repository |
| Controller inventory/draft/promote | Integration — phase7 profile + temp paths |

Use this split as the template: **stub narrow logic**, **embed real executor** for parity proofs.

---

*Document generated by GSD codebase mapper — quality focus — 2026-06-17.*
