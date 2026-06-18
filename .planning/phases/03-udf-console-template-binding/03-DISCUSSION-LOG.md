# Phase 3: UDF Console & Template Binding - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-18
**Phase:** 3-UDF Console & Template Binding
**Areas discussed:** Artifact persistence, Upload & publish API, Template reference validation, Console UI & RBAC, Sample UDFs & E2E

---

## Artifact Persistence

| Option | Description | Selected |
|--------|-------------|----------|
| JDBC table | Metadata + payload in DB, reuse TemplatePO/TemplateRepository Spring Data pattern; H2 embedded test-friendly | ✓ |
| Filesystem | JAR/script on disk (like uploaded-drivers/), rebuild index on restart | |
| Hybrid | Metadata in JDBC, large JAR bytes on disk, script/sql text in DB | |

**User's choice:** JDBC table.

| Option | Description | Selected |
|--------|-------------|----------|
| Reload on startup | Reload published UDFs into registry + re-enter runtime merge view | ✓ |
| Metadata only | Persist metadata only, payload re-uploaded | |

**User's choice:** Reload published UDFs on startup.

| Option | Description | Selected |
|--------|-------------|----------|
| Global | No tenantId, aligns Phase 2 D-08 | ✓ |
| Add tenantId | Reserve for multi-tenant | |

**User's choice:** Global registry.

---

## Upload & Publish API

| Option | Description | Selected |
|--------|-------------|----------|
| Dedicated controller | New ConsoleUdfController @ /api/console/udfs, R<T>, ConsoleApiAdvice | ✓ |
| Reuse ConsoleUploadController | Generic upload + separate UDF metadata endpoint | |

**User's choice:** Dedicated ConsoleUdfController.

| Option | Description | Selected |
|--------|-------------|----------|
| Unified multipart | JAR file part + text fields for script/sql, single endpoint | ✓ |
| Split endpoints | JAR multipart, script/sql via JSON | |

**User's choice:** Unified multipart.

| Option | Description | Selected |
|--------|-------------|----------|
| Two-step | Upload draft → review → publish (governance at publish, D-21) | ✓ |
| One-step | Upload publishes immediately | |

**User's choice:** Two-step draft → publish.

| Option | Description | Selected |
|--------|-------------|----------|
| New version required | Reject duplicate udfId+version (D-05), keep old versions, published immutable | ✓ |
| Overwrite draft same version | Allow draft overwrite, published immutable | |

**User's choice:** New version required.

---

## Template Reference Validation (UDF-06)

| Option | Description | Selected |
|--------|-------------|----------|
| Extend TemplateV2Validator | Same entry as template validation/governance, reuse structured errors | ✓ |
| Separate UdfReferenceValidator | Called by lifecycle service | |

**User's choice:** Extend TemplateV2Validator.

| Option | Description | Selected |
|--------|-------------|----------|
| Per D-27 | SQL sqlName in transform text + script udfRef{id,version?}; java via PF4J not strict | ✓ |
| udfRef only | Only explicit udfRef blocks, SQL sqlName not parsed | |

**User's choice:** D-27 detection.

| Option | Description | Selected |
|--------|-------------|----------|
| Publish only | Draft save allows dangling refs, publish hard fail (D-19/D-21) | ✓ |
| Draft warn + publish fail | Validate on draft save (warning), publish hard fail | |

**User's choice:** Publish-only hard fail.

| Option | Description | Selected |
|--------|-------------|----------|
| Structured codes | UDF_NOT_FOUND / UDF_NOT_PUBLISHED / UDF_DEPRECATED with field | ✓ |
| Simple string | Plain message string | |

**User's choice:** Structured error codes.

---

## Console UI & RBAC

| Option | Description | Selected |
|--------|-------------|----------|
| Top-level page | New UdfsPage.tsx + route + api/udfs.ts, peer to Templates/Jobs | ✓ |
| Embedded | Inside existing page (e.g. template editor) | |

**User's choice:** New top-level UDFs page.

| Option | Description | Selected |
|--------|-------------|----------|
| Grouped | Grouped by udfId, expandable version history + status tags, inline publish/deprecate | ✓ |
| Flat | Flat version list + filters | |

**User's choice:** Grouped by udfId.

| Option | Description | Selected |
|--------|-------------|----------|
| OPERATOR+ | Upload/publish/deprecate need OPERATOR+, view VIEWER+ | ✓ |
| EDITOR | Reuse EDITOR (consistent with template editing) | |

**User's choice:** OPERATOR+ for mutations, VIEWER+ for read.

| Option | Description | Selected |
|--------|-------------|----------|
| Type-driven form | java=file drag, script=code editor+schema, sql=SQL text+sqlName | ✓ |
| Generic | Generic textarea + file, metadata JSON manual | |

**User's choice:** Type-driven upload form.

---

## Sample UDFs & E2E (UDF-08)

| Option | Description | Selected |
|--------|-------------|----------|
| samples/ | java reuses PF4J sample, script/sql each one; harness fixtures reference | ✓ |
| fixtures | All in data-generator-test-fixtures | |

**User's choice:** Samples in samples/.

| Option | Description | Selected |
|--------|-------------|----------|
| Reuse PF4J | Reuse/extend samples/template-v2-pf4j-plugin as java-plugin UDF (D-28 dual path) | ✓ |
| New | New standalone java UDF sample | |

**User's choice:** Reuse existing PF4J sample.

| Option | Description | Selected |
|--------|-------------|----------|
| Embedded | register→publish→template ref→TemplateV2 run assert, extend verify-harness.ps1 | ✓ |
| Playwright | Console UI end-to-end | |
| Both | Embedded required + one Playwright smoke | |

**User's choice:** Embedded integration E2E.

| Option | Description | Selected |
|--------|-------------|----------|
| Extend | Reuse existing 3 rows, extend linked_tests with console/template-binding E2E | ✓ |
| New rows | Add three udf-*-e2e rows | |

**User's choice:** Extend existing matrix rows.

---

## Claude's Discretion

- Concrete JDBC schema/columns for the UDF table; BLOB vs LOB streaming for large JARs.
- Whether persistence is a `JdbcUdfRegistry` implementation or a repository the registry delegates to (must keep Phase 2 `UdfRegistry` API stable).
- `ConsoleUdfController` route shapes, DTO names, version-history pagination.
- `sqlName` token extraction mechanics inside `TemplateV2Validator`.
- Sample UDF package names / function semantics and concrete `linked_tests` class names.
- React UDFs page component decomposition and AntD table/drawer layout.

## Deferred Ideas

- Playwright console UI E2E for full upload→publish→run flow (optional later hardening).
- Dedicated console UDF audit view (events already recorded in Phase 2).
- Delete/rollback of UDF versions beyond deprecate.
- Payload size limits / artifact scanning quotas.
- Tenant-scoped UDF registry rows (global for now).
- Velocity script UDF engine (GraalJS only).
- New built-in transform operators / SQL surface (Phase 4).
