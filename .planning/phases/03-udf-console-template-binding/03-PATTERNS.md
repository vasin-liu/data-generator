# Phase 3: UDF Console & Template Binding - Pattern Map

**Mapped:** 2026-06-18
**Files analyzed:** 14 (new + modified)
**Analogs found:** 13 / 14

> All analogs are concrete, in-repo files. Every new `.java` file must carry the PCI copyright block + class Javadoc + public-member Javadoc (CONVENTIONS §3, `.cursor/rules/java-copyright-class-javadoc.mdc`). Console controllers return `R<T>` and throw `IllegalArgumentException` for client errors (handled by `ConsoleApiAdvice`). The Phase 2 `UdfRegistry`/`UdfRecord` API must stay stable (D-04).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `api/console/ConsoleUdfController.java` (new) | controller | request-response + file-I/O (multipart) | `api/console/ConsoleUploadController.java` + `ConsoleJobController.java` | exact (split: multipart + R<T> verbs) |
| `api/console/dto/Udf*Dto.java` / `Udf*Request.java` (new) | model (DTO) | request-response | `api/console/dto/TaskScheduleUpsertRequest.java`, `TaskScheduleView.java` | exact |
| `model/po/UdfArtifactPO.java` (new) | model (persistence) | CRUD | `model/po/TemplatePO.java` | exact |
| `repository/UdfArtifactRepository.java` (new) | repository | CRUD | `repository/TemplateRepository.java` | exact |
| `udf/JdbcUdfRegistry.java` (or persistence delegate) (new) | service (registry impl) | CRUD | `udf/InMemoryUdfRegistry.java` | exact (same `UdfRegistry` contract) |
| `udf/UdfStartupReloader.java` (D-02 reload) (new) | service | event-driven (startup) | `udf/UdfPublishService.java` (`refreshRuntime()`) | role-match |
| `template/TemplateV2Validator.java` (modified, UDF-06) | utility (validator) | transform | self — existing `validateTransform`/policy methods in same file | exact (in-file) |
| `security/ConsoleAuthorizationFilter.java` (modified, D-15) | middleware | request-response | self — existing `requiredPermission` branches | exact (in-file) |
| `config/CoreConfig.java` (modified, bean wiring) | config | — | self — existing `udfRegistry()` `@ConditionalOnMissingBean` bean | exact (in-file) |
| `console-web/src/app/pages/UdfsPage.tsx` (new) | component | CRUD | `console-web/src/app/pages/SchedulesPage.tsx` | exact |
| `console-web/src/api/udfs.ts` (new) | api client | CRUD + multipart | `console-web/src/api/schedules.ts` + `api/client.ts` (`apiFormRequest`) | exact |
| `console-web/src/app/App.tsx` + `layout/ConsoleLayout.tsx` + `i18n/locales/{en,zh-CN}.json` (modified) | route/config | — | self — existing route + nav + flat i18n entries | exact (in-file) |
| `samples/udf-*` (new) | config (sample) | — | `samples/template-v2-pf4j-plugin/` (java-plugin, D-18) | role-match (java only) |
| `.planning/test-matrix.yaml` + embedded IT + `scripts/verify-harness.ps1` (modified, UDF-08) | test | batch | existing `udf-sql`/`udf-script`/`udf-java-plugin` rows (D-20) | no analog read (matrix only) |

---

## Pattern Assignments

### `api/console/ConsoleUdfController.java` (controller, request-response + multipart)

**Analogs:** `ConsoleUploadController.java` (multipart intake) + `ConsoleJobController.java` (R<T> resource verbs). New base path `/api/console/udfs` (D-05).

**Class header + DI pattern** — copy from `ConsoleJobController.java` lines 31-38:

```31:38:data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleJobController.java
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class ConsoleJobController {

    private final TaskExecutionService taskExecutionService;
    private final JobExecutionDetailService jobExecutionDetailService;
    private final WorkflowPauseCoordinator workflowPauseCoordinator;
```

→ `@RequestMapping("/api/console/udfs")`, inject `UdfRegistryService` (list/find/registerDraft) + `UdfPublishService` (publish/deprecate gate).

**Multipart upload + client-error pattern** — copy from `ConsoleUploadController.java` lines 44-54 (unified endpoint per D-06: JAR as file part, script/sql as text fields):

