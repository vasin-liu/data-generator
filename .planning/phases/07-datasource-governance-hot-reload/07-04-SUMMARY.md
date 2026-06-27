---
phase: 07-datasource-governance-hot-reload
plan: 04
subsystem: ui
tags: [console, datasource, audit, governance, react, i18n]

requires:
  - phase: 07-datasource-governance-hot-reload
    provides: ConnectionCatalog health fields, unified test API, audit category filter (07-01..07-03)
provides:
  - Datasources page HEALTHY/DEGRADED badges, catalog detail drawer, connectivity test gate UX
  - Unified JDBC/Kafka/ES test-before-save flow with driver preset round-trip
  - Audit page category/resourceId deep-link from Datasources
  - Template editor run/publish resilience after datasource CRUD in same session
affects: [07-05-playwright-e2e]

tech-stack:
  added: []
  patterns:
    - "Catalog health badges joined by kind:name key across JDBC/Kafka/ES tables"
    - "Connectivity test gate disables Save when governance.requireConnectivityTestBeforeSave"
    - "Audit deep-link via /console/audit?category=DATASOURCE&resourceId={name}"

key-files:
  created: []
  modified:
    - data-generator-console-web/src/app/pages/DatasourcesPage.tsx
    - data-generator-console-web/src/app/pages/AuditPage.tsx
    - data-generator-console-web/src/app/pages/TemplateEditorPage.tsx
    - data-generator-console-web/src/app/editor/ReviewPanel.tsx
    - data-generator-console-web/src/app/datasources/DriverPresetFields.tsx
    - data-generator-console-web/src/api/datasources.ts
    - data-generator-console-web/src/api/audit.ts
    - data-generator-console-web/src/api/types.ts
    - data-generator-console-web/src/i18n/locales/en.json
    - data-generator-console-web/src/i18n/locales/zh-CN.json
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleDataSourceController.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/CatalogConnectionSummaryDto.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/DataSourcesOverviewDto.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/DatasourceGovernanceFlagsDto.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/ConnectionTestRequestDto.java

key-decisions:
  - "Template run fix uses route template id fallback and keeps editor visible during background refetch"
  - "Draft save surfaces governance warnings via post-save validateDraft (non-blocking toast)"

patterns-established:
  - "TestResultAlert + requireTestBeforeSave gate shared across JDBC/Kafka/ES modals"
  - "Catalog detail drawer shows degradedReason and last-known-good operator note (D-26)"

requirements-completed: [DS-03, DS-04, DS-05]

duration: 35min
completed: 2026-06-27
---

# Phase 07 Plan 04 Summary

**Console datasource health badges, unified connectivity test gate, audit deep-links, and template run regression fixes per 07-UI-SPEC**

## Performance

- **Duration:** ~35 min
- **Tasks:** 2
- **Files modified:** ~18 (console + REST DTO extensions)

## Accomplishments

- Datasources list/catalog shows HEALTHY/DEGRADED badges with last reload timestamp; detail drawer shows degraded reason and last-known-good note
- JDBC/Kafka/ES forms use unified `POST /api/datasources/connections/test`; Save blocked when governance requires passing test; inline success/error feedback
- Driver preset id persisted on save and rehydrated on edit (D-21)
- Audit page reads `category` and `resourceId` query params; Datasources links navigate to filtered audit view (D-25)
- Template Review run/publish use route id fallback; editor stays interactive during refetch after datasource CRUD; draft save shows governance warnings (D-16/D-21)

## Verification

```text
cd data-generator-console-web && npm run build
```

Result: **PASS** (tsc + vite build)

## Deviations from Plan

### Auto-fixed Issues

**1. [Plan file name] TemplateDetailPage → TemplateEditorPage + ReviewPanel**
- **Found during:** Task 2 implementation
- **Issue:** Plan referenced non-existent `TemplateDetailPage.tsx`; run/publish logic lives in `ReviewPanel` under `TemplateEditorPage`
- **Fix:** Applied run-button and publish id fallback in `ReviewPanel.tsx`; refetch/loading tweak in `TemplateEditorPage.tsx`
- **Files modified:** `ReviewPanel.tsx`, `TemplateEditorPage.tsx`

**2. [Build] Incomplete DatasourcesPage wiring**
- **Found during:** Task 1 verification
- **Issue:** Partial edit left `testFormMutation` reference and unused state (TypeScript errors)
- **Fix:** Completed test feedback UI, catalog drawer, Kafka/ES test buttons, and save gate
- **Files modified:** `DatasourcesPage.tsx`

---

**Total deviations:** 2 auto-fixed (path naming, incomplete partial implementation)
**Impact on plan:** No scope change; same acceptance criteria met.

## Issues Encountered

None blocking after completing partial DatasourcesPage implementation.

## Next Phase Readiness

- Console DS-03/DS-04/DS-05 UX ready for Playwright regression in 07-05 (`datasource-governance.spec.ts`)
- E2E can assert DEGRADED badge, test gate, audit deep-link, and post-CRUD template run

---
*Phase: 07-datasource-governance-hot-reload*
*Completed: 2026-06-27*
