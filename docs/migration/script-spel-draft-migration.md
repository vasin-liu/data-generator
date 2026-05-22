# SCRIPT → SpEL migration draft (operator notes)

Design: `docs/superpowers/specs/2026-05-21-script-spel-draft-migration-design.md`

## What shipped

- **Analyzer:** `language.type: SPEL` / `PLAIN` field `SCRIPT` → `recommendedPath: spel` (not `COMPATIBILITY_ONLY`).
- **Draft API** (`POST /template/migration/draft/{id}`): normalized V2 chain **`migrate-sql`** then **`migrate-spel`** when SCRIPT fields exist.
- **Runtime:** `SpelTransformFactory` registered in Spring (`CoreConfig`); `#dataset` → `#row`, `#faker` supported.

## Draft vs dual-run compare

| Path | V2 draft for promote / API | V2 side used in **compare** |
|------|---------------------------|-----------------------------|
| **Iterator / synthetic** (no JDBC readers) | SQL + SpEL | SQL + SpEL (same) |
| **Query-source / JDBC-shaped** | SQL + SpEL | SQL + SpEL (same) |

`buildDraftForCompare()` uses the same transform chain as `buildDraft()`. Bare V1 `#dataset` on `dependsOn` fields is rewritten to `#row['<dep>']` in the draft converter; JDBC compare still needs SQL (or iterator SQL) to expose source columns used by `#row['…']` expressions.

**Operator impact:**

- **Promote / compare / saved draft** all list SpEL columns for JDBC templates when SCRIPT fields exist — expected for 2b inventory.
- **Compare reports** may still be **APPROXIMATE** for faker/non-deterministic or READ-only V1 fields not carried into SpEL.
- **CI:** `BuiltinClasspathTemplateMigrationWorkflowTests` (`parking/11`, `demo/28`) runs JDBC-shaped compare with embedded H2 and full column projection.

## Template families (retirement)

| Family | Examples | Draft SpEL | Compare SpEL |
|--------|----------|------------|--------------|
| **B — SpEL-migratable SCRIPT** | Most `tocc/parking/*`, `idps/*` field scripts | Yes | Yes (SQL row must expose inputs) |
| **C — orchestration_legacy** | PAUSE, LOG, JS | No (`COMPATIBILITY_ONLY`) | No |

See `docs/migration/compatibility-only-templates.md`.

## Verification (local)

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service,data-generator-calcite -am `
  "-Dtest=V1ScriptToSpel*,MigrationDraftServiceSpelTests,BuiltinClasspathTemplateRegressionTests#spelPathBuiltinTemplatesIncludeSpelTransformInDraft" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Full reactor (2026-05-22, post JDBC compare SpEL): `.\mvnw-jdk25.ps1 test` — BUILD SUCCESS, 43 modules (~5m 11s).
