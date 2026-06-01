# V1 Retirement Deferred Ops (Merge Package) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close `feature-4.0` as a **capability merge** while retirement P3 production promote and P4 cutover remain **M2** (staging required), per `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md`.

**Architecture:** Docs-first merge gate (M1/M2 labels, MR narrative, staging checklist). Verification via existing Maven/CI tests—no new runtime features required for merge. Optional P1 hardening: reject promote without business sign-off when inventory row exists.

**Tech Stack:** Java 25, Maven (`mvnw-jdk25.ps1`), Spring Boot 4, existing migration REST + CI test classes.

**Spec:** `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md` (Approved 2026-05-21)

---

## File map

| Action | Path | Responsibility |
|--------|------|----------------|
| Modify | `docs/migration/MR-feature-4.0.md` | Merge narrative: capabilities + M1/M2 retirement |
| Modify | `docs/migration/staging-runbook.md` | M2-only banner + link to deferred ops spec |
| Create | `docs/migration/staging-readiness-checklist.md` | Ops prep template (no execution) |
| Modify | `docs/migration/retirement-readiness.md` | Evidence substitutes table (if not already complete) |
| Optional modify | `MigrationPromoteService.java` | Require sign-off before promote |
| Optional test | `MigrationPromoteServiceTests.java` | Failing test for unsigned promote |

---

## Task 1: Merge verification commands

**Files:** None (verification only)

- [x] **Step 1: Run retirement CI slice**

```powershell
cd D:\Work\99_Code\data-generator
.\mvnw-jdk25.ps1 -pl data-generator-service -am `
  "-Dtest=BuiltinClasspathTemplateRegressionTests,BuiltinClasspathTemplateMigrationWorkflowTests,MigrationWaveCohortSignoffTests,StagingSimulatedPromoteWorkflowTests,TemplateV2ControlPlaneServiceTests,TaskControllerV1ExecutionFlagTests,MigrationPromoteServiceTests,TemplateControllerMigrationDraftTests" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: `BUILD SUCCESS`, 0 failures.

- [x] **Step 2: Run calcite + service smoke (broader gate before merge)**

```powershell
.\mvnw-jdk25.ps1 -pl "data-generator-calcite,data-generator-service" -am test
```

Expected: `BUILD SUCCESS`. Note: Testcontainers MySQL/Postgres tests skip when Docker unavailable—acceptable per `docs/testing-embedded-components.md`.

- [x] **Step 3: Record result in MR**

Add a line under **Test plan** in `docs/migration/MR-feature-4.0.md`:

```markdown
- [x] Retirement M1 CI slice (YYYY-MM-DD): BuiltinClasspath*, MigrationWaveCohort*, StagingSimulatedPromote*, control plane, v1 flag — BUILD SUCCESS
```

Replace `YYYY-MM-DD` with actual run date.

---

## Task 2: Update merge request document

**Files:**
- Modify: `docs/migration/MR-feature-4.0.md`

- [x] **Step 1: Add retirement section after Summary**

Insert after the Summary bullet list:

```markdown
## V1 retirement (merge vs M2)

This MR **delivers retirement capabilities**; it does **not** complete production retirement.

| Milestone | In this MR? | Evidence |
|-----------|-------------|----------|
| **M1** (no staging) | Yes | CI: `BuiltinClasspathTemplate*`, `MigrationWaveCohortSignoffTests`, `StagingSimulatedPromoteWorkflowTests`; spec: `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md` |
| **M2** (staging) | No | Real `db-{id}` promote, batch compare, `v1-execution.enabled=false` — post-merge ops |

**Do not merge-block on:** production promote checkbox, wave-freeze calendar dates, staging runbook execution.
```

- [x] **Step 2: Update Out of scope — remove stale items**

Replace the "Out of scope" bullets that are now delivered:

```markdown
## Out of scope (post-merge / M2)

- Production-wide `db-{id}` promote and P4 `v1-execution.enabled=false` (requires staging — see `docs/migration/staging-readiness-checklist.md`)
- Vaadin operator UI for migration inventory
- Wave freeze calendar dates (product owner, after staging R0)
```

