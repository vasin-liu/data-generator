# Phase 2: UDF Platform Core - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-17
**Phase:** 2-UDF Platform Core
**Areas discussed:** Registry storage & lifecycle, UDF type boundaries, ID & versioning, Governance (UDF-07), Harness/matrix integration, Hot reload, Template ref syntax, Error contract, Service placement, PF4J compat, Deprecated behavior, Doc language

---

## Registry Storage & Lifecycle

| Option | Description | Selected |
|--------|-------------|----------|
| In-memory registry | Phase 2 API + Map; persistence Phase 3 | ✓ |
| JDBC Phase 2 | dg_udf_registry table now | |
| Hybrid | JDBC metadata + file artifacts | |

| Option | Description | Selected |
|--------|-------------|----------|
| Full FSM | draft → published → deprecated | ✓ |
| Minimal | registered/active only | |
| Register = published | No draft in Phase 2 | |

| Option | Description | Selected |
|--------|-------------|----------|
| data-generator-common | Domain + interfaces | ✓ |
| calcite only | All in calcite module | |
| New udf module | Top-level data-generator-udf | |

| Option | Description | Selected |
|--------|-------------|----------|
| Refresh on change | publish/deprecate → runtime refresh | ✓ |
| Resolve at run | Snapshot without auto refresh | |
| Defer refresh | Phase 3 only | |

| Option | Description | Selected |
|--------|-------------|----------|
| Inline payload | byte[]/String in memory | ✓ |
| Filesystem | conf/udfs/ directory | |

| Option | Description | Selected |
|--------|-------------|----------|
| Multi-version coexist | semver versions per udfId | ✓ |
| Single latest | Replace on new version | |

| Option | Description | Selected |
|--------|-------------|----------|
| Spring UdfRegistryService | Shared by tests + Phase 3 REST | ✓ |
| Pure Java API | No Spring in common | |

| Option | Description | Selected |
|--------|-------------|----------|
| Merge built-in | built-in wins on SQL name clash | ✓ |
| Registry only | built-ins also registered | |

---

## UDF Type Boundaries

| Option | Description | Selected |
|--------|-------------|----------|
| PF4J JAR | Existing plugin path; registry metadata | ✓ |
| Annotated class | Separate @Udf contract | |

| Option | Description | Selected |
|--------|-------------|----------|
| Callable function | Schema'd script UDF; not scripter stage | ✓ |
| JsTransform alias | No separate UDF model | |

| Option | Description | Selected |
|--------|-------------|----------|
| TemplateV2SqlFunction wrapper | Reuse SqlFunctionRegistry | ✓ |
| SQL macro | Inline SQL expansion | |

| Option | Description | Selected |
|--------|-------------|----------|
| JSON Schema at publish | UDF-03 validation | ✓ |
| Syntax only | Defer typing | |

| Option | Description | Selected |
|--------|-------------|----------|
| Full runtime plugin | sqlFunctions + transformFactories | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| GraalJS only Phase 2 | Velocity deferred | ✓ |
| GraalJS + Velocity | Both engines | |

---

## ID & Versioning

| Option | Description | Selected |
|--------|-------------|----------|
| Reverse DNS udfId | com.example.my_udf | ✓ |
| Flat slug | Short names | |

| Option | Description | Selected |
|--------|-------------|----------|
| Semver | strict major.minor.patch | ✓ |
| Integer / CalVer | Alternative schemes | |

| Option | Description | Selected |
|--------|-------------|----------|
| Latest published default | Omit version → newest published | ✓ |
| Explicit only | Must pin version | |

| Option | Description | Selected |
|--------|-------------|----------|
| udfId ≠ sqlName | Separate sqlName metadata | ✓ |
| udfId = SQL name | Single identifier | |

| Option | Description | Selected |
|--------|-------------|----------|
| Reject duplicate udfId+version | No overwrite | ✓ |
| Global registry | No tenant column Phase 2 | ✓ |

---

## Governance (UDF-07)

| Option | Description | Selected |
|--------|-------------|----------|
| Reuse governance pattern | Extend TemplateGovernanceSupport style | ✓ |
| Publish gate | All checks at publish | ✓ |
| Script blacklist | Dangerous pattern scan | ✓ |
| Audit on publish/deprecate | Same store as template audit | ✓ |
| PF4J isolation + JAR check | Java plugin governance | ✓ |

---

## Additional Gray Areas

| Topic | Selected |
|-------|----------|
| Matrix: 3 rows (java/script/sql) | ✓ |
| Job UDF snapshot at run start | ✓ |
| Typed ref syntax (sqlName / udfRef / PF4J) | ✓ |
| Structured error codes | ✓ |
| common interface + service impl | ✓ |
| Dual PF4J path (dirs + registry) | ✓ |
| Hard fail on draft/deprecated resolve | ✓ |
| CONTEXT.md English | ✓ |

---

*Discussion log for Phase 2 — UDF Platform Core*
