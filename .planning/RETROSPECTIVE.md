# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v2.2 — V2 Geo Synthetic Source

**Shipped:** 2026-07-31
**Phases:** 3 | **Plans:** 9 | **Tasks:** 26

### What Was Built

- BBOX/CIRCLE modes in `data-generator-geo` with seeded, in-domain point synthesis
- Template V2 `geo_synthetic` SourceVO, Factory, RowSource, and CoreConfig registration
- Four-mode `TemplateV2Runner` pipeline IT with passthrough SQL and console sink
- `docs/geo-synthetic-v2-source.md` distinguishing `geo_synthetic` vs read-only `geojson`
- P1 harness row `geo-synthetic` with three linked calcite tests; P0 frozen at 15

### What Worked

- Three-phase split (generator → source → proof/docs) kept dependencies linear and auditable
- Reusing `GeoResourceResolver` and existing boundary/line generators minimized new surface area
- Dedicated pipeline IT closed GEO-02 without HTTP or console E2E overhead
- Nyquist VALIDATION backfill from existing VERIFICATION kept closeout honest without new tests

### What Was Inefficient

- Phase timing rows leaked into STATE Deferred Items — should stay in phase SUMMARYs only
- Matrix doc generator requires inline `linked_tests` arrays (empty YAML arrays break column output)
- Milestone shipped in ~2 calendar days — velocity metrics less meaningful at this scale

### Patterns Established

- Geo V2: `geo_synthetic` (synthesis) vs `geojson` (read-only) as distinct source types
- Generator-first then SourceVO: extend `data-generator-geo` before calcite Factory wiring
- P1 proof rows for new source types; defer P0 promotion until merge-gate review

### Key Lessons

1. Split generator and pipeline evidence across phases when REQ spans module boundaries
2. Document source-type distinctions in dedicated doc, not only in overview pages
3. Accept intentional code duplication when shared-helper extraction is deferred (D-10)
4. Run validate-phase Nyquist hygiene before milestone audit to avoid false partials

### Cost Observations

- Model mix: not tracked this milestone
- Timeline: 2026-07-30 → 2026-07-31 (~71 commits, +6,569 LOC)
- Closeout: `tech_debt` accepted; 5/5 requirements, Nyquist compliant

---

## Milestone: v2.0 — Reader/Writer & Datasource Platform

**Shipped:** 2026-07-25
**Phases:** 7 | **Plans:** 36 | **Tasks:** 66

### What Was Built

- Unified `data-generator-datasource` catalog (JDBC/Kafka/ES) with managed resolve
- Snapshot hot-reload governance + DS-03 `snap:` execute-path routing (Phase 07.1)
- Streaming CSV/JSON I/O and PG/MySQL JDBC upsert with run-report metrics
- Five-engine JDBC dialects (DM/KB/HG/PG/CK) + console presets
- 15-row P0 harness gate; Phase 11 managed JDBC E2E + Kingbase evidence pack

### What Worked

- Inserted Phase 07.1 closed a critical audit gap without renumbering the roadmap
- Embedded-first + Testcontainers/PG-proxy pattern kept dialect coverage CI-friendly
- Phase 11 surgical closeout converted PARTIAL E2E flows to OK with accepted limits
- Canonical merge gate (`verify-harness.ps1`) stayed clearer than proliferating UAT scripts

### What Was Inefficient

- Missing Phase 6/7 VERIFICATION.md delayed audit to `gaps_found` until backfill
- Dual JDBC resolvers required a dedicated inserted phase once governance shipped
- Long accomplishment lists from plan summaries need condensation at milestone write-up
- Nyquist validation lagging behind execute phases created audit noise without DoD value

### Patterns Established

- Datasource: ConnectionCatalog resolve-only; CRUD stays in service layer
- Execute-path authority: `DefaultRuntimeJdbcEndpointResolver` owns `snap:` routing
- Dialect evidence: unit SQL + PG-proxy IT + optional licensed-driver IT
- Closeout: dedicated E2E IT + evidence pack + surgical audit edit

### Key Lessons

1. Write phase VERIFICATION.md before milestone audit — prevents false requirement orphans
2. Prefer inserted decimal phases for execute-path gaps discovered after governance lands
3. Document accepted limits (in-process vs HTTP, opt-in drivers) in the audit, not as open gaps
4. Keep UAT scripts supplementary; one harness command as merge gate

### Cost Observations

- Model mix: not tracked this milestone
- Sessions: multi-session across ~31 calendar days
- Notable: Phase 11 (3 plans) unblocked archive after audit `tech_debt` with no new blockers

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Phases | Plans | Key Change |
|-----------|--------|-------|------------|
| v1.0 | 5 | 18 | Quality-first harness before feature breadth |
| v2.0 | 7 | 36 | Insert platform + RW gap closure; inserted 07.1 + closeout 11 |
| v2.1 | 6 | 18 | Hardening proofs; P1 rows without P0 inflation |
| v2.2 | 3 | 9 | First-class geo_synthetic V2 source; P1 harness only |

### Cumulative Quality

| Milestone | P0 rows | Gate | Audit at close |
|-----------|---------|------|----------------|
| v1.0 | 7 | verify-harness | Skipped (accepted) |
| v2.0 | 15 | verify-harness | tech_debt (reqs 13/13; no critical gaps) |
| v2.1 | 15 | verify-harness | tech_debt (reqs 8/8; Nyquist compliant) |
| v2.2 | 15 | verify-harness | tech_debt (reqs 5/5; Nyquist compliant) |

### Top Lessons (Verified Across Milestones)

1. Harness-first delivery keeps regression risk bounded while expanding surface area
2. Milestone audit before archive surfaces proof-depth gaps earlier than plan checklists alone
3. Document deferred tech debt explicitly so the next milestone can pick it up deliberately
