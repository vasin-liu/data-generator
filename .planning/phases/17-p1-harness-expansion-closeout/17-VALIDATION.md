---
phase: 17
slug: p1-harness-expansion-closeout
status: pending
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-29
---

# Phase 17 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Research skipped (`research_enabled=false`); checks are matrix/doc/harness-oriented for TEST-09 wiring.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | `.planning/test-matrix.yaml` SSOT + `scripts/verify-harness.ps1` + JUnit linked ITs |
| **Config file** | Matrix yaml; harness writes `target/test-matrix-summary.json` |
| **Quick run command** | `rg -n "exec-http-managed-catalog\|rbac-enable-path" .planning/test-matrix.yaml docs/test-feature-matrix.md` |
| **Full suite command** | `powershell -NoProfile -File scripts/verify-harness.ps1 -SkipPlaywright` |
| **Focused Maven slice** | `.\mvnw-jdk25.ps1 -pl data-generator-service -am "-Dtest=ManagedJdbcCatalogHttpExecuteIT,ConsoleAuthorizationIntegrationIT" -Dsurefire.failIfNoSpecifiedTests=false test` |
| **Estimated runtime** | ~5–15 min full harness; ~2–4 min focused Maven slice |

---

## Sampling Rate

- **After every task commit:** Quick grep/static checks from Per-Task Verification Map
- **After Wave 1 (plan 17-01):** Matrix row + P0 count invariant grep
- **After Wave 2 (plan 17-02):** Full harness + summary JSON `p0` block check
- **After Wave 3 (plan 17-03):** Doc + planning state grep bundle
- **Before `/gsd-verify-work`:** Harness green; TEST-09 marked complete in REQUIREMENTS
- **Max feedback latency:** 900 seconds (full harness path)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 17-01-01 | 01 | 1 | TEST-09 | Pitfall 8 | P0 baseline captured before edits | static | `(rg -c "tier: P0" .planning/test-matrix.yaml) -eq 15` | ✅ | ⬜ pending |
| 17-01-02 | 01 | 1 | TEST-09 | — | HTTP P1 rows with correct linked_tests | static | `rg -A6 "id: exec-http-managed-catalog" .planning/test-matrix.yaml \| Select-String "ManagedJdbcCatalogHttpExecuteIT"` | ❌ W0 | ⬜ pending |
| 17-01-03 | 01 | 1 | TEST-09 | — | RBAC P1 row + dist row not duplicated | static | `rg -n "id: rbac-enable-path" .planning/test-matrix.yaml`; `rg -A8 "id: dist-multi-jvm-worker" .planning/test-matrix.yaml \| Select-String "linked_tests: \[\]"` | ❌ W0 | ⬜ pending |
| 17-01-04 | 01 | 1 | TEST-09 | Pitfall 8 | P0 ids/membership unchanged | static | `(rg -c "tier: P0" .planning/test-matrix.yaml) -eq 15`; `git diff scripts/verify-harness.ps1` empty | ✅ | ⬜ pending |
| 17-02-01 | 02 | 2 | TEST-09 | — | test-feature-matrix.md regenerated | docs | `rg -n "exec-http-managed-catalog" docs/test-feature-matrix.md` | ✅ doc | ⬜ pending |
| 17-02-02 | 02 | 2 | TEST-09 | — | p0.total=15, p0.pass=true | script | `powershell -NoProfile -File scripts/verify-harness.ps1 -SkipPlaywright`; JSON parse | ✅ harness | ⬜ pending |
| 17-02-03 | 02 | 2 | TEST-09 | — | Four TEST-09 ids in summary JSON | script | `Select-String -Path target/test-matrix-summary.json -Pattern "exec-http-managed-catalog\|rbac-enable-path\|dist-multi-jvm-worker"` | ✅ | ⬜ pending |
| 17-02-04 | 02 | 2 | TEST-09 | D-10 | EXEC-02 skipped-conditional acceptable | script | Harness exit 0 when PG IT skipped; row status may be skipped-conditional | ✅ IT | ⬜ pending |
| 17-02-05 | 02 | 2 | TEST-09 | — | EXEC-01 Maven linkage green | junit | `mvnw-jdk25.ps1 -pl data-generator-service -Dtest=ManagedJdbcCatalogHttpExecuteIT test` | ✅ IT | ⬜ pending |
| 17-03-01 | 03 | 3 | TEST-09 | — | test-harness.md Phase 17 subsection | docs | `rg -n "Phase 17\|exec-http-postgres-dialect\|non-blocking" docs/test-harness.md` | ✅ | ⬜ pending |
| 17-03-02 | 03 | 3 | TEST-09 | — | AGENTS P1 count + supplementary scripts | docs | `rg -n "verify-multi-jvm-worker\|verify-rbac-enable\|Phase 17" AGENTS.md` | ✅ | ⬜ pending |
| 17-03-03 | 03 | 3 | TEST-09 | — | REQUIREMENTS TEST-09 complete | docs | `rg "\[x\].*TEST-09" .planning/REQUIREMENTS.md` | ✅ | ⬜ pending |
| 17-03-04 | 03 | 3 | TEST-09 | — | ROADMAP/MILESTONES/STATE closeout | docs | `rg "17-03-PLAN\|Ready for closeout\|Phase 17" .planning/ROADMAP.md .planning/MILESTONES.md .planning/STATE.md` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Map synced to 3-plan / 3-wave breakdown at `/gsd-plan-phase 17` (research skipped).*