Remove lines claiming "Full explain/preview control plane" and "Official non-SQL transformer" as deferred—they are on the branch (control plane REST, `SpelTransformFactory`).

- [x] **Step 3: Extend API table**

Add to the migration API table:

```markdown
| POST | `/template/v2/validate` |
| GET | `/template/v2/explain/{id}` |
| POST | `/template/v2/preview/{id}` |
```

Add config note:

```markdown
| Config | `data.generator.v1-execution.enabled` (default `true`; gates V1 task run when `false`) |
```

- [x] **Step 4: Update Test plan checkboxes**

Change staging items to M2 and check M1:

```markdown
- [x] Retirement M1: CI simulated promote + cohort sign-off (see deferred-ops spec)
- [ ] **M2** On staging: pick one production V1 JDBC export template → draft → compare → review classification
- [ ] **M2** Staging trial: `v1-execution.enabled=false` after cohort promote
```

- [x] **Step 5: Commit**

```bash
git add docs/migration/MR-feature-4.0.md
git commit -m "docs(mr): align feature-4.0 MR with deferred retirement M1/M2"
```

Footer per repo policy: `AI-Assisted-by: Cursor`, `Co-authored-by: <git user.name> <git user.email>`.

---

## Task 3: Staging runbook M2 banner

**Files:**
- Modify: `docs/migration/staging-runbook.md`

- [x] **Step 1: Add front matter after title**

After `# Staging migration runbook`, insert:

```markdown
> **Milestone:** **M2 only** — not required to merge `feature-4.0`.  
> **M1 substitute:** CI tests and `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md` evidence table.  
> **Prerequisite:** Complete `docs/migration/staging-readiness-checklist.md` before first staging sweep.
```

- [x] **Step 2: Commit**

```bash
git add docs/migration/staging-runbook.md
git commit -m "docs(migration): mark staging runbook as M2-only gate"
```

---

## Task 4: Staging readiness checklist (ops prep)

**Files:**
- Create: `docs/migration/staging-readiness-checklist.md`

- [x] **Step 1: Create checklist file**

```markdown
# Staging readiness checklist (M2 unlock)

Complete before first staging migration sweep. Does **not** block `feature-4.0` merge.

## Environment

| Item | Owner | Done |
|------|-------|------|
| Staging `data-generator-service` URL | | [ ] |
| Metadata DB reachable (same schema as prod templates) | | [ ] |
| `docs/migration/scenario-inventory.yaml` writable path configured | | [ ] |

## Data sources

| dataSourceId (staging) | Matches prod? | Smoke query OK? |
|------------------------|---------------|-----------------|
| | | [ ] |

## Catalog

| Metric | Value |
|--------|-------|
| Approximate V1 template count in DB | |
| Templates marked COMPATIBILITY_ONLY (expected) | |
| Target batch compare cap (default 50) | |

## Execution (M2 — after checklist complete)

1. `scripts/migration-staging.ps1 -Action refresh`
2. `scripts/migration-staging.ps1 -Action batch-compare`
3. Per-template promote only when classification ∈ {EXACT, ADAPTED, APPROXIMATE}
4. `GET /template/migration/signoff-status` → both families complete
5. Staging trial: `data.generator.v1-execution.enabled: false`

See `docs/migration/staging-runbook.md` and `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md`.
```

- [x] **Step 2: Link from retirement-readiness**

In `docs/migration/retirement-readiness.md`, under the M1/M2 table, add:

```markdown
Staging prep: `docs/migration/staging-readiness-checklist.md`
```

- [x] **Step 3: Commit**

```bash
git add docs/migration/staging-readiness-checklist.md docs/migration/retirement-readiness.md
git commit -m "docs(migration): add staging readiness checklist for M2"
```

---

## Task 5 (Optional P1): Promote requires business sign-off

Skip this task for minimal merge package. Implement only if team wants ops safety before staging.

