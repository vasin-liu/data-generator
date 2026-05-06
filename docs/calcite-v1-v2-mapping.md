# V1 to V2 Capability Mapping

## Goal

Map the current V1 template capabilities to the planned Calcite-based V2 model so migration work can be prioritized by direct replacement value instead of by module ownership alone.

## Mapping Rules

- `Direct`: V1 capability can move into V2 almost as-is through SQL or a thin adapter
- `Adapted`: V1 capability can move into V2, but only after introducing a V2 abstraction such as `RowSource`, `RowSink`, or UDFs
- `Partial`: only a subset should move into V2; compatibility support remains necessary
- `Keep`: capability should remain outside the V2 SQL transform path

## Template-Level Mapping

| V1 Area | V1 Shape | V2 Target | Mapping Type | Notes |
|---|---|---|---|---|
| Template root | `iterator + generator + fields + output` | `sources + transform + sink` | Adapted | generator can be retained initially as an execution option |
| Iterator root | `template.iterator` | `sources.input` or named sources | Adapted | iterator becomes a logical table source |
| Field graph | `fields[].dependsOn` | SQL projection dependency | Direct | Calcite naturally resolves projection dependencies |
| Field stage chain | `fields[].stages` | `transform.sql` | Partial | transformation stages map well; orchestration stages do not |
| Output root | `output.writers` | `sink.writers` | Adapted | sink should reuse current writers through adapters |

## Iterator Mapping

| V1 Capability | Current Type | V2 Target | Mapping Type | Suggested First-Phase Handling |
|---|---|---|---|---|
| Number iterator | `NUMBER` | `IteratorRowSource` | Direct | expose as `input` table with explicit schema |
| Constant iterator | `CONSTANT` | `IteratorRowSource` | Direct | expose values as one-column or explicit-schema rows |
| Datetime iterator | `DATETIME` | `IteratorRowSource` | Direct | expose generated timestamps as rows |
| Database iterator | `DATABASE` | `QuerySourceVO` | Adapted | final direction is to converge into the query-backed source family only |
| CSV iterator | `CSV` | `RowSource` | Adapted | prefer source abstraction over iterator semantics in V2 |
| Excel iterator | `EXCEL` | `RowSource` | Adapted | prefer source abstraction over iterator semantics in V2 |
| JSON iterator | `JSON` | `RowSource` | Adapted | prefer source abstraction over iterator semantics in V2 |
| Iterator choose/otherwise | iterator-side branch logic | compatibility path | Partial | not a first-phase SQL concern |
| Iterator pause | iterator throttling | orchestration layer | Keep | do not move into SQL |

## Stage Mapping

### Strong migration candidates

| V1 Stage | V2 Target | Mapping Type | Suggested V2 Form |
|---|---|---|---|
| `SCRIPT` | SQL expressions + UDFs | Partial | `SELECT expr AS alias` |
| `MAPPING` | SQL conditional mapping | Direct | `CASE WHEN ... THEN ... ELSE ... END` |
| `CONDITION` | row-local SQL condition | Direct | `CASE WHEN` or `WHERE` |
| `CONVERT` | cast/format UDF | Direct | `CAST`, format functions, custom UDFs |

### Source- or sink-facing migration candidates

| V1 Stage | V2 Target | Mapping Type | Suggested V2 Form |
|---|---|---|---|
| `READ` | `source` | Adapted | named source declaration |
| `SELECT` | source sampling/selection policy | Partial | source adapter policy, not transform SQL |
| `WRITE` | `sink` | Adapted | named sink declaration |

### Stages that should remain outside SQL transform

| V1 Stage | V2 Target | Mapping Type | Reason |
|---|---|---|---|
| `LOG` | runtime diagnostics | Keep | not data transformation |
| `PAUSE` | runtime throttling | Keep | not data transformation |
| `SHARED` | runtime/shared-state mechanism | Keep | requires orchestration semantics |

## Reader Mapping

