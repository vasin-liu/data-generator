# Coding Conventions — data-generator

**Mapped:** 2026-06-17  
**Scope:** Java 25 / Maven monorepo, Spring Boot 4.x service, React operator console  
**Sources:** `.cursor/rules/`, production Java under `data-generator-*`, `AGENTS.md`, `CLAUDE.md`

---

## 1. Behavioral baseline (all contributors)

Follow the Karpathy guidelines in `.cursor/rules/karpathy-guidelines.mdc` and `CLAUDE.md`:

1. **Think before coding** — state assumptions; ask when ambiguous.
2. **Simplicity first** — minimum code that solves the problem; no speculative abstractions.
3. **Surgical changes** — touch only what the task requires; match existing style.
4. **Goal-driven execution** — define verifiable success criteria (tests, scripts) before claiming done.

When changing console UI, REST facades, or E2E assets, also follow `.cursor/rules/console-verify.mdc` and run `scripts/verify-console.ps1`.

---

## 2. Repository layout and module naming

| Pattern | Example | Purpose |
|---------|---------|---------|
| `data-generator-<concern>/` | `data-generator-calcite/` | Top-level Maven module for a domain concern |
| `data-generator-<concern>-<adapter>/` | `data-generator-writer-kafka/` | Pluggable adapter submodule |
| Package root | `org.gensokyo.data.<area>` | All Java sources |
| Service entry | `org.gensokyo.data.DataGeneratorApplication` | Spring Boot main in `data-generator-service` |
| Console REST | `org.gensokyo.data.api.console.*` | Operator console `/api/*` facades |
| Legacy/admin REST | `org.gensokyo.data.controller.*` | Non-console HTTP controllers |
| V2 pipeline runtime | `org.gensokyo.data.calcite.runtime.*` | Template V2 execution engine |
| Model VOs | `org.gensokyo.data.model.v2.*` | Template V2 YAML/JSON binding types |
| Shared response | `org.gensokyo.data.model.vo.R` | Standard API envelope |

**Rule:** Put new behavior in the **smallest module that owns it** (see `AGENTS.md`). Cross-cutting dependency versions go in root `pom.xml` / `data-generator-dependencies/`.

---

## 3. Java source file layout (mandatory)

Enforced by `.cursor/rules/java-copyright-class-javadoc.mdc` for **every** `.java` file.

### 3.1 Order

```
copyright block
package
imports
type-level Javadoc
class/interface/enum body
```

### 3.2 Copyright block (do not alter company text)

```java
/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
```

Reference: `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleJobController.java`

### 3.3 Type-level Javadoc

Required on the primary public type. Include description, `@author`, and `@since` (or `@version` where the project already uses it).

```java
/**
 * Task execution history for the React job center.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class ConsoleJobController {
```

### 3.4 Public API Javadoc

**Every** `public` method and constructor must have Javadoc with summary; `@param`, `@return`, `@throws` when applicable. No empty placeholders.

```java
/**
 * Lists execution history rows for the console job center.
 *
 * @param templateId  optional filter; when absent or blank, returns all executions
 * @param triggerType optional filter (e.g. MANUAL or SCHEDULED)
 * @return execution summaries newest first
 */
@GetMapping
public R<List<TaskExecutionSummary>> list(
        @RequestParam(name = "templateId", required = false) String templateId,
        @RequestParam(name = "triggerType", required = false) String triggerType) {
```

### 3.5 Inline comments

Use `//` (or `/* */` for longer notes) for **non-obvious** control flow, invariants, and workarounds — not for restating the code in English.

---

## 4. Naming conventions

### 4.1 Classes and types

| Kind | Pattern | Example |
|------|---------|---------|
| Spring `@RestController` (console) | `Console<Feature>Controller` | `ConsoleJobController` |
| Spring `@RestController` (legacy) | `<Feature>Controller` | `TaskController` |
| Service layer | `<Feature>Service` | `TaskExecutionService` |
| Configuration | `<Feature>Config` / `*Properties` | `ConsoleWebConfig`, `DataGeneratorProperties` |
| Factory / registry | `<Thing>Factory`, `<Thing>Registry` | `QuerySourceFactory`, `TemplateV2RuntimeRegistry` |
| Value objects (V2) | `*VO` suffix | `TemplateV2VO`, `ExecutionPolicyVO` |
| Persistence | `*PO`, `*Repository` | `TemplatePO`, `TemplateRepository` |
| DTO (console) | under `api.console.dto` | `JobExecutionDetail` |
| Utility | `*Kit`, `*Support` | `TemplateKit`, `EmbeddedKafkaTestSupport` |
| Domain exceptions | descriptive noun + base `ScaleLimitExceededException` in `data-generator-calcite` |