---

## Wave 0 Requirements

- [ ] `.planning/test-matrix.yaml` — three new P1 rows + verified `dist-multi-jvm-worker`
- [ ] `docs/test-feature-matrix.md` — regenerated
- [ ] `docs/test-harness.md` — Phase 17 subsection
- [ ] `AGENTS.md` — P1 expansion pointer
- [ ] `.planning/REQUIREMENTS.md` — TEST-09 complete

*Existing (reuse, do not re-implement):*

- `ManagedJdbcCatalogHttpExecuteIT`, `ManagedJdbcCatalogHttpPostgresUpsertIT` (Phase 12)
- `ConsoleAuthorizationIntegrationIT`, `ConsoleSecurityDefaultOffIT`, `ConsoleAuthorizationFilterTest` (Phase 16)
- `scripts/verify-multi-jvm-worker.ps1` (Phase 15)
- `scripts/verify-rbac-enable.ps1` (Phase 16)
- `dist-multi-jvm-worker` matrix row (Phase 15)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| EXEC-02 with Docker | D-10 | Docker daemon variance | Run full Postgres IT on Docker host; confirm row status `covered` vs `skipped-conditional` without Docker |
| Multi-JVM script smoke | DIST-01 | Port 9876 / host JVM | Run `.\scripts\verify-multi-jvm-worker.ps1`; confirm dist row notes still accurate |
| RBAC supplementary UAT | SEC-01 | Optional Playwright | Run `.\scripts\verify-rbac-enable.ps1` without `-SkipPlaywright` on Podman host |

*Default merge bar remains 15-row P0; all Phase 17 rows are P1 non-blocking.*

---

## Scope Guard Checks (D-17)

Run after phase execution to confirm non-goals held:

```powershell
# No P0 inflation
(rg -c "tier: P0" .planning/test-matrix.yaml) -eq 15

# No new P0 row ids
rg "tier: P0" .planning/test-matrix.yaml | Select-String "exec-http|rbac-enable|dist-multi"

# Harness gate script untouched
git diff scripts/verify-harness.ps1  # expect empty

# No milestone archive unless requested
git diff --name-only milestones/  # expect empty

# No Phase 12-16 IT edits unless linkage fix
git diff data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/
git diff data-generator-service/src/test/java/org/gensokyo/data/security/

# Supplementary scripts not in linked_tests for script-primary rows
rg -A8 "id: dist-multi-jvm-worker" .planning/test-matrix.yaml | Select-String "linked_tests: \[\]"
```

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 900s (full harness path)
- [ ] `nyquist_compliant: true` set in frontmatter after `/gsd-verify-work`

**Approval:** pending

