---
phase: 03-udf-console-template-binding
plan: 03
subsystem: frontend
tags: [react, antd, react-query, i18n, console, udf, typescript]

requires:
  - phase: 03-udf-console-template-binding
    provides: "/api/console/udfs REST surface, UdfVersionView/UdfGroupView wire contract"
provides:
  - "Operator console UDFs page at /udfs (peer to Templates/Jobs)"
  - "Typed UDF API client (fetchUdfs, uploadUdf, publishUdf, deprecateUdf)"
  - "Grouped-by-udfId list with expandable version history + lifecycle tags + type-driven upload"
  - "en/zh-CN i18n parity for all udfs.* / nav.udfs keys"
affects: [03-05]

tech-stack:
  added: []
  patterns:
    - "Type-driven upload modal: Form.useWatch('type') switches file vs script vs sql+sqlName inputs"
    - "FormData assembled client-side; apiFormRequest lets the browser set the multipart boundary"
    - "Nested expandable AntD Table renders versions[] under each udfId row"

key-files:
  created:
    - data-generator-console-web/src/api/udfs.ts
    - data-generator-console-web/src/app/pages/UdfsPage.tsx
  modified:
    - data-generator-console-web/src/api/types.ts
    - data-generator-console-web/src/app/App.tsx
    - data-generator-console-web/src/app/layout/ConsoleLayout.tsx
    - data-generator-console-web/src/i18n/locales/en.json
    - data-generator-console-web/src/i18n/locales/zh-CN.json

key-decisions:
  - "Java-plugin file captured via AntD Upload.Dragger with beforeUpload=false (no auto-upload); the File is appended to FormData on submit"
  - "Publish gated to state==='draft' and Deprecate to state==='published' via disabled buttons, mirroring the server FSM"
  - "Reused existing common.refresh / common.cancel keys; added a self-contained udfs.* block with full en/zh-CN parity"

patterns-established:
  - "Pattern 1: SPA never fetches/render payload bytes — consumes payload-free UdfGroupView/UdfVersionView only (D-14)"
  - "Pattern 2: nav selectedKey + route + menu item added as a peer triple for a new top-level console page"

requirements-completed: [UDF-05]

duration: ~15min
completed: 2026-06-18
---

# Phase 3 / Plan 03: Console UDFs React Page Summary

**A top-level `/udfs` console page — grouped-by-udfId list with expandable version history, lifecycle tags, inline publish/deprecate, and a type-driven multipart upload modal — fully bilingual (en/zh-CN).**

## Performance

- **Duration:** ~15 min (the tsc+vite production build dominated wall-clock at ~3m45s)
- **Tasks:** TS types + API client, UdfsPage, route/nav/i18n wiring
- **Files modified:** 7 (2 created, 5 modified)

## Accomplishments
- `src/api/udfs.ts` provides `fetchUdfs` / `publishUdf` / `deprecateUdf` (JSON via `apiRequest`, URL-encoded ids) and `uploadUdf` (multipart via `apiFormRequest`); `types.ts` gains `UdfVersionView` / `UdfGroupView` mirroring the 03-02 DTOs (no payload).
- `UdfsPage` lists groups by `udfId` with an expandable nested table of versions, renders `state` as draft/published/deprecated `Tag`s, and offers inline Publish (draft-only) / Deprecate (published-only) actions plus a type-driven upload `Modal` (java=JAR drag/drop, script=code TextArea, sql=SQL TextArea + `sqlName`). Root carries `data-testid="udfs-page"`.
- `App.tsx` routes `path="udfs"`; `ConsoleLayout.tsx` adds the `nav-udfs` menu item (FunctionOutlined) and `/udfs` to `selectedKey`.
- `en.json` + `zh-CN.json` gain `nav.udfs` and a full `udfs.*` block with identical key sets.

## Files Created/Modified
- `udfs.ts` - typed UDF client.
- `UdfsPage.tsx` - grouped list + version history + type-driven upload.
- `types.ts` - `UdfVersionView`/`UdfGroupView`.
- `App.tsx` - `/udfs` route.
- `ConsoleLayout.tsx` - nav item + selectedKey.
- `en.json`, `zh-CN.json` - bilingual strings (key parity).

## Decisions Made
- See key-decisions in frontmatter. The file is captured via `Upload.Dragger` with `beforeUpload=false` and appended to `FormData` at submit so the browser controls the multipart boundary (`apiFormRequest` omits Content-Type).

## Deviations from Plan
None — implemented as specified.

## Issues Encountered
- None functional. The production bundle emits the pre-existing ">500 kB chunk" advisory (not introduced here); `tsc --noEmit` clean and `vite build` exited 0.

## Next Phase Readiness
- The console surface is ready; sample UDFs + the embedded E2E harness (03-05) can now exercise upload→publish→list end-to-end.

---
*Phase: 03-udf-console-template-binding*
*Completed: 2026-06-18*
