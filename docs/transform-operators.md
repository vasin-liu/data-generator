# Built-in Transform Operators

Template V2 ships built-in, no-code transform operators that complement the `sql`, `spel`, and `js`
transforms and operator-uploaded UDFs. Each operator is a `transform` node identified by its `type`
discriminator. Operators are discoverable at runtime through the catalog endpoint
`GET /api/console/transforms` (built-ins plus published UDFs).

All operators are **row-local**: they read the in-pipeline table `input` and emit a transformed table.
Failures are **fail-fast** (the first error terminates the run) and are surfaced as structured
`transformErrors` in the run report and console job detail.

## `json`

Parses a JSON string column into an object and, when `flatten` is enabled, expands nested fields into
separate columns using a separator-named convention (e.g. `addr.city`). Exercises decision D-02.

| Field | Type | Required | Meaning |
|---|---|---|---|
| `sourceColumn` | string | yes | Input column whose string value is parsed as JSON on each row. |
| `targetColumn` | string | no | Column holding the parsed object when `flatten` is `false`; defaults to `sourceColumn`. |
| `flatten` | boolean | no | When `true`, nested fields become separate columns; defaults to `false`. |
| `separator` | string | no | Separator used to compose nested-key column names when flattening; defaults to `.`. |

```yaml
transform:
  type: json
  sourceColumn: payload
  flatten: true
  separator: "."
```

## `mask`

Masks column values in place using predefined named strategies. The output schema equals the input
schema (no columns added). Exercises decision D-03. Error messages carry the column and strategy names
only — never the original unmasked value (PII-safe).

| Field | Type | Required | Meaning |
|---|---|---|---|
| `rules` | list | yes | Masking rules applied per input row. |
| `rules[].column` | string | yes | Column to redact. |
| `rules[].strategy` | string | yes | One of `email`, `phone`, `credit-card`, `generic-fixed`. |

Strategies:

- `email` — keep the first character of the local part and the full domain (`a****@example.com`).
- `phone` — keep the last 4 alphanumeric characters, mask the rest.
- `credit-card` — keep the last 4 alphanumeric characters, mask the rest; separators preserved.
- `generic-fixed` — mask every alphanumeric character, preserving non-alphanumeric separators.

```yaml
transform:
  type: mask
  rules:
    - column: email
      strategy: email
    - column: phone
      strategy: phone
    - column: card
      strategy: credit-card
```

## `lookup`

Enriches input rows by joining, on a key, against another source already declared in the same template
(a named in-template source). It does not read from a configured JDBC datasource and does not define
inline maps. A missing lookup source, a duplicate `rightKey`, or a lookup miss is a fail-fast failure.
Exercises decision D-04.

| Field | Type | Required | Meaning |
|---|---|---|---|
| `source` | string | yes | Name of the in-template source to join against. |
| `leftKey` | string | yes | Input-row column used as the join key. |
| `rightKey` | string | yes | Lookup-source column matched against `leftKey`. |
| `columns` | list of string | yes | Lookup-source columns projected onto the enriched output row. |

```yaml
sources:
  input: { type: inline_rows, rows: [ { id: "1", dept_id: d1 } ] }
  ref:   { type: inline_rows, rows: [ { dept_id: d1, dept_name: Eng } ] }
transform:
  type: lookup
  source: ref
  leftKey: dept_id
  rightKey: dept_id
  columns:
    - dept_name
```

## Schema & version notes

The `json`, `mask`, and `lookup` operator `type`s and all of their fields are **additive** to the
Template V2 schema (decision D-13). Existing templates parse and run unchanged, and **no breaking
template-version bump is introduced**. Persisted run reports gained an additive `transformErrors` list
that null-normalizes for backward compatibility, so historical reports still deserialize.

## Internal SQL functions

`V2_JSON_EXTRACT(json, path)` is an **internal-only** Calcite scalar function usable inside `sql`
transforms to read a value at a dot path from a JSON string. It is intentionally **not** listed in the
`/api/console/transforms` catalog and is reserved under the `V2_` prefix, away from the UDF `sqlName`
namespace (decision D-12). It is an implementation aid, not a published, supported surface.