```44:54:data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleUploadController.java
    @PostMapping("/file")
    public R<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "upload.bin");
        String safeName = sanitizeFileName(original);
        Path dest = prepareDest(safeName);
        file.transferTo(dest);
        return R.ok(Const.R_OK, dest.toAbsolutePath().toString());
    }
```

→ Upload creates a `draft` only (D-07): read `file.getBytes()` for the JAR part (or `@RequestParam` text for script/sql), call `udfRegistryService.registerDraft(udfId, version, type, payload, metadata)`, return `R.ok(view)`.

**R<T> verb handlers (publish / deprecate / list / get)** — mirror `ConsoleJobController.java` lines 47-92:

```47:80:data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleJobController.java
    @GetMapping
    public R<List<TaskExecutionSummary>> list(
            @RequestParam(name = "templateId", required = false) String templateId,
            @RequestParam(name = "triggerType", required = false) String triggerType) {
        ...
        return R.ok(taskExecutionService.list(parsedTemplateId, triggerType));
    }
    ...
    @PostMapping("/{instanceId}/cancel")
    public R<String> cancel(@NotNull @PathVariable Long instanceId) {
        taskExecutionService.requestCancel(instanceId);
        ...
        return R.ok("Cancel requested");
    }
```

→ `GET /api/console/udfs` (list, grouped by `udfId` in the DTO assembly per D-14), `POST /{udfId}/{version}/publish` → `udfPublishService.publish(...)`, `POST /{udfId}/{version}/deprecate` → `udfPublishService.deprecate(...)`. The `/publish` path also triggers UDF-06 template validation upstream (see shared pattern).

**Error handling:** none in-controller. `UdfRegistryException` (Phase 2) and `IllegalArgumentException` bubble to `ConsoleApiAdvice` (see Shared Patterns). Note `UdfRegistryException` already carries a structured `code` + `List<UdfValidationError>` — preserve those in the failure body where field-level detail is needed (D-12).

---

### `api/console/dto/Udf*Dto.java` / `Udf*Request.java` (model/DTO, request-response)

**Analog:** `dto/TaskScheduleUpsertRequest.java` (record + Jakarta validation). DTOs live in `org.gensokyo.data.api.console.dto` (CONVENTIONS §4.1).

**Record DTO pattern** — copy from `TaskScheduleUpsertRequest.java` lines 21-26:

```21:26:data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/TaskScheduleUpsertRequest.java
public record TaskScheduleUpsertRequest(
        @NotNull Long templateId,
        @NotBlank String cronExpression,
        Boolean enabled,
        String description) {
}
```

→ `UdfVersionView` (udfId, version, type, state, registeredAt/publishedAt/deprecatedAt, metadata) mapped from `UdfRecord`; `UdfGroupView` (udfId + `List<UdfVersionView>`) for the grouped list (D-14). Map from `UdfRecord` accessors (`udfId()`, `version()`, `type()`, `state()`, timestamps) — see `UdfRecord.java` lines 47-105. Do **not** include `payload()` bytes in any list/view DTO. `UdfType` exposes `jsonName()` (used in `UdfPublishService.java` line 98) for stable wire values.

---

### `model/po/UdfArtifactPO.java` (model, persistence — CRUD)

**Analog:** `model/po/TemplatePO.java`. JPA `@Entity` + `@Table`, Lombok `@Getter/@Setter`, `CLOB` columns for large text (D-01).

**Entity pattern** — copy from `TemplatePO.java` lines 25-95:

```25:71:data-generator-service/src/main/java/org/gensokyo/data/model/po/TemplatePO.java
@Getter
@Setter
@Entity
@Table(name = "template")
public class TemplatePO implements Serializable {

    @Id
    private Long id;

    @Column(name = "name")
    private String name;
    ...
    @Column(columnDefinition = "CLOB", name = "content_json")
    private String contentJson;
```

→ Columns: `udf_id`, `version`, `type`, `state` (mirror `TemplatePO.status` `length=16`, line 94), `registered_at`/`published_at`/`deprecated_at` (`Instant`, mirror `archivedAt` line 89), `metadata_json` (`CLOB`), and the artifact bytes. For payload bytes (planner discretion, D-01): a `byte[]` column with `@Lob`/BLOB, or base64 in a `CLOB` (repo precedent for large text is `columnDefinition = "CLOB"` — see also `AuditEventPO.detail_json`, `TaskExecutionPO.report_json`). Composite identity is `udfId + version` (D-08) — use an `@IdClass`/`@EmbeddedId` (precedent: `AiQuotaScopeDailyUsageId.java`) or a surrogate `@Id` with a unique constraint.