**Files:**
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/template/migration/MigrationPromoteService.java`
- Modify: `data-generator-service/src/test/java/org/gensokyo/data/template/migration/MigrationPromoteServiceTests.java`

- [x] **Step 1: Write failing test**

Add to `MigrationPromoteServiceTests.java`:

```java
@Test
void promoteRejectsWhenInventoryRowExistsWithoutBusinessSignoff() throws Exception {
    TemplatePO entity = new TemplatePO();
    entity.setId(103L);
    entity.setName("unsigned-promote");
    entity.setContentYaml("""
            name: unsigned-promote
            iterator:
              type: number
              from: 1
              to: 2
            output:
              writers:
                - type: console
            """);
    repository.saveAndFlush(entity);

    MigrationInventoryEntry entry = new MigrationInventoryEntry();
    entry.setId("db-103");
    entry.setOrigin("database");
    entry.setDbTemplateId(103L);
    entry.setMigrationClass(MigrationClassification.EXACT);
    entry.setLastCompareReportPath("docs/migration/reports/sample.md");
    entry.setBusinessSignoffApproved(false);
    inventory.saveAll(List.of(entry));

    MigrationPromoteService service = newService(inventory);
    IllegalArgumentException ex = Assertions.assertThrows(
            IllegalArgumentException.class, () -> service.promote(103L));
    Assertions.assertTrue(ex.getMessage().toLowerCase().contains("sign-off")
            || ex.getMessage().toLowerCase().contains("signoff"));
}
```

- [x] **Step 2: Run test — expect FAIL**

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service `
  "-Dtest=MigrationPromoteServiceTests#promoteRejectsWhenInventoryRowExistsWithoutBusinessSignoff" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

- [x] **Step 3: Implement guard in `rejectIfInventoryBlocksPromote`**

After COMPATIBILITY_ONLY/BLOCKED check, add:

```java
if (!entry.isBusinessSignoffApproved()) {
    throw new IllegalArgumentException(String.format(
            "Template '%s' cannot be promoted: inventory %s lacks business sign-off",
            templateId, inventoryId));
}
```

- [x] **Step 4: Run full `MigrationPromoteServiceTests` — expect PASS**

- [x] **Step 5: Commit**

```bash
git commit -m "fix(migration): require business sign-off before promote when inventory row exists"
```

---

## Task 6: Final merge gate review

**Files:** `docs/migration/retirement-readiness.md`

- [x] **Step 1: Confirm M1 checkboxes honest**

Verify:

- P3 synthetic / multi_source sign-off: **checked** (CI cohort)
- P3 production promote: **unchecked** with M2 note
- P4 staging/prod flag: **unchecked**

- [x] **Step 2: Push branch and open/update MR**

```bash
git push origin feature-4.0
```

MR description should link:

- `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md`
- `docs/migration/MR-feature-4.0.md`

- [x] **Step 3: Commit any final doc nits**

```bash
git commit -m "docs(migration): complete M1 merge gate documentation"
```

---

## Spec coverage (self-review)

| Spec requirement | Task |
|------------------|------|
| Merge gate tests | Task 1 |
| MR capability vs deferred retirement | Task 2 |
| M2 runbook labeling | Task 3 |
| Staging prep checklist | Task 4 |
| Optional sign-off guard | Task 5 |
| Honest readiness checkboxes | Task 6 |
| No staging promote in merge window | Tasks 2, 4, 6 (explicit) |
| No P4 flag flip | Tasks 2, 6 |

## Verification summary

```powershell
# Minimum before merge approval
.\mvnw-jdk25.ps1 -pl data-generator-service -am `
  "-Dtest=StagingSimulatedPromoteWorkflowTests,MigrationWaveCohortSignoffTests" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

---

## Revision history

| Date | Change |
|------|--------|
| 2026-05-21 | Initial plan from approved deferred-ops spec |
| 2026-05-21 | Tasks 1–6 and optional Task 5 completed; MR reviewer checklist added |
