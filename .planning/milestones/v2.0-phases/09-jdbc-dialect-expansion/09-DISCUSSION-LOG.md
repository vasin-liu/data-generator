# Phase 9: JDBC Dialect Expansion - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-21
**Phase:** 9-JDBC Dialect Expansion
**Areas discussed:** Per-dialect upsert/merge, Dialect identity in sink YAML, Console presets & connectivity, Embedded test strategy

---

## Per-dialect upsert / merge semantics

### Kingbase / HighGo upsert SQL

| Option | Description | Selected |
|--------|-------------|----------|
| Reuse PostgreSQL `ON CONFLICT` path | Same Phase 8 PG implementation | ✓ |
| Separate dialect keys + generators | Even if SQL is identical | |
| You decide | Idempotent re-run semantics only | |

**User's choice:** Reuse PostgreSQL `ON CONFLICT` path

### Dameng upsert strategy

| Option | Description | Selected |
|--------|-------------|----------|
| `MERGE INTO` with upsert + upsertKeys YAML | Same knobs, different SQL | ✓ |
| INSERT only; reject upsert for DM | Defer upsert | |
| You decide | Prefer no silent failure | |

**User's choice:** `MERGE INTO`

### ClickHouse `upsert: true`

| Option | Description | Selected |
|--------|-------------|----------|
| Hard reject + operator docs | No fake upsert | ✓ |
| Allow; plain INSERT + strong warning | Table-engine users | |
| INSERT + ReplacingMergeTree docs only | Soft “semantic upsert” | |

**User's choice:** Hard reject + docs

### Fail timing for unsupported capabilities

| Option | Description | Selected |
|--------|-------------|----------|
| Publish + run dual fail-fast | Phase 8 D-14 parity | ✓ |
| Run-time only | Publish allowed | |
| Docs warn only | No block | |

**User's choice:** Publish + run dual fail-fast

---

## Dialect identity in sink YAML

### Where dialect comes from

| Option | Description | Selected |
|--------|-------------|----------|
| Explicit `options.dialect` required | Primary Phase 9 path | ✓ |
| Auto-detect from URL/driver | YAML optional | |
| Explicit preferred; fallback auto-detect | Conflict → explicit + warn | |

**User's choice:** Explicit required

### Kingbase / HighGo YAML key

| Option | Description | Selected |
|--------|-------------|----------|
| Independent `kingbase` / `highgo` keys, map to PG path | Readable templates | ✓ |
| Always write `postgres` | Simpler YAML | |
| Accept all three as equivalent | `kingbase`/`highgo`/`postgres` | |

**User's choice:** Independent keys with internal PG mapping

### Dialect vs connection mismatch

| Option | Description | Selected |
|--------|-------------|----------|
| YAML dialect wins for SQL; connectivity tests connection only | No hard mismatch check | ✓ |
| Publish/run require dialect↔driver/URL family match | Fail-fast | |
| Warn only | Allow run | |

**User's choice:** YAML dialect is source of truth; no hard mismatch check

### `generic` + `upsert: true`

| Option | Description | Selected |
|--------|-------------|----------|
| Publish + run fail-fast | No silent ignore | ✓ |
| Keep Phase 8 ignore behavior | Compat | |
| Draft warn; publish OK; run ignore | Middle ground | |

**User's choice:** Fail-fast

---

## Console presets & connectivity validation

### RW-06 depth

| Option | Description | Selected |
|--------|-------------|----------|
| Complete presets + connectivity for five engines | No extra hint/link UI | ✓ |
| + dialect capability hints after preset | Upsert/bulk tips | |
| + template editor dialect↔preset linking | Deeper UX | |

**User's choice:** Presets + connectivity only  
**Notes:** Capability hints and template linking deferred

### Proprietary driver packaging

| Option | Description | Selected |
|--------|-------------|----------|
| Keep in `jdbc-bundled/`, `bundled: true` | Parity with MySQL/PG | ✓ |
| Do not bundle; operator supplies drivers | License caution | |
| You decide | Follow existing packaging | |

**User's choice:** Bundle with `bundled: true`

### Connectivity error detail

| Option | Description | Selected |
|--------|-------------|----------|
| Actionable summary; no password / full URL | Phase 7 hygiene | ✓ |
| Include driver exception stack summary | Faster triage | |
| Generic “connection failed” only | Least leak | |

**User's choice:** Actionable summary without secrets/full URL

### Console verification

| Option | Description | Selected |
|--------|-------------|----------|
| API/unit preset tests + Playwright one preset-fill-save | Balanced | ✓ |
| Backend/API only | No Playwright | |
| Full Playwright five engines | Highest cost | |

**User's choice:** API/unit + one Playwright path

---

## Embedded test strategy

### How to prove five engines

| Option | Description | Selected |
|--------|-------------|----------|
| Layered: TC for PG/CK; PG proxy + mapping for KB/HG; DM MERGE unit + optional IT; CK reject contracts | No prod credentials | ✓ |
| SQL unit tests only | Fastest, weakest runtime | |
| Require real/private DM/KB/HG images in CI | Highest fidelity | |

**User's choice:** Layered strategy

### Dameng optional IT default

| Option | Description | Selected |
|--------|-------------|----------|
| Skip by default; document enable flag | CI stays green | ✓ |
| Fail if image missing | Hard dependency | |
| Never run real DM IT | Unit only forever | |

**User's choice:** Skip by default with enable flag

### Do Kingbase/HighGo proxy tests count for success criteria?

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, with docs stating proxy + mapping | Meets “no prod creds” | ✓ |
| No; need real Kingbase/HighGo | Stricter | |
| You decide | Planner picks green CI path | |

**User's choice:** Proxy counts with documentation

### Phase 9 UAT script

| Option | Description | Selected |
|--------|-------------|----------|
| New `scripts/verify-phase9-uat-jdbc-dialect.ps1` (`-SkipPlaywright`) | Mirror 6/7/8 | ✓ |
| Extend Phase 8 script only | No new script | |
| Maven only; UAT script in Phase 10 | Defer script | |

**User's choice:** New phase9 verify script

---

## Claude's Discretion

- Exact Dameng MERGE SQL shape
- Internal `kingbase`/`highgo` → PG upsert mapping implementation
- Optional DM IT flag naming
- Playwright file naming / fixture layout

## Deferred Ideas

- Console dialect capability hints after preset selection
- Template editor dialect dropdown linked to datasource preset
- Harness P0 rows / CI gates for dialects (Phase 10)
- Default-CI requirement for licensed DM/Kingbase/HighGo images