> **Schema bootstrap note:** the JPA store (templates, schedules, audit) is Hibernate-managed; the `classpath:db/schema.sql` in `application.yaml` (line 36) is bound to a separate Druid datasource, **not** the JPA persistence unit. Follow the existing `@Entity`-driven DDL path — no hand-written schema.sql needed for the UDF table.

---

### `repository/UdfArtifactRepository.java` (repository, CRUD)

**Analog:** `repository/TemplateRepository.java`. `JpaRepository<PO, IdType>` with derived finder methods + `@Query` where derived names are insufficient.

**Repository pattern** — copy from `TemplateRepository.java` lines 21-48:

```21:48:data-generator-service/src/main/java/org/gensokyo/data/repository/TemplateRepository.java
public interface TemplateRepository extends JpaRepository<TemplatePO, Long> {

    List<TemplatePO> findByName(String name);
    ...
    @Query("select t from TemplatePO t where t.archived = false or t.archived is null")
    List<TemplatePO> findActiveForCatalog();
```

→ `findByUdfIdOrderByVersion(...)` (version history, D-08), `findByState("PUBLISHED")` (startup reload, D-02), `findByUdfIdAndVersion(...)` (duplicate guard / lookup).

---

### `udf/JdbcUdfRegistry.java` — persistence behind `UdfRegistry` (service, CRUD)

**Analog:** `udf/InMemoryUdfRegistry.java`. Implement the **same** `UdfRegistry` interface (`UdfRegistry.java` lines 18-75) so Phase 2 callers (`UdfRegistryService`, `UdfPublishService`, `RegistryBackedRuntimePluginProvider`) are untouched (D-04). Whether this is a standalone impl or a delegate behind the in-memory one is planner discretion — keep the contract stable.

**Validation + lifecycle transition + structured errors** — copy semantics from `InMemoryUdfRegistry.java`:

```33:57:data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/udf/InMemoryUdfRegistry.java
    @Override
    public UdfRecord registerDraft(String udfId, String version, UdfType type, byte[] payload,
                                   Map<String, String> metadata) {
        String normalizedId = validateUdfId(udfId);
        String normalizedVersion = validateVersion(version);
        ...
        if (records.containsKey(key)) {
            throw new UdfRegistryException("UDF_DUPLICATE_VERSION",
                    "UDF already registered: " + normalizedId + "@" + normalizedVersion);
        }
        UdfRecord record = new UdfRecord.Builder()...build();
        records.put(key, record);
        return record;
    }
```

```59:93:data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/udf/InMemoryUdfRegistry.java
    @Override
    public UdfRecord publish(String udfId, String version) {
        UdfRecord current = requireExisting(udfId, version);
        if (current.state() == UdfLifecycleState.PUBLISHED) {
            return current;
        }
        if (current.state() != UdfLifecycleState.DRAFT) {
            throw new UdfRegistryException("UDF_INVALID_TRANSITION", ...);
        }
        UdfRecord published = current.toBuilder()
                .state(UdfLifecycleState.PUBLISHED)
                .publishedAt(Instant.now())
                .build();
        ...
    }
```

→ Replace the `ConcurrentHashMap` store with repository calls; reuse the **exact** validation regexes, semver compare, `UdfRegistryException` codes (`UDF_DUPLICATE_VERSION`, `UDF_NOT_FOUND`, `UDF_NOT_PUBLISHED`, `UDF_DEPRECATED`, `UDF_INVALID_*`), and the immutable-`published` rule (D-08). Map `UdfArtifactPO ↔ UdfRecord` via `UdfRecord.Builder` (`UdfRecord.java` lines 126-228).

**Bean wiring** — the registry bean is override-ready in `CoreConfig.java` (already `@ConditionalOnMissingBean`):

```319:323:data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java
    @Bean
    @ConditionalOnMissingBean(UdfRegistry.class)
    public UdfRegistry udfRegistry() {
        return new InMemoryUdfRegistry();
    }
```

→ Define the JDBC-backed `UdfRegistry` `@Bean` in `data-generator-service` (it owns the repository); `@ConditionalOnMissingBean` then yields to it automatically. The `RegistrySqlFunctionSource` bean (lines 332-336) consumes whichever `UdfRegistry` is present — no change needed.

---

### `udf/UdfStartupReloader.java` — published-record rehydrate (service, event-driven)

