# 05-02 Summary — API slice expansion + P0 regression gate

**Status:** Complete  
**Requirements:** COV-03, COV-04

## Delivered

- Expanded `ConsoleUdfControllerTest` (history, deprecate lifecycle, unknown-id 400).
- Expanded `ConsoleTransformCatalogControllerTest` (kind=UDF filter, builtin metadata completeness).
- Added matrix rows `console-api-udf` and `console-api-transforms` (tier P1, linked slice tests).
- Added P0 regression gate to `scripts/verify-harness.ps1` (non-zero exit when `p0.pass` is false).
- Renamed CI step to **Harness verification + P0 regression gate**; summary upload uses `if: always()`.
- Documented merge criteria in `AGENTS.md`.

## Verification

- `ConsoleUdfControllerTest` + `ConsoleTransformCatalogControllerTest` green.
- Harness enforces P0 gate via `p0.pass`.
