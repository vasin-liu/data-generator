# Phase 21 Discussion Log

**Date:** 2026-07-31  
**Mode:** `--auto` (single pass)

## Gray areas (all selected)

1. Asset identity & template binding
2. Persistence & upload limits
3. Delete & references
4. Runtime resolution
5. Governance

## Auto-selected decisions

| Area | Choice |
|------|--------|
| Binding | Dedicated `*AssetId` fields + `asset:{uuid}` wire format; asset-id wins over path if both set |
| Storage | CLOB in metadata DB; no filesystem spill |
| Upload API | New `ConsoleGeoAssetController` — do not reuse ephemeral upload controller |
| Limits | Configurable max-bytes + max-features; raise multipart defaults ~16MB |
| Delete | Hard delete; 409 if templates reference asset |
| Missing asset | Fail run (`IllegalArgumentException`) |
| Audit/RBAC | Audit upload/delete; existing RBAC flag only |

## Outcome

Wrote `21-CONTEXT.md`. Proceeding to `/gsd-plan-phase 21 --auto`.