**Analog:** `udf/UdfPublishService.java` `refreshRuntime()` (lines 103-107) — the runtime-refresh hook to re-trigger after rehydration (D-02).

```103:107:data-generator-service/src/main/java/org/gensokyo/data/udf/UdfPublishService.java
    private void refreshRuntime() {
        if (runtimeRegistryProvider != null) {
            runtimeRegistryProvider.refresh();
        }
    }
```

→ On `ApplicationReadyEvent` (or `@PostConstruct`), load `published` rows via the repository into the registry, then call `TemplateV2RuntimeRegistryProvider.refresh()` so persisted UDFs re-enter the runtime merge view exactly as a fresh publish does. If a JDBC-backed `UdfRegistry` reads straight from the DB, the reload is just the `refresh()` call. Inject `TemplateV2RuntimeRegistryProvider` as `@Nullable` (mirror `UdfPublishService` constructor lines 42-50) for minimal/test contexts.

---

### `template/TemplateV2Validator.java` (MODIFIED — UDF-06 reference validation)

**Analog:** self. Extend in-file following the existing structured, path-scoped validation style. This is publish-only hard fail (D-11) — wire the new check into the publish gate, not draft saves.

**Per-transform dispatch to copy** — `validateTransform` lines 249-262 already branches per transform subtype; add UDF reference scanning here:

```249:262:data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java
    private static void validateTransform(TransformVO transformer, String path) {
        if (transformer == null) {
            throw new IllegalArgumentException(pathMessage(path, "transform must not be null"));
        }
        if (transformer instanceof SqlTransformVO sqlTransform && StrKit.isBlank(sqlTransform.getSql())) {
            throw new IllegalArgumentException(pathMessage(path + ".sql", "SQL transformer SQL must not be blank"));
        }
        if (transformer instanceof SpelTransformVO spelTransform) {
            validateSpelTransform(spelTransform, path);
        }
        if (transformer instanceof JsTransformVO jsTransform) {
            validateJsTransform(jsTransform, path);
        }
    }
```

→ For `SqlTransformVO`, scan `getSql()` for `sqlName` tokens; for `JsTransformVO`, scan the script for `udfRef:{id, version?}` blocks (Phase 2 D-27/D-10). Java/PF4J capabilities are not reference-validated (D-10). Token-extraction mechanics without false positives are planner discretion (D-12 note).

**Regex-scan + structured-message precedent** — reuse the in-file pattern compilation + path-message style (lines 264-266, 354-356):

```354:356:data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java
    private static final Pattern SQL_JOIN = Pattern.compile("\\bJOIN\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_SELECT_DISTINCT =
            Pattern.compile("\\bSELECT\\s+DISTINCT\\b", Pattern.CASE_INSENSITIVE);
```

→ Resolve each reference via `UdfRegistryService.resolve(udfId, version)` / `find(...)` and raise structured codes `UDF_NOT_FOUND` / `UDF_NOT_PUBLISHED` / `UDF_DEPRECATED` with the offending `field`/`path` (D-12). The registry already throws these exact codes (`InMemoryUdfRegistry.requirePublished` lines 134-144) — reuse, don't re-invent. Because `TemplateV2Validator` is currently a static-utility class with no Spring deps, passing the registry in (parameter or a small companion validator bean) is planner discretion; keep the existing static `validate(...)` signature working for callers that don't need UDF checks.

---

### `console-web/src/app/pages/UdfsPage.tsx` (component, CRUD)

**Analog:** `pages/SchedulesPage.tsx` — React Query list + AntD `Table` + `Modal` form + mutations + status `Tag`s, the closest CRUD-with-lifecycle page.

**Page scaffold (query + mutations + invalidate)** — copy from `SchedulesPage.tsx` lines 44-123:

```44:114:data-generator-console-web/src/app/pages/SchedulesPage.tsx
export function SchedulesPage() {
  const { t } = useTranslation();
  ...
  const schedulesQuery = useQuery({
    queryKey: ['schedules', filterTemplateId],
    queryFn: () => fetchSchedules(filterTemplateId),
    refetchInterval: 30_000,
  });
  ...
  const saveMutation = useMutation({
    mutationFn: async (values: ScheduleFormValues) => { ... },
    onSuccess: () => {
      message.success(t('schedules.dialog.saved'));
      setModalOpen(false);
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });
```

