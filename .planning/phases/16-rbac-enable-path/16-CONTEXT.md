# Phase 16: RBAC Enable Path - Context

**Gathered:** 2026-07-29
**Status:** Ready for planning
**Mode:** --auto (recommended defaults selected in one pass)

<domain>
## Phase Boundary

Deliver a **documented, testable header-RBAC enable path** for operators (SEC-01): staging/e2e profiles turn on `data.generator.console-security.*`, IT and/or E2E prove deny/allow behavior when enabled, and **base/local defaults stay off**.

This phase closes the gap between shipped RBAC code (`ConsoleAuthorizationFilter`, profiles, existing ITs/specs) and operator-facing enable/verify documentation. It does **not** flip default-on RBAC, add IdP/OAuth2/JWT, merge distributed+RBAC proof, or wire P1 harness rows (Phase 17 / TEST-09).

</domain>

<decisions>
## Implementation Decisions

### Documentation packaging
- **D-01:** Deliver a **single focused operator doc** under `docs/` (recommended name: `docs/staging-console-rbac.md`) covering: property keys, required headers (`X-Console-Role`, optional `X-Console-Actor`), role→permission summary, profile names, and verify one-liners. Add **one AGENTS.md Commands/docs pointer** (Phase 13–15 pattern: comment + path).
- **D-02:** **Cross-link** from existing `docs/operator-console-usage.md` Configuration reference (already mentions staging + verify-console) to the new doc — do not duplicate the full enable recipe in two places. Staging overlay remains `application-staging.yaml` (`spring.profiles.active=staging`, port 8080).

### Maven IT proof (primary backend evidence)
- **D-03:** Keep **`ConsoleAuthorizationIntegrationIT`** as the canonical HTTP deny/allow proof when `console-security.enabled=true` (missing header → 403, VIEWER cannot POST templates, EDITOR cannot publish, governance interaction on draft run). Extend only if gaps are found during research — do not replace with filter-only unit tests as sole evidence.
- **D-04:** Add an explicit **default-off regression test** (recommended: small `@SpringBootTest` or properties-binding test) asserting `ConsoleSecurityProperties.enabled` is **false** when loading base/`application-phase7-test.yaml` without RBAC overrides, and that base `application.yaml` does **not** set `console-security.enabled: true`. This guards Pitfall 4 (accidental default-on / profile bleed).
- **D-05:** Keep existing unit tests (`ConsoleAuthorizationFilterTest`, `ConsoleUdfAuthorizationFilterTest`) green; no new auth framework.

### Playwright E2E proof (secondary, opt-in)
- **D-06:** **Reuse** existing gated specs `data-generator-console-web/e2e/specs/rbac.console.spec.ts` and `rbac.ui.spec.ts` (active when `DG_E2E_RBAC=true`) with Podman profile `e2e-rbac` (`application-e2e-rbac.yaml`) — already wired in `scripts/e2e-podman.ps1`. Phase 16 **documents** this path; new Playwright specs only if audit finds uncovered deny/allow scenarios.
- **D-07:** Document the distinction between **`DG_E2E_RBAC=true`** (dedicated RBAC Podman profile) and **`DG_E2E_GOVERNANCE_STAGING=true`** (staging governance headers via `e2e/helpers/api.ts` `defaultConsoleRole()`). Default `verify-console.ps1` / `e2e` profile stays RBAC-off (`application-e2e.yaml` explicitly `enabled: false`).

### Runnable verify script
- **D-08:** Add **`scripts/verify-rbac-enable.ps1`** — Maven slice runs RBAC IT + filter unit tests + optional `-SkipPlaywright` flag (mirrors Phase 13/15 verify scripts). Script is the **operator one-liner** referenced from the new doc and AGENTS.md; **not** a P0 merge gate.

### Profile isolation & non-goals
- **D-09:** **Profile contract (must remain true after Phase 16):**
  - Base / dev: no `console-security.enabled` in `application.yaml` → Java default `false`
  - `application-e2e.yaml`, `application-e2e-distributed.yaml`, `application-distributed-staging.yaml`: **`enabled: false`**
  - `application-staging.yaml`, `application-e2e-rbac.yaml`: **`enabled: true`** (opt-in overlays only)
- **D-10:** **Out of scope for SEC-01 done criteria:** P1 matrix row / `test-matrix.yaml` wiring (Phase 17), multi-JVM + RBAC combined runs, Spring Security/OAuth2/JWT, default-on production RBAC (SEC-02), changing `ConsoleAuthorizationFilter` permission matrix unless a test gap forces a bugfix, Podman-only proof as sole gate.

### Claude's Discretion
- Exact doc filename if `staging-console-rbac.md` collides or a better home exists
- Whether default-off regression lives in a new `ConsoleSecurityDefaultOffIT` vs extending an existing security test class
- Exact verify script flags (`-SkipPlaywright`, `-IncludeWebBuild`) aligned with sibling verify scripts
- Minor doc tables for role→permission mapping sourced from `ConsoleRole` / `ConsolePermission`
- Whether to add a positive allow-path assertion in IT (e.g., VIEWER GET scenarios → 200) if not already covered

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & roadmap
- `.planning/REQUIREMENTS.md` — SEC-01 (active), SEC-02 (deferred), TEST-09 (Phase 17)
- `.planning/ROADMAP.md` — Phase 16 goal and success criteria
- `.planning/PROJECT.md` — RBAC default-off; testable enable path
- `.planning/STATE.md` — Phase 16 entry
- `.planning/research/SUMMARY.md` — RBAC enable path rationale; default-off guard
- `.planning/research/STACK.md` — profiles, Playwright helpers, IT patterns
- `.planning/research/ARCHITECTURE.md` — `ConsoleAuthorizationFilter` flow; anti-pattern default-on
- `.planning/research/PITFALLS.md` — Pitfall 4 accidental RBAC default-on; enable-path IT/profile only
- `.planning/research/FEATURES.md` — RBAC testable enable; do not flip default

