# Phase 12: HTTP Execute-Path Proof - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-25
**Phase:** 12-HTTP Execute-Path Proof
**Areas discussed:** HTTP entry, Async completion, Dialect packaging, snap binding depth

---

## HTTP entry

| Option | Description | Selected |
|--------|-------------|----------|
| POST /task/run/{id} | Classic TaskController spine | ✓ |
| Console /api/templates/{id}/run | Operator console path | |
| Both endpoints | Broader proof, higher cost | |
| You decide | Defer to Claude | |

**User's choice:** POST /task/run/{id}

| Option | Description | Selected |
|--------|-------------|----------|
| Publish then run | Real publish gate | ✓ |
| Disable publish gate in test YAML | Faster, weaker | |
| You decide | | |

**User's choice:** Publish then run

| Option | Description | Selected |
|--------|-------------|----------|
| Service/repo seed + publish + MockMvc run | Recommended | ✓ |
| Full HTTP create→publish→run | Heavier E2E | |
| You decide | | |

**User's choice:** Service layer seed + publish + MockMvc run

| Option | Description | Selected |
|--------|-------------|----------|
| Parse instanceId= from R.ok message | Existing regex pattern | ✓ |
| Query latest task_execution row | Race-prone | |
| You decide | | |

**User's choice:** Parse instanceId= from message

---

## Async completion

| Option | Description | Selected |
|--------|-------------|----------|
| Poll TaskExecution repo/service | Recommended | ✓ |
| MockMvc poll /api/jobs | More console-like | |
| Fixed sleep | Flaky | |
| You decide | | |

**User's choice:** Poll TaskExecution repository/service

| Option | Description | Selected |
|--------|-------------|----------|
| SUCCESS + managed-pool COUNT(*) | Phase 11 evidence bar | ✓ |
| SUCCESS status only | Weaker | |
| SUCCESS + run report metrics | Heavier | |
| You decide | | |

**User's choice:** SUCCESS + COUNT(*)

| Option | Description | Selected |
|--------|-------------|----------|
| 30–60s timeout + fail on FAILED/CANCELLED | Recommended | ✓ |
| Timeout only | Weaker failure signal | |
| You decide | | |

**User's choice:** Timeout + immediate fail on FAILED/CANCELLED

---

## Dialect packaging

| Option | Description | Selected |
|--------|-------------|----------|
| Separate IT from EXEC-01 | Recommended | ✓ |
| Same IT sequential | More brittle | |
| You decide | | |

**User's choice:** Separate IT

| Option | Description | Selected |
|--------|-------------|----------|
| Testcontainers PostgreSQL | Recommended | ✓ |
| Kingbase-proxy | Closer to Phase 11 pack, more complex | |
| H2 MySQL-mode dialect stand-in | Weak proof | |
| You decide | | |

**User's choice:** Testcontainers PostgreSQL

| Option | Description | Selected |
|--------|-------------|----------|
| Managed id + ON CONFLICT upsert | Recommended | ✓ |
| Plain INSERT only | Weaker EXEC-02 | |
| You decide | | |

**User's choice:** Managed dataSourceId + dialect upsert

---

## snap binding depth

| Option | Description | Selected |
|--------|-------------|----------|
| Unbound managed id via HTTP only | Recommended for Phase 12 | ✓ |
| Must assert snap: on HTTP path | Stronger, heavier | |
| Both unbound + snap HTTP | Max proof | |
| You decide | | |

**User's choice:** Unbound managed dataSourceId via HTTP only

---

## Claude's Discretion

- Poll interval/backoff within 30–60s
- IT naming/package placement
- H2 naming reuse from Phase 11
- Publish via lifecycle service vs MockMvc publish endpoint (as long as published before run)

## Deferred Ideas

- Console run API as additional evidence
- HTTP-path snap: assertion
- Dameng / multi-JVM / RBAC / P1 (later phases)