→ `useQuery(['udfs', typeFilter], fetchUdfs)`; `publishMutation`/`deprecateMutation`/`uploadMutation` each `invalidate(['udfs'])` on success. List grouped by `udfId` with expandable version rows (`Table` `expandable`) and lifecycle `Tag`s — reuse the `Tag`-by-status idiom (`SchedulesPage.tsx` lines 234-242). Upload `Modal` is type-driven (java = file drag/drop, script = code editor, sql = SQL text + `sqlName`, D-16). Add `data-testid="udfs-page"` (smoke coverage convention, line 298).

**Header + actions** — copy `ConsolePageHeader` usage lines 299-308; inline publish/deprecate `Button`s in the actions column (lines 270-292 pattern).

---

### `console-web/src/api/udfs.ts` (api client, CRUD + multipart)

**Analog:** `api/schedules.ts` (JSON verbs) + `api/client.ts` (`apiRequest` for JSON, `apiFormRequest` for multipart).

**JSON client pattern** — copy from `schedules.ts` lines 7-44:

```7:27:data-generator-console-web/src/api/schedules.ts
export function fetchSchedules(templateId?: string): Promise<TaskScheduleView[]> {
  const suffix = templateId != null ? `?templateId=${encodeURIComponent(templateId)}` : '';
  return apiRequest<TaskScheduleView[]>(`/console/schedules${suffix}`);
}
...
export function createSchedule(body: TaskScheduleUpsertRequest): Promise<TaskScheduleView> {
  return apiRequest<TaskScheduleView>('/console/schedules', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}
```

→ `fetchUdfs(type?)` → `GET /console/udfs`, `publishUdf(udfId, version)` → `POST /console/udfs/{udfId}/{version}/publish`, `deprecateUdf(...)`. **Multipart upload** uses `apiFormRequest` (FormData; `Content-Type` omitted so the browser sets the boundary) — `client.ts` lines 48-55:

```48:55:data-generator-console-web/src/api/client.ts
export async function apiFormRequest<T>(path: string, form: FormData, method = 'POST'): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    body: form,
    headers: consoleRoleHeaders(),
  });
  return parseApiResult<T>(res);
}
```

Add view/request types to `src/api/types.ts` (peer to `TaskScheduleView`).

---

### `App.tsx` + `ConsoleLayout.tsx` + i18n (MODIFIED — route, nav, strings)

**Route registration** — add to `App.tsx` route tree (lines 17-29), peer to `schedules`:

```24:28:data-generator-console-web/src/app/App.tsx
        <Route path="jobs" element={<JobsPage />} />
        <Route path="jobs/:instanceId" element={<JobDetailPage />} />
        <Route path="schedules" element={<SchedulesPage />} />
        <Route path="audit" element={<AuditPage />} />
```

→ `<Route path="udfs" element={<UdfsPage />} />` + import.

**Nav entry** — add to `ConsoleLayout.tsx` menu items (line 65 sibling):

```61:66:data-generator-console-web/src/app/layout/ConsoleLayout.tsx
    { key: '/', testId: 'nav-home', label: t('nav.home'), icon: <HomeOutlined /> },
    { key: '/templates', testId: 'nav-templates', label: t('nav.templates'), icon: <FileTextOutlined /> },
    { key: '/datasources', testId: 'nav-datasources', label: t('nav.datasources'), icon: <DatabaseOutlined /> },
    { key: '/jobs', testId: 'nav-jobs', label: t('nav.jobs'), icon: <HistoryOutlined /> },
    { key: '/schedules', testId: 'nav-schedules', label: t('nav.schedules'), icon: <ClockCircleOutlined /> },
    { key: '/audit', testId: 'nav-audit', label: t('nav.audit'), icon: <AuditOutlined /> },
```

→ `{ key: '/udfs', testId: 'nav-udfs', label: t('nav.udfs'), icon: <...Outlined /> }`.

**i18n** — keys are **flat dotted strings** (not nested), added to both `en.json` and `zh-CN.json` (D-16):

```2:7:data-generator-console-web/src/i18n/locales/en.json
  "nav.home": "Home",
  "nav.templates": "Templates",
  "nav.datasources": "Datasources",
  "nav.jobs": "Jobs",
  "nav.schedules": "Schedules",
  "nav.audit": "Audit",
```

→ Add `"nav.udfs"`, `"udfs.title"`, `"udfs.upload.*"`, `"udfs.status.{draft,published,deprecated}"`, etc., to both locale files (parity required).

---

### `samples/udf-*` (config/sample)