### Prior phase decisions
- `.planning/phases/12-http-execute-path-proof/12-CONTEXT.md` — RBAC filter not required for EXEC-01; deferred here
- `.planning/phases/15-multi-jvm-worker-e2e/15-CONTEXT.md` — RBAC-on distributed runs deferred to Phase 16 (combined proof still out of SEC-01 scope per D-10)

### RBAC runtime (source of truth)
- `data-generator-service/src/main/java/org/gensokyo/data/config/ConsoleSecurityProperties.java` — `enabled=false` Java default; header names
- `data-generator-service/src/main/java/org/gensokyo/data/security/ConsoleAuthorizationFilter.java` — `/api/**` enforcement when enabled
- `data-generator-service/src/main/java/org/gensokyo/data/security/ConsoleRole.java` — role enum + header parsing
- `data-generator-service/src/main/java/org/gensokyo/data/security/ConsolePermission.java` — permission enum
- `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleRuntimeController.java` — runtime RBAC status surfacing

### Config profiles
- `data-generator-service/src/main/resources/application.yaml` — base (no console-security block → default off)
- `data-generator-service/src/main/resources/application-staging.yaml` — staging RBAC-on overlay
- `data-generator-service/src/main/resources/application-e2e.yaml` — default e2e RBAC-off
- `data-generator-service/src/main/resources/application-e2e-rbac.yaml` — RBAC-on e2e profile
- `data-generator-service/src/main/resources/application-e2e-distributed.yaml` — distributed e2e RBAC-off
- `data-generator-service/src/main/resources/application-distributed-staging.yaml` — multi-JVM staging RBAC-off

### Tests & E2E
- `data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleAuthorizationIntegrationIT.java` — canonical RBAC-on IT
- `data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleAuthorizationFilterTest.java` — filter unit tests
- `data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleUdfAuthorizationFilterTest.java` — UDF path classification
- `data-generator-console-web/e2e/specs/rbac.console.spec.ts` — API RBAC E2E (gated)
- `data-generator-console-web/e2e/specs/rbac.ui.spec.ts` — UI RBAC E2E (gated)
- `data-generator-console-web/e2e/helpers/api.ts` — `consoleRoleHeaders`, `DG_E2E_GOVERNANCE_STAGING`

### Scripts & existing docs
- `scripts/e2e-podman.ps1` — `e2e-rbac` profile + `DG_E2E_RBAC=true` Playwright block
- `scripts/verify-console.ps1` — full console pipeline (includes RBAC Podman leg)
- `docs/operator-console-usage.md` — configuration reference (cross-link target)
- `AGENTS.md` — verify-script catalog + docs pointer home

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **RBAC stack is shipped and profile-gated:** `ConsoleSecurityProperties` defaults off; `ConsoleAuthorizationFilter.shouldNotFilter` skips when disabled
- **`ConsoleAuthorizationIntegrationIT`** already proves 403 deny paths + governance interaction under `enabled=true`
- **Playwright RBAC specs exist** but run only when `DG_E2E_RBAC=true`; Podman restart with `DG_SPRING_PROFILES_ACTIVE=e2e-rbac` is in `e2e-podman.ps1`
- **`application-e2e-rbac.yaml`** and **`application-staging.yaml`** already enable RBAC — Phase 16 is docs + default-off guard + verify script, not greenfield auth
- **`docs/operator-console-usage.md`** has a configuration block and mentions staging/verify-console — needs focused enable runbook

### Established Patterns
- Default-off for local/dev; enable only via named Spring profiles (research Pitfall 4)
- Verify scripts: `scripts/verify-*.ps1` with Maven slice + optional Playwright skip (Phases 13–15)
- Docs packaging: single maintainer/operator doc + AGENTS.md pointer (Phases 13–14)
- P1 harness rows deferred to Phase 17 when proof lands (Phase 15 D-03 pattern — but RBAC row explicitly Phase 17 per TEST-09)

### Integration Points
- Filter applies to `/api/**` only; legacy `/task/**` and `/healthz` unaffected
- Role header → `ConsoleRole.fromHeader` → `ConsolePermission` matrix in filter
- E2E helpers inject headers when `DG_E2E_GOVERNANCE_STAGING=true` or explicit role passed
- Console UI role picker behavior tied to RBAC enabled state via runtime API

</code_context>

<specifics>
## Specific Ideas

- Research consensus: highest risk is flipping default-on or breaking default e2e — D-04/D-09 directly address this
- `application-distributed-staging.yaml` keeps RBAC off so Phase 15 multi-JVM script stays header-free; do not enable RBAC there for SEC-01
- Positive allow-path IT (VIEWER GET catalog → 200) strengthens proof if quick to add

</specifics>

<deferred>
## Deferred Ideas

- **P1 matrix row for RBAC enable** — Phase 17 / TEST-09 (link to `verify-rbac-enable.ps1` + existing IT/E2E)
- **Multi-JVM worker + RBAC combined proof** — optional future hardening; not SEC-01 done gate
- **Default-on production RBAC (SEC-02)** — product/security decision beyond v2.1
- **Spring Security / OAuth2 / JWT console auth** — out of milestone scope
- **Expanding permission matrix or new roles** — only if test audit finds bugs

</deferred>

---
*Phase: 16-rbac-enable-path*
*Discussed: 2026-07-29 (--auto)*
