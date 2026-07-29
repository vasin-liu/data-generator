---
phase: 15
slug: multi-jvm-worker-e2e
status: complete
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-29
---

# Phase 15 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Research skipped (`research_enabled=false`); checks are grep/script-oriented for the script-primary DIST-01 path.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | PowerShell verify script + existing JUnit 5 embedded ITs (unchanged) |
| **Config file** | `application-distributed-staging.yaml` + coordinator/worker profile fragments |
| **Quick run command** | `rg -n "verify-multi-jvm-worker" scripts/ .planning/test-matrix.yaml docs/staging-distributed-deployment.md AGENTS.md` |
| **Full suite command** | `powershell -NoProfile -File scripts/verify-multi-jvm-worker.ps1 -SkipMavenPreflight` |
| **Estimated runtime** | ~30–120s (first run includes Maven classpath build); embedded preflight +60–90s if enabled |

---

## Sampling Rate

- **After every task commit:** Quick grep/static checks from Per-Task Verification Map
- **After Wave 1 (plan 15-01):** Full `verify-multi-jvm-worker.ps1` smoke on JDK 25 host with port 9876 free
- **After Wave 2 (plan 15-02):** `verify-harness.ps1 -SkipPlaywright` + summary JSON row check
- **After Wave 3 (plan 15-03):** Doc + AGENTS grep bundle
- **Before `/gsd-verify-work`:** Full verify script green; P0 gate unchanged
- **Max feedback latency:** 120 seconds (script path)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 15-01-01 | 01 | 1 | DIST-01 | — | Minimal template uses console sink only; no credential echo | static | `rg -n "New-DistributedMinimalIteratorConsoleTemplate\|Wait-DistributedDualSuccess" scripts/lib/distributed-staging-rest.ps1` | ❌ W0 | ⬜ pending |
| 15-01-02 | 01 | 1 | DIST-01 | — | Host JVM helpers spawn background processes, not interactive shells | static | `rg -n "Start-DistributedHostJvm\|DataGeneratorWorkerApplication" scripts/lib/distributed-host-jvm.ps1` | ❌ W0 | ⬜ pending |
| 15-01-03 | 01 | 1 | DIST-01 | — | Script exits non-zero on failure; no Podman gate | script | `rg -n "distributed-staging,distributed-coordinator" scripts/verify-multi-jvm-worker.ps1`; `rg "e2e-distributed-podman" scripts/verify-multi-jvm-worker.ps1` → no match | ❌ W0 | ⬜ pending |
| 15-01-04 | 01 | 1 | DIST-01 | — | Single-JVM ITs untouched | git | `git diff --name-only data-generator-service/src/test/java/org/gensokyo/data/task/Distributed*.java` → empty | ✅ | ⬜ pending |
| 15-01-05 | 01 | 1 | DIST-01 | — | End-to-end dual SUCCESS | script | `powershell -NoProfile -File scripts/verify-multi-jvm-worker.ps1 -SkipMavenPreflight`; expect exit 0 + `[SUCCESS]` | ❌ W0 | ⬜ pending |
| 15-02-01 | 02 | 2 | DIST-01 | — | P1 row only; P0 count stable | static | `rg -A6 "id: dist-multi-jvm-worker" .planning/test-matrix.yaml \| Select-String "tier: P1"`; `(rg -c "tier: P0" .planning/test-matrix.yaml) -eq 15` | ❌ W0 | ⬜ pending |
| 15-02-02 | 02 | 2 | DIST-01 | — | Harness summary surfaces row; P0.pass unchanged | script | `powershell -NoProfile -File scripts/verify-harness.ps1 -SkipPlaywright`; `Select-String dist-multi-jvm-worker target/test-matrix-summary.json` | ✅ harness | ⬜ pending |
| 15-03-01 | 03 | 3 | DIST-01 | — | Runbook documents dual SUCCESS + cleanup | docs | `rg -n "DIST-01 local verify\|verify-multi-jvm-worker\|distributed_job.status" docs/staging-distributed-deployment.md` | ✅ doc | ⬜ pending |
| 15-03-02 | 03 | 3 | DIST-01 | — | AGENTS pointer; no harness gate edits | docs | `rg -n "verify-multi-jvm-worker" AGENTS.md`; `git diff scripts/verify-harness.ps1` empty | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Map synced to 3-plan / 3-wave breakdown at `/gsd-plan-phase 15` (research skipped).*

---

## Wave 0 Requirements

- [ ] `scripts/verify-multi-jvm-worker.ps1` — primary DIST-01 proof script
- [ ] `scripts/lib/distributed-host-jvm.ps1` — host coordinator/worker lifecycle
- [ ] `scripts/lib/distributed-staging-rest.ps1` — minimal template + dual SUCCESS poll helpers
- [ ] `.planning/test-matrix.yaml` — `dist-multi-jvm-worker` P1 row
- [ ] `docs/staging-distributed-deployment.md` — DIST-01 local verify subsection
- [ ] `AGENTS.md` — Commands pointer

*Existing:* embedded distributed ITs (`DistributedSplitRoleIntegrationTests`, etc.) remain fast feedback — not modified.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Full verify on maintainer laptop | DIST-01 | Port 9876 conflicts / JDK path variance | Free port 9876; run `.\scripts\verify-multi-jvm-worker.ps1`; confirm `[SUCCESS]` and dual SUCCESS in output |
| P1 row status → covered | D-03 discretion | Matrix status is manual until script green | After script PASS, set row `status: covered` in test-matrix.yaml and regenerate doc |

*Default merge bar remains 15-row P0; P1 multi-JVM row is non-blocking.*

---

## Scope Guard Checks (D-10)

Run after phase execution to confirm non-goals held:

```powershell
# No P0 promotion
rg -A3 "id: dist-multi-jvm-worker" .planning/test-matrix.yaml | Select-String "tier: P1"

# Embedded ITs untouched
git log -1 --name-only -- data-generator-service/src/test/java/org/gensokyo/data/task/DistributedSplitRoleIntegrationTests.java

# Podman not required for done gate
rg -n "verify-multi-jvm-worker" scripts/verify-harness.ps1  # expect no match (script not in harness Maven slice)
```

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s (script quick path)
- [ ] `nyquist_compliant: true` set in frontmatter after `/gsd-verify-work`

**Approval:** pending

