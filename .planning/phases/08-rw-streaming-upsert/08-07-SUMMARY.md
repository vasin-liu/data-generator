---
phase: 08-rw-streaming-upsert
plan: 07
subsystem: api
tags: [validation, upsert, console, publish, warnings]

requires: [08-05]
provides:
  - TemplateV2Validator validateJdbcUpsertOptions fail-fast at publish
  - collectWarnings large-file IN_MEMORY guidance (warn-only)
  - Opaque transform upsertKeys run-time validation warning
  - Console execution policy hints and JDBC upsert options guidance
  - Publish flow surfaces validation.warnings toast
affects: [08-11, 08-12]

tech-stack:
  added: []
  patterns:
    - "upsert=true requires non-empty upsertKeys at publish (D-14)"
    - "Simple SQL SELECT column cross-check at publish; JS/SpEL opaque fallback warning (W-04)"
    - "IN_MEMORY + 10MB/100k rows warn-only via collectWarnings (D-05, D-22)"

key-files:
  created:
    - data-generator-service/src/test/java/org/gensokyo/data/template/TemplateV2ValidatorTests.java
  modified:
    - data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java
    - data-generator-console-web/src/app/editor/ReviewPanel.tsx
    - data-generator-console-web/src/app/editor/steps/ExecutionStep.tsx
    - data-generator-console-web/src/app/editor/steps/SinksStep.tsx
    - data-generator-console-web/src/i18n/locales/en.json
    - data-generator-console-web/src/i18n/locales/zh-CN.json

key-decisions:
  - "Use maxRows >= 100_000 on CsvSourceVO/JsonSourceVO as row-count bar (no estimatedRows VO field)"
  - "Classpath and filesystem path resolution for file-size warnings"
  - "Publish mutation validates draft client-side before publish API to surface warnings toast"

patterns-established:
  - "validateJdbcUpsertOptions + appendLargeFileInMemoryWarnings + appendOpaqueUpsertKeyWarnings in TemplateV2Validator"

requirements-completed: [RW-03, RW-04]

duration: 45min
completed: 2026-06-29
---

# Phase 08 Plan 07 Summary

**Publish-time upsert validation, large-file IN_MEMORY warnings, and console execution-policy / upsert form hints**

## Performance

- **Duration:** 45 min
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- `validateJdbcUpsertOptions` throws when `upsert=true` with empty/malformed `upsertKeys` (sink path in message)
- Simple `SqlTransformVO` publish-time cross-check of `upsertKeys` against inferrable SELECT output columns
- Opaque JS/SpEL/complex SQL: skip column cross-check; `collectWarnings` emits run-time validation notice
- `appendLargeFileInMemoryWarnings` for IN_MEMORY + CSV/JSON ≥10 MB or `maxRows` ≥100k (warn-only, recommends CHUNKED/STREAMING)
- `TemplateV2ValidatorTests` (6 cases): upsert missing keys, SQL key mismatch, opaque warning, large file, row count, scenario-c baseline
- Console: execution mode `Form.Item` extra hints per mode; JDBC sink upsert options info alert
- `ReviewPanel` publish calls `validateDraft` first and shows `message.warning` for validation warnings

## Task Commits

1. **Validator upsert + large-file warnings, console hints, tests** - feat commit

## Files Created/Modified

- `TemplateV2Validator.java` - upsert publish validation, large-file warnings, SQL column extractor
- `TemplateV2ValidatorTests.java` - Phase 8 validator regression tests
- `ExecutionStep.tsx` - mode-specific hint extra text
- `SinksStep.tsx` - JDBC upsert options guidance alert
- `ReviewPanel.tsx` - publish warning toast wiring
- `en.json` / `zh-CN.json` - execution mode hints and JDBC upsert copy

## Decisions Made

- Reused `WriterOptionResolver.upsertKeysOption()` from calcite for key resolution (legacy `conflictColumns` supported)
- `scenario-c-csv-export` small fixture produces zero Phase-8 warnings under IN_MEMORY
- `TemplateV2ControlPlaneService` unchanged — validate API already merges `collectWarnings`

## Deviations from Plan

- No dedicated upsert toggle in console (writers use JSON options); hint shown as info Alert in JDBC options section instead
- `estimatedRows` VO field does not exist; used `maxRows` on CsvSourceVO/JsonSourceVO for row-count bar

## Issues Encountered

- Maven `-Dtest=TemplateV2ValidatorTests` requires `-Dsurefire.failIfNoSpecifiedTests=false` with `-am` reactor slice

## Self-Check: PASSED

- `TemplateV2ValidatorTests`: 6 tests, 0 failures
- `npm run verify:unit` (console tsc): exit 0
- `upsert=true` + empty keys throws with `sink[0].writer[0]` path
- Simple SQL missing upsertKey throws at publish
- JS transform + upsert passes validate with opaque run-time warning
- Large CSV (10 MB) IN_MEMORY warning contains CHUNKED or STREAMING
- `scenario-c-csv-export` normalized template: validate OK, zero collectWarnings

## Next Phase Readiness

- Plan 08-11 can rely on publish validation for upsertKeys before run-report UI work
- Plan 08-12 Playwright can assert IN_MEMORY large-file warn toast on publish (D-23 scenario 6)

---
*Phase: 08-rw-streaming-upsert*
*Completed: 2026-06-29*