| V1 Reader | V2 Target | Mapping Type | Notes |
|---|---|---|---|
| Constant reader | inline or source table | Direct | can become a logical source or SQL literal substitute |
| JDBC reader | `QuerySourceVO` | Adapted | final direction is to converge with `DatabaseIterator` into one query-backed source family; multiple JDBC readers under one V1 field now migrate into distinct QuerySource entries instead of overwriting |
| CSV reader | named source | Adapted | natural V2 source |
| Excel reader | named source | Adapted | natural V2 source |
| JSON reader | named source | Adapted | natural V2 source |
| SpEL reader | source or UDF-based replacement | Partial | simple expressions can move; complex expression logic should remain compatible |
| AI reader | official `AiSourceVO` | Adapted | prefer a first-class V2 source, potentially aligned with Spring AI |

## Script Mapping

| V1 Script Type | V2 Target | Mapping Type | Notes |
|---|---|---|---|
| Plain | SQL literal/expression | Direct | often removable entirely |
| SpEL | SQL + UDF | Partial | strong migration target for expression subset |
| JavaScript | compatibility-only or targeted UDF extraction | Partial | not a strong direct SQL target |

## Writer Mapping

| V1 Writer | V2 Target | Mapping Type | Notes |
|---|---|---|---|
| Console | sink adapter | Direct | first sink to land |
| JDBC/MySQL/Postgres/ClickHouse | sink adapter | Adapted | preserve writer internals |
| Kafka | sink adapter | Adapted | preserve runtime registry and serialization logic |
| Elasticsearch | sink adapter | Adapted | preserve runtime registry and bulk write logic |
| CSV/Excel/JSON | sink adapter | Adapted | lower first-phase priority than console/db |

## Common V1 Patterns and V2 Rewrites

### Pattern 1: dependent field + script

V1:

```yaml
fields:
  - name: FULL_NAME
    dependsOn:
      - FIRST_NAME
      - LAST_NAME
    stages:
      - type: SCRIPT
        language:
          type: SPEL
          content: "#dataset[0] + ' ' + #dataset[1]"
```

V2:

```yaml
transform:
  type: sql
  sql: |
    SELECT
      FIRST_NAME,
      LAST_NAME,
      concat(FIRST_NAME, ' ', LAST_NAME) AS FULL_NAME
    FROM input
```

### Pattern 2: mapping stage

V1:

```yaml
fields:
  - name: STATUS_NAME
    dependsOn:
      - STATUS
    stages:
      - type: MAPPING
        mapping:
          1: ACTIVE
          0: INACTIVE
```

V2:

```yaml
transform:
  type: sql
  sql: |
    SELECT
      STATUS,
      CASE
        WHEN STATUS = '1' THEN 'ACTIVE'
        WHEN STATUS = '0' THEN 'INACTIVE'
        ELSE NULL
      END AS STATUS_NAME
    FROM input
```

### Pattern 3: condition stage

V1:

```yaml
fields:
  - name: LEVEL
    dependsOn:
      - SCORE
    stages:
      - type: CONDITION
```

V2:

```yaml
transform:
  type: sql
  sql: |
    SELECT
      SCORE,
      CASE
        WHEN SCORE >= 90 THEN 'A'
        WHEN SCORE >= 80 THEN 'B'
        ELSE 'C'
      END AS LEVEL
    FROM input
```

### Pattern 4: convert stage

V1:

```yaml
fields:
  - name: AMOUNT_LONG
    dependsOn:
      - AMOUNT
    stages:
      - type: CONVERT
```

V2:

```yaml
transform:
  type: sql
  sql: |
    SELECT
      AMOUNT,
      CAST(AMOUNT AS BIGINT) AS AMOUNT_LONG
    FROM input
```

## Suggested UDF Replacement Table

