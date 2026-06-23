# Milestones

## v1.0 — UDF, Transform & Test Harness

**Shipped:** 2026-06-23
**Phases:** 5 | **Plans:** 18
**Git range:** `79c17b9` → `HEAD` (46 commits, 133 files, +13,276 / −63 lines)

### Summary

Quality-first brownfield release: automated test harness with feature matrix, unified multi-form UDF platform, Template V2 transform operators (json/mask/lookup), and P0 CI regression gate.

### Key Accomplishments

1. **Test harness foundation** — Feature matrix (`.planning/test-matrix.yaml`), `data-generator-test-fixtures`, `scripts/verify-harness.ps1`, `harness-verify.yml` CI, Playwright smoke
2. **UDF platform core** — Unified `UdfRegistry` for java-plugin, script, and SQL types with governance hooks
3. **UDF console & binding** — JDBC persistence, `/api/console/udfs`, React Udfs page, publish-time template validation, in-repo sample UDFs
4. **Transform operators** — json/mask/lookup VOs and runtime, transform catalog API, actionable run-report errors, `V2_JSON_EXTRACT`
5. **Coverage ramp** — P0/P1/P2 tiers, 7/7 P0 green, console API slice tests, merge regression gate documented in `AGENTS.md`

### Archives

- Roadmap: [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md)
- Requirements: [milestones/v1.0-REQUIREMENTS.md](milestones/v1.0-REQUIREMENTS.md)

### Known Gaps at Close

- No `v1.0-MILESTONE-AUDIT.md` (audit skipped; accepted as tech debt)
- Known deferred items at close: 3 (see `STATE.md` Deferred Items)