**Analog:** `samples/template-v2-pf4j-plugin/` — the java-plugin UDF sample reuses/extends this PF4J module to preserve the D-28 dual-path guarantee (D-18). Script and SQL samples are added alongside as new sample assets (function semantics + package names are planner discretion). No code excerpt — packaging is documented in `samples/template-v2-pf4j-plugin/README.md`.

---

## Shared Patterns

### Console error mapping (all controller + validator paths)
**Source:** `api/console/ConsoleApiAdvice.java` lines 29-44
**Apply to:** `ConsoleUdfController`, the UDF-06 validation path

```29:44:data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleApiAdvice.java
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> badRequest(IllegalArgumentException ex) {
        return R.fail(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> serverError(Exception ex) {
        log.error("Console API error", ex);
        return R.fail(ex.getMessage() != null ? ex.getMessage() : "Internal error");
    }
```

Throw `IllegalArgumentException` for client errors (400). `UdfRegistryException` is **not** `IllegalArgumentException` — to get a 400 + structured `code`/violations in the body (D-12), either add an `@ExceptionHandler(UdfRegistryException.class)` to `ConsoleApiAdvice` (returning `R.fail(message, violationsPayload)`) or translate it to `IllegalArgumentException` at the controller boundary. Adding the handler is the lower-risk, more faithful option. In MockMvc slice tests register advice via `.setControllerAdvice(new ConsoleApiAdvice())` (CONVENTIONS §7.1).

### Response envelope `R<T>`
**Source:** `model/vo/R.java` lines 40-66
**Apply to:** every `ConsoleUdfController` method

`R.ok(data)`, `R.ok(message, data)`, `R.fail(message)`, `R.fail(message, data)`. Never return raw entities/`UdfRecord` at the top level.

### RBAC gating (D-15)
**Source:** `security/ConsoleAuthorizationFilter.java` lines 72-115 + `security/ConsoleRole.java` lines 18-35
**Apply to:** add a `/api/console/udfs` branch in `requiredPermission`

```88:114:data-generator-service/src/main/java/org/gensokyo/data/security/ConsoleAuthorizationFilter.java
        if (path.contains("/publish")) {
            return ConsolePermission.TEMPLATE_PUBLISH;
        }
        ...
        if (path.startsWith("/api/console/schedules")) {
            if (HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method) || HttpMethod.DELETE.matches(method)) {
                return ConsolePermission.TEMPLATE_RUN;
            }
            return ConsolePermission.JOB_READ;
        }
```

→ Add: `/api/console/udfs` mutations (upload/publish/deprecate = POST) require an OPERATOR-grade permission; GET requires a VIEWER-grade read permission. The existing generic `path.contains("/publish")` branch (line 88) currently maps to `TEMPLATE_PUBLISH` and would catch `/udfs/.../publish` — **add the explicit `/api/console/udfs` branch before it** to assign the intended UDF permission. Security is default-off (`ConsoleSecurityProperties.isEnabled()` line 37), but honor the contract where enabled. Frontend already attaches `X-Console-Role` on every request (`api/client.ts` lines 7-11) — no per-call change needed.

### Audit + runtime refresh on lifecycle change (already implemented)
**Source:** `udf/UdfPublishService.java` lines 60-107
**Apply to:** all publish/deprecate flows — call `UdfPublishService`, do **not** call `registry.publish/deprecate` directly from the controller, so governance (D-21), audit (D-24), and runtime refresh (D-08) all run.

---

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `.planning/test-matrix.yaml` + harness E2E wiring (UDF-08) | test | batch | Matrix is YAML config (not read in this pass); reuse existing `udf-sql`/`udf-script`/`udf-java-plugin` rows and extend `linked_tests` (D-20). Embedded register→publish→reference→run IT follows `data-generator-test-fixtures` (`FixtureTemplates.load` / `H2Seed.apply`) per CONTEXT canonical refs; wire through `scripts/verify-harness.ps1` (D-19). No new matrix rows. |

> Note: script/SQL **sample UDF** assets have no direct in-repo analog (only the PF4J java-plugin sample exists); their function semantics are planner discretion (D-18) — listed as role-match, not "no analog".

## Metadata

**Analog search scope:** `data-generator-service/.../api/console`, `.../model/po`, `.../repository`, `.../udf`, `.../security`, `.../template`, `.../config`; `data-generator-common/.../udf`; `data-generator-console-web/src/{app,api,i18n}`
**Files scanned:** 20 read in full/targeted + globs across 5 directories
**Pattern extraction date:** 2026-06-18