| V1 Expression Style | V2 Direction |
|---|---|
| `#faker.snowflake.next` | `FAKER_SNOWFLAKE()` |
| `#faker.common.text(a,b)` | `FAKER_TEXT(a, b)` |
| `#faker.number.numberBetween(a,b)` | `FAKER_NUMBER_BETWEEN(a, b)` |
| `#faker.phoneNumber.cellPhone` | `FAKER_PHONE_CELL()` |
| `#faker.expression("#{date.past ...}")` | `FAKER_DATE_PAST(...)` |
| `#faker.datetime.now()` | `FAKER_DATETIME_NOW()` |
| `#faker.datetime.seconds()` | `FAKER_DATETIME_SECONDS()` |
| `#faker.datetime.minusDays(n)` | `FAKER_DATETIME_MINUS_DAYS(n)` |
| `#faker.datetime.minusDays(x,n)` | `FAKER_DATETIME_MINUS_DAYS(x, n)` |
| `#faker.datetime.minusHours(n)` | `FAKER_DATETIME_MINUS_HOURS(n)` |
| `#faker.datetime.minusHours(x,n)` | `FAKER_DATETIME_MINUS_HOURS(x, n)` |
| `#faker.datetime.minusMinutes(n)` | `FAKER_DATETIME_MINUS_MINUTES(n)` |
| `#faker.datetime.minusMinutes(x,n)` | `FAKER_DATETIME_MINUS_MINUTES(x, n)` |
| `#faker.datetime.minusSeconds(n)` | `FAKER_DATETIME_MINUS_SECONDS(n)` |
| `#faker.datetime.minusSeconds(x,n)` | `FAKER_DATETIME_MINUS_SECONDS(x, n)` |
| `#faker.datetime.plusDays(n)` | `FAKER_DATETIME_PLUS_DAYS(n)` |
| `#faker.datetime.plusDays(x,n)` | `FAKER_DATETIME_PLUS_DAYS(x, n)` |
| `#faker.datetime.plusHours(n)` | `FAKER_DATETIME_PLUS_HOURS(n)` |
| `#faker.datetime.plusHours(x,n)` | `FAKER_DATETIME_PLUS_HOURS(x, n)` |
| `#faker.datetime.plusMinutes(n)` | `FAKER_DATETIME_PLUS_MINUTES(n)` |
| `#faker.datetime.plusMinutes(x,n)` | `FAKER_DATETIME_PLUS_MINUTES(x, n)` |
| `#faker.datetime.plusSeconds(n)` | `FAKER_DATETIME_PLUS_SECONDS(n)` |
| `#faker.datetime.plusSeconds(x,n)` | `FAKER_DATETIME_PLUS_SECONDS(x, n)` |
| `#faker.datetime.parse(x)` | `FAKER_DATETIME_PARSE(x)` |
| `#faker.datetime.beforeDays(min,max)` | `FAKER_DATETIME_BEFORE_DAYS(min, max)` |
| `#faker.datetime.beforeDays(x,min,max)` | `FAKER_DATETIME_BEFORE_DAYS(x, min, max)` |
| `#faker.datetime.beforeHours(min,max)` | `FAKER_DATETIME_BEFORE_HOURS(min, max)` |
| `#faker.datetime.beforeHours(x,min,max)` | `FAKER_DATETIME_BEFORE_HOURS(x, min, max)` |
| `#faker.datetime.beforeMinutes(min,max)` | `FAKER_DATETIME_BEFORE_MINUTES(min, max)` |
| `#faker.datetime.beforeMinutes(x,min,max)` | `FAKER_DATETIME_BEFORE_MINUTES(x, min, max)` |
| `#faker.datetime.beforeSeconds(min,max)` | `FAKER_DATETIME_BEFORE_SECONDS(min, max)` |
| `#faker.datetime.beforeSeconds(x,min,max)` | `FAKER_DATETIME_BEFORE_SECONDS(x, min, max)` |
| `#faker.datetime.afterDays(min,max)` | `FAKER_DATETIME_AFTER_DAYS(min, max)` |
| `#faker.datetime.afterDays(x,min,max)` | `FAKER_DATETIME_AFTER_DAYS(x, min, max)` |
| `#faker.datetime.afterHours(min,max)` | `FAKER_DATETIME_AFTER_HOURS(min, max)` |
| `#faker.datetime.afterHours(x,min,max)` | `FAKER_DATETIME_AFTER_HOURS(x, min, max)` |
| `#faker.datetime.afterMinutes(min,max)` | `FAKER_DATETIME_AFTER_MINUTES(min, max)` |
| `#faker.datetime.afterMinutes(x,min,max)` | `FAKER_DATETIME_AFTER_MINUTES(x, min, max)` |
| `#faker.datetime.afterSeconds(min,max)` | `FAKER_DATETIME_AFTER_SECONDS(min, max)` |
| `#faker.datetime.afterSeconds(x,min,max)` | `FAKER_DATETIME_AFTER_SECONDS(x, min, max)` |
| `#faker.datetime.format(x)` | `FAKER_DATETIME_FORMAT(x)` |
| `#faker.datetime.format(x,'yyyy-MM-dd')` | `FAKER_DATETIME_FORMAT(x, 'yyyy-MM-dd')` or `V2_FORMAT_DATE(...)` when the value is already a date |
| `#faker.vehicleCN.plateProvince(x)` | `FAKER_VEHICLE_CN_PLATE_PROVINCE(x)` |
| `#faker.snowflake.viid(deviceId, baseType, passTime, semanticType)` | `FAKER_SNOWFLAKE_VIID(deviceId, baseType, passTime, semanticType)` |
| custom SpEL utility chains | targeted repository-local UDFs |