### 4.2 Methods and fields

- **Controllers:** HTTP verb names or domain verbs — `list`, `get`, `cancel`, `findById`.
- **Tests:** JUnit 5 `@Test` methods use `camelCase` describing behavior — `list_returnsExecutions`, `chunkedModeWritesAllRowsInBatches`, `singleTableSelectIsRowLocal`.
- **Constants:** `UPPER_SNAKE_CASE` in `static final` fields (see `ChunkedPipelineTests.ROW_COUNT`).

### 4.3 Test class naming (see `TESTING.md` for detail)

| Suffix | Meaning | Example |
|--------|---------|---------|
| `Tests` (plural) | Unit or integration test class | `ExecutionShapeClassifierTests` |
| `Test` (singular) | Also used (legacy/console slice) | `ConsoleJobControllerTest` |
| `IT` | Integration test (full or partial Spring context) | `ConsoleWebEndpointIT`, `V2ScenarioTemplateIT` |

Prefer **`Tests`** for new classes unless matching an existing file in the same package.

---

## 5. Language, build, and dependencies

- **Java 25** — `<java.version>25</java.version>`, `maven.compiler.release` in root `pom.xml`.
- **Enforcer** requires JDK `[25,)` and Maven `[3.6.3,)` (root `pom.xml` `maven-enforcer-plugin`).
- **Build on Windows:** use `.\mvnw-jdk25.ps1` or `.\mvnw.cmd -s .mvn\settings-jdk25.xml` (internal Nexus uses HTTP).
- **Lombok** (`1.18.44`): `@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Slf4j` are common in service and console code.
- **Spring Boot** `4.0.5` aggregated in `data-generator-service`.
- **Jackson 3.x** for JSON/YAML binding.

---

## 6. Spring and REST patterns

### 6.1 Console controllers

- Package: `org.gensokyo.data.api.console`
- Base path: `/api/<resource>` (e.g. `/api/jobs`)
- Constructor injection via `@RequiredArgsConstructor` + `final` fields
- Return type: **`R<T>`** envelope (never raw entities at the top level)

```java
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class ConsoleJobController {
    private final TaskExecutionService taskExecutionService;
    // ...
    @GetMapping
    public R<List<TaskExecutionSummary>> list(...) {
        return R.ok(taskExecutionService.list(parsedTemplateId, triggerType));
    }
}
```

File: `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleJobController.java`

### 6.2 Standard response envelope `R<T>`

Defined in `data-generator-service/src/main/java/org/gensokyo/data/model/vo/R.java`:

- Success: `R.ok(data)` → `success=true`, HTTP 200 semantics in `code`
- Failure: `R.fail(message)` → `success=false`
- Factory methods: `R.ok(...)`, `R.fail(...)`, nested `R.Page` for pagination

Console and legacy controllers both use `R<T>` for JSON consistency. Frontend helpers in `data-generator-console-web/e2e/helpers/api.ts` expect `{ success, data, message }`.

### 6.3 Validation

- Jakarta Validation on parameters: `@NotNull`, `@PathVariable`, `@RequestParam`
- Domain validation in services/runtime: throw **`IllegalArgumentException`** for client errors (unknown id, invalid template shape)
- **`IllegalStateException`** for internal invariant violations (pipeline misconfiguration)
- **`UnsupportedOperationException`** for unimplemented execution modes
- **`ScaleLimitExceededException`** for policy limit breaches in V2 runtime (`data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/`)

Example from `TemplateV2Runner.java`:

```java
if (template == null) {
    throw new IllegalArgumentException("Template V2 must not be null");
}
// ...
throw new UnsupportedOperationException("Execution mode not yet supported: " + policy.mode());
```

---

## 7. Error handling

### 7.1 Console API — centralized advice

`ConsoleApiAdvice` in `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleApiAdvice.java` applies only to `org.gensokyo.data.api.console`:

| Exception | HTTP status | Response |
|-----------|-------------|----------|
| `IllegalArgumentException` | 400 BAD_REQUEST | `R.fail(ex.getMessage())` |
| `Exception` (catch-all) | 500 INTERNAL_SERVER_ERROR | `R.fail(...)` + `log.error` |

