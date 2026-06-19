# Phase 4: Transform Operators & SQL - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-19
**Phase:** 4-Transform Operators & SQL
**Areas discussed:** Operator set, Catalog discovery surface, Error surfacing contract, SQL enhancement exposure

---

## Operator Set (XFORM-02)

| Option | Description | Selected |
|--------|-------------|----------|
| JSON parse/flatten | Parse string column to object, optional flatten to columns | ✓ |
| Masking/redaction | Built-in masking (email/phone/credit-card/generic) | ✓ |
| Lookup/join helper | Join another dataset/map by key | ✓ |
| Type coerce/cast | Explicit type conversion (string↔number↔date) | |
| Regex extract/replace | Regex-based extraction/replacement | |

**User's choice:** json, mask, lookup

### Follow-up — Lookup source
| Option | Description | Selected |
|--------|-------------|----------|
| Inline map only | key→value table in YAML | |
| Reference existing named source | Join by key against a source already in the template | ✓ |
| Reference JDBC datasource | Configured datasource/table | |
| Inline map + named source | Both | |

**User's choice:** Reference existing named source

### Follow-up — Masking rule
| Option | Description | Selected |
|--------|-------------|----------|
| Named strategies | email/phone/credit-card/generic-fixed by name | ✓ |
| Generic rule | keep first/last N, mask middle | |
| Both | named + custom | |

**User's choice:** Named strategies

### Follow-up — JSON output shape
| Option | Description | Selected |
|--------|-------------|----------|
| Parse + optional flatten | object + separator-named columns (addr.city) | ✓ |
| Parse only | nested map column, no auto-flatten | |
| Explicit path→column map | user lists JSON path → target columns | |

**User's choice:** Parse + optional flatten

---

## Catalog Discovery Surface (XFORM-01)

| Option | Description | Selected |
|--------|-------------|----------|
| API-only | metadata endpoint + docs, no console page | ✓ |
| API + console page | new console catalog page | |
| API + inline in editor | lightweight surfacing in template editor | |

**User's choice:** API-only

### Follow-up — Catalog scope
| Option | Description | Selected |
|--------|-------------|----------|
| Unified | built-in operators + published UDFs with source flag | ✓ |
| Built-in only | UDFs queried separately via /api/console/udfs | |

**User's choice:** Unified

### Follow-up — Entry metadata richness
| Option | Description | Selected |
|--------|-------------|----------|
| Rich | type + description + param schema + example | ✓ |
| Medium | type + description + param list | |
| Minimal | type + description | |

**User's choice:** Rich

---

## Error Surfacing Contract (XFORM-05)

| Option | Description | Selected |
|--------|-------------|----------|
| Rich | operator + step path + root cause + row/field locators | ✓ |
| Medium | operator/step + cause (no row/field) | |
| Minimal | message only | |

**User's choice:** Rich

### Follow-up — Location
| Option | Description | Selected |
|--------|-------------|----------|
| Both | run report + console job detail | ✓ |
| Report only | run report (console reuses report fields) | |

**User's choice:** Both

### Follow-up — Failure policy
| Option | Description | Selected |
|--------|-------------|----------|
| Fail-fast | terminate run, report failing step + cause | ✓ |
| Per-row tolerant | skip/record bad rows, threshold to fail | |
| Configurable | default fail-fast, optional tolerance | |

**User's choice:** Fail-fast

---

## SQL Enhancement Exposure (XFORM-03)

| Option | Description | Selected |
|--------|-------------|----------|
| Minimal | operators standalone; add Calcite scalar functions only where internally needed | ✓ |
| First-class | expose SQL scalar functions (JSON_EXTRACT/MASK) for sql transforms, in catalog | |
| SQL-first | implement mainly as SQL functions, thin operator wrappers | |

**User's choice:** Minimal

### Follow-up — Internal SQL function visibility
| Option | Description | Selected |
|--------|-------------|----------|
| Internal-only | not in catalog (docs only) | ✓ |
| List them | catalog as sql-function kind | |

**User's choice:** Internal-only

---

## Claude's Discretion
- Operator `type` string names, `*TransformVO` field shapes, factory class placement.
- Catalog endpoint route, DTO names, metadata sourcing approach.
- Naming/prefix for internal Calcite scalar functions (avoid UDF sqlName collisions).
- `lookup` materialization/indexing and missing/duplicate-key behavior.
- Concrete `mask` strategy patterns and per-operator harness sample templates.

## Deferred Ideas
- `type coerce/cast` and `regex extract/replace` operators — future built-ins.
- Per-row error tolerance / skip-and-continue with thresholds.
- Console operator-catalog UI page.
- First-class SQL scalar functions in `sql` transforms + catalog listing.
- `lookup` against configured JDBC datasources/tables.
