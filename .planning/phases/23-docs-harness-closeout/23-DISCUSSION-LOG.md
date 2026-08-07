# Phase 23: Docs + Harness Closeout - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-07  
**Phase:** 23-docs-harness-closeout  
**Mode:** --auto  
**Areas discussed:** Docs placement/content, Harness P1 matrix, P0 freeze / harness green bar

---

## Docs placement/content (DOC-01)

| Option | Description | Selected |
|--------|-------------|----------|
| Extend geo-synthetic + overview (+ optional geo-assets.md) | Mirror Phase 20 D-06; update stale v2.2 wording | ✓ |
| New standalone geo-assets book only | Risk duplicating geo_synthetic YAML | |
| Docs-only in ROADMAP / PROJECT | Too thin for operators | |

**User's choice:** [--auto] recommended — extend `docs/geo-synthetic-v2-source.md` + `docs/geospatial-overview.md`; optional `docs/geo-assets.md` if long  
**Notes:** Cover asset-id YAML, path vs asset-id wins, map preview honesty, max-bytes/max-features

---

## Harness P1 matrix (TEST-11)

| Option | Description | Selected |
|--------|-------------|----------|
| Add P1 `geo-assets` with Maven IT links | Non-blocking; P0 stays 15 | ✓ |
| Promote to P0 | Explicitly forbidden by ROADMAP | |
| Skip matrix; docs only | Fails TEST-11 / SC2–SC4 | |

**User's choice:** [--auto] recommended — P1 covered; link ConsoleGeoAsset* IT + GeoAssetServiceTests + TemplateV2RunnerGeoAssetSourceTests  
**Notes:** Playwright not required for matrix row

---

## P0 freeze / harness green bar

| Option | Description | Selected |
|--------|-------------|----------|
| Matrix + harness summary; optional thin UAT | Matches Phase 20 closeout | ✓ |
| Mandatory new merge-gate script | Would risk P0 inflation | |

**User's choice:** [--auto] recommended — no P0 change; optional UAT only if useful  

---

## Claude's Discretion

- Overview vs `docs/geo-assets.md` length heuristic  
- Exact matrix notes / optional UAT script  
- Plan wave ordering  

## Deferred Ideas

- `/gsd-complete-milestone` after Phase 23 verifies  
- P0 promotion of geo-assets / map E2E  
- GEO-06; Phase 22 LINE_SAMPLE preview DTO gap as product fix  
