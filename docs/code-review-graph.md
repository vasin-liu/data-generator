# Code review graph (data-generator-calcite)

This document describes how to build and use the **structural code review graph** for the Template V2 Calcite module.

## What was built

A graphify pipeline (AST-only, no semantic LLM extraction) over:

`data-generator-calcite/`

Generated artifacts are written under **`graphify-out/`** at the repository root:

| Artifact | Purpose |
|----------|---------|
| `graphify-out/graph.html` | Interactive HTML — open in a browser |
| `graphify-out/graph.json` | Machine-readable graph (communities, nodes, edges) |
| `graphify-out/graph.graphml` | Import into Gephi or yEd for layout and annotation |
| `graphify-out/GRAPH_REPORT.md` | Narrative report: hubs, cross-community bridges, cohesion |

The full monorepo is intentionally **not** graphed in one shot (file/word limits and noise); extend the detect path in the graphify scripts if you need another module (for example `data-generator-service`).

## Prerequisites

```powershell
pip install graphifyy
```

## Regenerate

See `graphify-out/README.md` for the exact helper scripts (`run_ast.py`, `merge_extract.py`, `run_build.py`, `label_and_refresh.py`) and the graphify skill reference.

## Using the graph in review

1. Open **`graphify-out/graph.html`** and filter by community to walk **sources → runtime → sinks**.
2. Read **`GRAPH_REPORT.md` → God Nodes** to find the highest-fan-out types (good review entry points).
3. Use **Surprising Connections** to spot cross-cutting interfaces (`RowSink`, `TemplateV2RuntimePluginProvider`, etc.).
