# Phase 10: Harness Coverage & CI Gates - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-22
**Phase:** 10-harness-coverage-ci-gates
**Areas discussed:** P0 merge-blocking set, Dialect covered bar, Matrix row shape, Expansion scope
**Language:** 中文讨论

---

## P0 merge-blocking set

### Q1 Streaming CSV/JSON tier
| Option | Description | Selected |
|--------|-------------|----------|
| Both P0 | Merge-blocking for both | ✓ |
| One P0 one P1 | Split | |
| Both P1 | Track only | |
| You decide | Claude discretion | |

**User's choice:** 1 — Both P0

### Q2 JDBC upsert tier
| Option | Description | Selected |
|--------|-------------|----------|
| P0 | Merge-blocking | ✓ |
| P1 | Track only | |
| Split PG/MySQL tiers | | |
| You decide | | |

**User's choice:** 1 — P0

### Q3 Dialect P0 membership
| Option | Description | Selected |
|--------|-------------|----------|
| All five P0 | DM, KB, HG, PG, CK | ✓ |
| PG+CK P0; DM/KB/HG P1 | | |
| PG+CK+KB+HG P0; DM P1 | | |
| You decide | | |

**User's choice:** 1 — All five P0

### Q4 CI failure strategy
| Option | Description | Selected |
|--------|-------------|----------|
| Strict covered-only | Non-covered P0 fails gate | ✓ |
| Allow partial/skipped | | |
| Staged pending then promote | | |
| You decide | | |

**User's choice:** 1 — Strict

---

## Dialect covered bar

### Q1 Dameng
| Option | Description | Selected |
|--------|-------------|----------|
| MERGE unit tests = covered | Align D-13/D-14 | ✓ |
| Must run real DM IT | | |
| Unit + validator | | |
| You decide | | |

**User's choice:** 1

### Q2 Kingbase/HighGo
| Option | Description | Selected |
|--------|-------------|----------|
| PG-proxy IT + mapping units | Align D-15 | ✓ |
| Real KB/HG engines | | |
| Mapping unit only | | |
| You decide | | |

**User's choice:** 1

### Q3 PostgreSQL/ClickHouse
| Option | Description | Selected |
|--------|-------------|----------|
| Testcontainers IT | | ✓ |
| Unit only | | |
| IT + phase UAT in CI | | |
| You decide | | |

**User's choice:** 1

### Q4 Optional gated DM IT
| Option | Description | Selected |
|--------|-------------|----------|
| Not in P0 linked_tests | notes only | ✓ |
| Link as skipped-conditional | | |
| Separate P2 row | | |
| You decide | | |

**User's choice:** 1

---

## Matrix row shape

### Q1 Streaming split
| Option | Description | Selected |
|--------|-------------|----------|
| Two independent P0 rows | CSV + JSON | ✓ |
| One combined row | | |
| Up to four reader/writer | | |
| You decide | | |

**User's choice:** 1

### Q2 Upsert split
| Option | Description | Selected |
|--------|-------------|----------|
| One row linking PG+MySQL | | ✓ |
| Two engine rows | | |
| Upsert vs insert split | | |
| You decide | | |

**User's choice:** 1

### Q3 Dialect split
| Option | Description | Selected |
|--------|-------------|----------|
| Five independent P0 rows | | ✓ |
| Three grouped | | |
| One all-dialects | | |
| You decide | | |

**User's choice:** 1

### Q4 linked_tests source
| Option | Description | Selected |
|--------|-------------|----------|
| Reuse Phase 8/9 classes | No mandatory new fixtures | ✓ |
| New fixtures per row | | |
| Maven + Playwright P0 | | |
| You decide | | |

**User's choice:** 1

---

## Expansion scope

### Q1 Matrix scope
| Option | Description | Selected |
|--------|-------------|----------|
| Strict TEST-07 | No Phase 6–7 DS rows | ✓ |
| TEST-07 + DS P1 | | |
| TEST-07 + DS P0 | | |
| You decide | | |

**User's choice:** 1

### Q2 CI workflow
| Option | Description | Selected |
|--------|-------------|----------|
| No harness-verify.yml change | Gate via matrix only | ✓ |
| Add CI infra for containers | | |
| Also run phase UAT scripts | | |
| You decide | | |

**User's choice:** 1

### Q3 Documentation
| Option | Description | Selected |
|--------|-------------|----------|
| AGENTS.md + docs/test-harness.md | | ✓ |
| AGENTS.md only | | |
| Plus generate-test-matrix-doc | | |
| You decide | | |

**User's choice:** 1

### Q4 Drive-by obligations
| Option | Description | Selected |
|--------|-------------|----------|
| None | Matrix + docs + auto gate | ✓ |
| Regenerate matrix doc | | |
| Retire old pending rows | | |
| You decide | | |

**User's choice:** 1

---

## Claude's Discretion

- Exact matrix id/capability naming strings
- Exact Phase 8/9 class → row mapping
- Optional non-blocking P1 companion rows
- Optional local matrix-doc regenerate (not a success criterion)

## Deferred Ideas

- Phase 6–7 DS matrix expansion
- CI workflow rewrite / UAT aggregation into harness-verify
- Playwright as P0
- Licensed DM/KB/HG images in default CI
- Exhaustive pending-row cleanup
