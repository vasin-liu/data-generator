# SCRIPT → SpEL migration draft (operator notes)

Design: `docs/superpowers/specs/2026-05-21-script-spel-draft-migration-design.md`

## What shipped

- **Analyzer:** `language.type: SPEL` / `PLAIN` field `SCRIPT` → `recommendedPath: spel` (not `COMPATIBILITY_ONLY`).
- **Draft API** (`POST /template/migration/draft/{id}`): normalized V2 chain **`migrate-sql`** then **`migrate-spel`** when SCRIPT fields exist.
- **Runtime:** `SpelTransformFactory` registered in Spring (`CoreConfig`); `#dataset` → `#row`, `#faker` supported.

## Draft vs dual-run compare (important)

| Path | V2 draft for promote / API | V2 side used in **compare** |
|------|---------------------------|-----------------------------|
| **Iterator / synthetic** (no JDBC readers) | SQL + SpEL | SQL + Spel (same) |
| **Query-source / JDBC-shaped** | SQL + SpEL | **SQL only** (`buildDraftForCompare`) |

**Why:** V1 field `SCRIPT` often uses `#dataset` as the **current field input** (including `dependsOn`), while V2 SpEL runs on the **SQL `SELECT *` row**. Running SpEL during JDBC compare caused evaluation errors until SQL projection carries those columns.

**Operator impact:**

- **Promote / saved draft** may list SpEL columns for JDBC templates — expected for 2b inventory.
- **Compare reports** for JDBC/multi_source may still show APPROXIMATE until SQL row shape matches field SCRIPT inputs (future enhancement).
- **Iterator cohort** (`demo/28`, `v1-iterator-simple`) compare exercises SpEL in CI.

## Template families (retirement)

| Family | Examples | Draft SpEL | Compare SpEL |
|--------|----------|------------|--------------|
| **B — SpEL-migratable SCRIPT** | Most `tocc/parking/*`, `idps/*` field scripts | Yes | JDBC: no (see above) |
| **C — orchestration_legacy** | PAUSE, LOG, JS | No (`COMPATIBILITY_ONLY`) | No |

See `docs/migration/compatibility-only-templates.md`.

## Verification (local)

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service,data-generator-calcite -am `
  "-Dtest=V1ScriptToSpel*,MigrationDraftServiceSpelTests,BuiltinClasspathTemplateRegressionTests#spelPathBuiltinTemplatesIncludeSpelTransformInDraft" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Full reactor (2026-05-22): `.\mvnw-jdk25.ps1 test` — BUILD SUCCESS, 43 modules.