Real migration examples:

- see `docs/calcite-v1-v2-migration-examples.md` for repository-backed V1-to-V2 rewrites covering query source convergence, faker UDF migration, mapping/condition rewrites, and multi-sink fan-out

Current V2 UDF status:

- `TemplateV2SqlFunctionRegistry` is the extension point for repository and future plugin-provided SQL functions
- built-in date helpers currently use the `V2_*` namespace to avoid Calcite dialect-function conflicts
- custom UDFs must register both Calcite validation metadata and runtime evaluator behavior
- future faker/script compatibility functions should be added through this registry, not by hard-coding more evaluator branches

## Coverage-Oriented Migration Plan

### Wave 1 - Direct migration patterns

- [x] map `MAPPING` to `CASE WHEN`
- [x] map `CONDITION` to `CASE WHEN / WHERE`
- [x] map `CONVERT` to `CAST / UDF`
- [x] map simple dependent `SCRIPT` stages to SQL projection

Expected coverage:

- a large fraction of current row-local transformation templates
- repository-backed migration examples now exist in `docs/calcite-v1-v2-migration-examples.md`

### Wave 2 - Source normalization

- [x] map number/constant/datetime iterators to `RowSource`
- [x] map JDBC/file readers to named sources
- [x] converge `DatabaseIterator` and `JdbcReader` into one query-backed source family at the model/mapping level
- [x] complete runtime migration entrypoints so new V2 authoring only exposes `QuerySourceVO`
- [x] preserve `SelectStrategy` as source policy
- [x] introduce `AiSourceVO`
- [x] define explicit schema rules for first-phase V2 templates

Expected coverage:

- removal of most `READ + SELECT` configuration chains from V2 authoring
- exact V1 selection semantics are still partial even though the source policy model/runtime is in place

### Wave 3 - Sink reuse

- [x] map console writer first
- [x] map DB writers
- [x] map Kafka writer
- [x] map Elasticsearch writer
- [x] add multi-sink fan-out
- [x] add configurable multi-sink failure policy

Expected coverage:

- V2 can execute on the repository's main output paths without writer rewrites

### Wave 4 - Script reduction

- [x] map the common SpEL expression subset to SQL/UDFs
- [ ] identify which JavaScript usage remains compatibility-only
- [x] publish a script-to-UDF migration guide

Expected coverage:

- SQL becomes the preferred transformation language for new templates
- remaining work is the long-tail SpEL/faker catalog, not the first migration path

### Wave 5 - Compatibility stabilization

- [ ] document V1-only stage semantics
- [ ] keep orchestration-heavy templates on V1
- [ ] mark unsupported direct V2 migrations explicitly

Expected coverage:

- V1 and V2 coexistence remains intentional and understandable

### Wave 6 - V1 parity and beyond

- [ ] define the V1 parity scoreboard
- [ ] verify business-relevant V1 template families against V2 coverage
- [ ] close the remaining high-value V1 gaps
- [ ] add V2-only composition advantages such as richer source composition and sink fan-out

Expected coverage:

- V2 reaches V1 business parity and becomes the preferred long-term authoring model

## Recommendation

Use this mapping as the operational migration backlog:

- move transformation semantics first
- move source declarations second
- reuse sink infrastructure third
- keep runtime orchestration and complex procedural scripting outside the Calcite core

This keeps the V2 effort focused on the part of the system where Calcite produces the highest simplification return.
