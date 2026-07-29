# Phase 16: RBAC Enable Path - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-29
**Phase:** 16-RBAC Enable Path
**Mode:** --auto (all gray areas auto-selected; recommended defaults)
**Areas discussed:** Documentation packaging, Maven IT proof, Playwright E2E proof, Default-off regression guard, Verify script, Profile isolation, Harness linkage (deferred)

---

## Documentation packaging

| Option | Description | Selected |
|--------|-------------|----------|
| Extend `operator-console-usage.md` only | Add enable recipe to existing config section | |
| New focused doc + AGENTS.md pointer + cross-link (recommended) | `docs/staging-console-rbac.md` as source of truth; light cross-link from operator-console-usage | ✓ |
| Staging runbook under `.planning/` only | Planning artifact, not operator-facing | |

**Auto-selected:** New focused doc + AGENTS.md pointer + cross-link from `docs/operator-console-usage.md`
**Notes:** Matches Phase 13–15 docs packaging pattern; staging profile remains `application-staging.yaml`.

---

## Maven IT proof scope

| Option | Description | Selected |
|--------|-------------|----------|
| Filter unit tests only | Fast but no full HTTP stack proof | |
| Keep `ConsoleAuthorizationIntegrationIT` + add default-off regression (recommended) | Canonical deny/allow on live context + guard against default-on | ✓ |
| New large RBAC matrix IT covering every endpoint | Over-scoped for SEC-01 | |

**Auto-selected:** Keep existing IT + add default-off regression test
**Notes:** `ConsoleAuthorizationIntegrationIT` already covers missing header, VIEWER create deny, EDITOR publish deny, governance on draft run.

---

## Playwright E2E proof scope

| Option | Description | Selected |
|--------|-------------|----------|
| Write new RBAC E2E specs from scratch | Duplicates existing `rbac.*.spec.ts` | |
| Document existing gated specs + `e2e-podman.ps1` path (recommended) | `DG_E2E_RBAC=true` + `e2e-rbac` profile already wired | ✓ |
| Require RBAC specs in default CI Podman gate | Would force headers on all e2e; breaks default-off contract | |

**Auto-selected:** Document existing specs; add new specs only if audit finds gaps
**Notes:** Distinguish `DG_E2E_RBAC` vs `DG_E2E_GOVERNANCE_STAGING` in operator doc.

---

## Default-off regression guard

| Option | Description | Selected |
|--------|-------------|----------|
| Rely on Java field default only | No CI guard if yaml changes | |
| Explicit IT/properties test asserting default-off (recommended) | Catches profile bleed and accidental base yaml enable | ✓ |
| Add `enabled: false` comment to base `application.yaml` | Documentation-only; weaker than test | |

**Auto-selected:** Explicit regression test + profile contract verification (D-09)
**Notes:** Addresses research Pitfall 4 (accidental RBAC default-on).

---

## Verify script

| Option | Description | Selected |
|--------|-------------|----------|
| No dedicated script; point to `verify-console.ps1` only | Full pipeline is heavy for RBAC-only check | |
| Add `scripts/verify-rbac-enable.ps1` (recommended) | Maven RBAC slice + optional Playwright; mirrors Phase 13/15 | ✓ |
| Maven only, no script | Less operator-friendly | |

**Auto-selected:** `scripts/verify-rbac-enable.ps1` with `-SkipPlaywright` option
**Notes:** Not a P0 merge gate.

---

## Harness / matrix linkage

| Option | Description | Selected |
|--------|-------------|----------|
| Add P1 row in Phase 16 | Combines SEC-01 with TEST-09 | |
| Defer P1 row to Phase 17 (recommended) | ROADMAP separates SEC-01 (16) from TEST-09 (17) | ✓ |
| Promote to P0 | Explicitly out of scope per milestone research | |

**Auto-selected:** Defer P1 matrix row to Phase 17
**Notes:** Phase 16 delivers proof artifacts and docs; Phase 17 wires matrix.

---

## Distributed + RBAC combined proof

| Option | Description | Selected |
|--------|-------------|----------|
| Require multi-JVM + RBAC E2E in Phase 16 | Phase 15 deferred this; expands SEC-01 scope | |
| Out of scope; document profile isolation (recommended) | `application-distributed-staging.yaml` stays RBAC-off | ✓ |

**Auto-selected:** Combined distributed+RBAC proof out of SEC-01 done criteria
**Notes:** Phase 15 multi-JVM verify script remains header-free.

---

## Claude's Discretion

- Exact doc and test class naming
- Verify script flag alignment with sibling scripts
- Optional positive allow-path IT assertion (VIEWER GET → 200)
- Minor role→permission doc table from enums

## Deferred Ideas

- P1 matrix row (`rbac-enable-path`) — Phase 17 / TEST-09
- Multi-JVM + RBAC combined runs — future hardening
- Default-on production RBAC (SEC-02) — beyond v2.1
- OAuth2/JWT/IdP console auth — new capability phase
