# JavaScript transform sandbox

Template V2 compute blocks may include a **JavaScript (`type: js`)** transform step. Scripts run in a **sandboxed GraalJS** context, once per input row, after upstream SQL or DAG nodes materialize the row set.

## Row binding

- The current row is exposed as a **`row` map** (string keys → values).
- Scripts should **mutate `row` in place**; the engine reads the map back as the output row.
- Example (from `scenario-js-transform.yaml`):

```yaml
transformers:
  - type: sql
    sql: SELECT value AS amount FROM seed
  - type: js
    script: row.amount = row.amount * 2
```

## Sandbox guarantees

Enforced by `JsTransformFactory` (GraalJS `Context`):

| Capability | Allowed |
|------------|---------|
| Map access on `row` | Yes |
| File / network IO | No (`IOAccess.NONE`) |
| Host Java classes | No (`allowHostClassLookup` → false) |
| Native access | No |
| Create threads | No |

ECMAScript version: **2022**.

## Limits

| Limit | Value | Config field |
|-------|-------|--------------|
| Max script size | 65 536 UTF-8 bytes | inline `script` |
| Default timeout | 5 000 ms per row | optional `timeoutMs` |

Validation: `TemplateV2Validator` rejects blank scripts, non-positive timeouts, and oversize bodies. Runtime applies the same size check before execution.

## Authoring surfaces

- **Console:** Compute block → Transform tab → linear chain or DAG node type **JavaScript (sandboxed)**.
- **YAML:** `type: js` with `script` and optional `timeoutMs` under `transformers[]` or `transformGraph.transforms`.

## Official scenario

| Id | Path | IT |
|----|------|-----|
| `GF-JS` | `template/v2-scenarios/scenario-js-transform.yaml` | `V2WorkflowScenarioIT` |

## Related docs

- `docs/template-v2-workflow-authoring-guide.md` — workflow + compute block overview
- `docs/superpowers/specs/2026-06-02-v2-capability-roadmap-design.md` — Wave 4 (JsTransform) acceptance