```java
@RestControllerAdvice(basePackages = "org.gensokyo.data.api.console")
public class ConsoleApiAdvice {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> badRequest(IllegalArgumentException ex) {
        return R.fail(ex.getMessage());
    }
}
```

**When adding console endpoints:** rely on `IllegalArgumentException` for predictable 400 responses; register `ConsoleApiAdvice` in standalone MockMvc tests via `.setControllerAdvice(new ConsoleApiAdvice())` (see `ConsoleJobControllerTest`).

### 7.2 Logging

Use Lombok `@Slf4j` on advice and long-running services. Log unexpected errors at ERROR with stack trace; avoid logging sensitive data (secrets, credentials).

### 7.3 Fail-fast vs. envelope

- **Console `/api/*`:** exceptions → `ConsoleApiAdvice` → `R.fail`
- **Legacy controllers:** may return `R` directly from controller methods; some paths use `R.fail` in-controller
- **Runtime/pipeline:** throw typed exceptions; let upper layers translate to HTTP or run reports

---

## 8. V2 template and pipeline conventions

- Template model types live in `org.gensokyo.data.model.v2.*` with `*VO` suffix.
- Runtime orchestration: `TemplateV2Runner`, `ChunkedPipeline`, `StreamingPipeline` in `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/`.
- Execution modes: `CHUNKED`, streaming, partitioned compute — policy in `ExecutionPolicyVO`.
- Factories implement plugin-style registration (`QuerySourceFactory`, `JdbcSinkFactory`, etc.) wired through `TemplateV2RuntimeRegistry`.

When adding a source/sink/transform, implement the factory in the owning module and register via SPI or Spring auto-configuration in that module's `*AutoConfigurationTests` pattern.

---

## 9. Git commit conventions

From `.cursor/rules/git-commit-conventional-ai.mdc`:

```
type(optional scope): subject

optional body

AI-Assisted-by: Cursor
Co-authored-by: Name <email>
```

- **Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- **Subject:** ~72 chars, imperative, lowercase start, no trailing period
- **Footer:** always include `AI-Assisted-by` and `Co-authored-by` (use `git config user.name` / `user.email` for the latter)

---

## 10. Operator console (TypeScript / React)

Path: `data-generator-console-web/`

| Concern | Convention |
|---------|------------|
| Runtime | Node **22+**, `"type": "module"` in `package.json` |
| Build | `tsc -p tsconfig.json --noEmit && vite build` |
| UI library | Ant Design 5, React 19, React Router 7 |
| Data fetching | TanStack React Query |
| i18n | `i18next` + `react-i18next` |
| Page components | `export function <Name>Page()` in `src/app/pages/` |
| Shared UI | `src/components/` |
| Theme | `src/theme/ThemeProvider.tsx`, dual-theme support |
| Static embed | Built to `target/console-dist`, embedded in service JAR at `classpath:static/console/` |

**Naming:** PascalCase for React components (`JobsPage`, `TemplateStatusTag`); camelCase for hooks and helpers.

**No Vitest/Jest unit tests** in this repo slice — frontend verification is **TypeScript compile** (`verify:unit`) plus **Playwright E2E** under `e2e/specs/`.

E2E helpers centralize API access in `data-generator-console-web/e2e/helpers/api.ts`:

```typescript
export function apiBaseUrl(): string {
  return (process.env.DG_E2E_API_URL ?? 'http://127.0.0.1:9876').replace(/\/$/, '');
}

export function consoleRoleHeaders(role?: ConsoleRoleHeader): Record<string, string> {
  if (!role) return {};
  return { 'X-Console-Role': role };
}
```

RBAC tests pass `X-Console-Role` header (`VIEWER`, `EDITOR`, `OPERATOR`, `DATASOURCE_ADMIN`, `ADMIN`).

---

## 11. CodeGraph and exploration

When navigating symbols structurally, prefer CodeGraph MCP tools (see `.cursor/rules/codegraph.mdc`) over blind grep for "where is X defined". Index lives under `.codegraph/` when initialized.

---

## 12. Checklist for new Java public API

1. Copyright block above `package`
2. Class-level Javadoc with `@author`, `@since`
3. Javadoc on every new `public` method/constructor
4. Inline `//` only where logic is non-obvious
5. Place class in the owning Maven module
6. Console endpoints: return `R<T>`, throw `IllegalArgumentException` for client errors
7. Add tests in the same module (`*Tests` or `*IT`); run targeted `-pl … -am test`
8. Console changes: run `scripts/verify-console.ps1`

---

*Document generated by GSD codebase mapper — quality focus — 2026-06-17.*
