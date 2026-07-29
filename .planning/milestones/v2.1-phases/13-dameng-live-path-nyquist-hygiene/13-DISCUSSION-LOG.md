# Phase 13: Dameng Live Path + Nyquist Hygiene - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-26
**Phase:** 13-Dameng Live Path + Nyquist Hygiene
**Areas discussed:** Dameng proof depth, Dameng runtime target, Nyquist phase set, Recipe packaging

---

## Dameng proof depth

| Option | Description | Selected |
|--------|-------------|----------|
| Docs-only recipe | Document enable path; IT may remain placeholder abort | |
| Wire real JDBC and PASS | IT greens when host available; default CI still skips | ✓ |
| You decide | Planner/research default | |

**User's choice:** Wire real JDBC and PASS
**Notes:** Aligns with DIAL-01 “expected PASS” when host exists

| Option | Description | Selected |
|--------|-------------|----------|
| Hard FAIL | Flag on but unreachable → fail | ✓ |
| Soft skip/abort | Abort when unreachable | |
| You decide | | |

**User's choice:** Hard FAIL

| Option | Description | Selected |
|--------|-------------|----------|
| Chunked upsert idempotent | Re-run same PK; assert rows | ✓ |
| Single write + COUNT | One-shot success only | |
| You decide | | |

**User's choice:** Chunked upsert idempotency

| Option | Description | Selected |
|--------|-------------|----------|
| Must reuse UpsertParitySupport | Same as PG/MySQL ITs | ✓ |
| Dameng-specific asserts OK | Fork if needed | |
| You decide | Prefer reuse | |

**User's choice:** Must reuse `UpsertParitySupport.assertUpsertIdempotent`

---

## Dameng runtime target

| Option | Description | Selected |
|--------|-------------|----------|
| External JDBC URL/env | Operator-provided host | ✓ |
| Testcontainers licensed image | Self-boot container | |
| Both, URL preferred | Fallback chain | |

**User's choice:** External JDBC URL/env only

| Option | Description | Selected |
|--------|-------------|----------|
| Dedicated DG_DM_* | URL/USER/PASSWORD + DG_DM_IT | ✓ |
| Generic TEST_JDBC_* | Shared names | |
| You decide | | |

**User's choice:** `DG_DM_JDBC_URL` / `DG_DM_USER` / `DG_DM_PASSWORD`

| Option | Description | Selected |
|--------|-------------|----------|
| Plaintext env | Test-only; warn never commit | ✓ |
| SecretResolver required | Full secret path | |
| You decide | | |

**User's choice:** Plaintext env credentials

| Option | Description | Selected |
|--------|-------------|----------|
| Exclude Testcontainers | Out of scope this phase | ✓ |
| Docs mention future only | No code | |
| Implement container fallback | In scope | |

**User's choice:** Explicitly exclude Testcontainers/licensed image

---

## Nyquist phase set

| Option | Description | Selected |
|--------|-------------|----------|
| Strict DIAL-02 (07/07.1/08) | No Phase 12 | ✓ |
| DIAL-02 + Phase 12 | Also refresh 12-VALIDATION | |
| You decide | Floor DIAL-02 | |

**User's choice:** Strict DIAL-02 only

| Option | Description | Selected |
|--------|-------------|----------|
| Map existing → nyquist_compliant true | No new product tests | ✓ |
| Allow false with documented gaps | | |
| Hybrid | | |

**User's choice:** Map existing tests then mark `nyquist_compliant: true`

| Option | Description | Selected |
|--------|-------------|----------|
| In-place milestones/v2.0-phases | Single source of truth | ✓ |
| Active .planning/phases copies | Risk of drift | |
| You decide | Prefer archive | |

**User's choice:** In-place archive VALIDATION files

| Option | Description | Selected |
|--------|-------------|----------|
| Sync v2.0-MILESTONE-AUDIT Nyquist table | Reduce audit noise | ✓ |
| VALIDATION only | Leave audit for later | |
| You decide | | |

**User's choice:** Sync milestone audit Nyquist table

---

## Recipe packaging

| Option | Description | Selected |
|--------|-------------|----------|
| Extend template-v2-jdbc-sink-guide.md | Dedicated Dameng live section | ✓ |
| New docs/testing-dameng-live-it.md | Separate file | |
| Javadoc only | No docs/ expansion | |

**User's choice:** Extend `docs/template-v2-jdbc-sink-guide.md`

| Option | Description | Selected |
|--------|-------------|----------|
| Add verify-*-dameng*.ps1 | Match other UAT scripts | ✓ |
| Docs Maven commands only | No script | |
| You decide | | |

**User's choice:** Add verify PowerShell script

| Option | Description | Selected |
|--------|-------------|----------|
| Non-zero exit if not configured | Clear usage; not fake UAT green | ✓ |
| Zero exit + warn skip | Optional UAT style | |
| You decide | | |

**User's choice:** Non-zero exit + usage when flag/URL missing

| Option | Description | Selected |
|--------|-------------|----------|
| Add AGENTS.md Commands line | Discoverability | ✓ |
| Skip AGENTS.md | Docs + script only | |
| You decide | | |

**User's choice:** Add AGENTS.md opt-in command pointer

---

## Claude's Discretion

- Exact verify script filename
- Optional system-property mirrors for URL/user/password (env canonical)
- VALIDATION row formatting (follow phase 9)
- Minor javadoc on DamengTestSupport / IT

## Deferred Ideas

- Dameng Testcontainers / licensed image path
- Phase 12 VALIDATION Nyquist refresh
- Dameng live as P0 (DIAL-03)
- Dual JDBC resolver code merge
