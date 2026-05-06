# Calcite / Template V2 Migration Examples

## Purpose

This document turns real V1 templates in the repository into concrete V2 authoring examples.

It is not a byte-for-byte converter output. It shows the preferred V2 shape after migration:

- keep source definitions explicit
- move row-local logic into SQL
- replace high-frequency SpEL/faker calls with built-in SQL functions
- keep sink fan-out and failure policy explicit

Related references:

- `docs/calcite-v1-v2-mapping.md`
- `docs/calcite-v1-parity-scorecard.md`
- `docs/calcite-implementation-status.md`

## Example 1. `tocc/parking/01_car_detect_info`

Source template:

- `data-generator-service/src/main/resources/template/tocc/parking/01_car_detect_info.yaml`

Representative V1 patterns:

- `#faker.snowflake.next`
- `#faker.datetime.format(#faker.datetime.minusHours(#dataset),'yyMMddHHmmss')`
- `#faker.vehicleCN.plateProvince(#dataset)`
- `#faker.snowflake.viid(...)`
- field extraction from constant object datasets

Suggested V2 shape:

```yaml
name: parking_car_detect_info_v2
sources:
  seq:
    type: iterator
    iterator:
      type: number
      from: 1
      to: 100
      step: 1
  vehicle:
    type: iterator
    iterator:
      type: constant
      dataset:
        - HPHM: 粤A379T7
          HPZL: 2
          CLLX: 24
          CSYS: 10
          HPYS: 0
        - HPHM: 粤B12345
          HPZL: 2
          CLLX: 24
          CSYS: 1
          HPYS: 1
  device:
    type: iterator
    iterator:
      type: constant
      dataset:
        - KKBH: 440112000000111112
          SXJSBBM: 44011200000011111201
        - KKBH: 440112000000111113
          SXJSBBM: 44011200000011111301

transformers:
  - name: project
    type: sql
    sql: |
      SELECT
        FAKER_SNOWFLAKE() AS INFO_ID,
        vehicle.HPZL AS HPZL,
        vehicle.CLLX AS CLLX,
        vehicle.CSYS AS CSYS,
        vehicle.HPHM AS HPHM,
        vehicle.HPYS AS HPYS,
        coalesce(device.SXJSBBM, concat(device.KKBH, '01')) AS DEVICE_ID,
        FAKER_DATETIME_FORMAT(
          FAKER_DATETIME_MINUS_HOURS(seq.value),
          'yyMMddHHmmss'
        ) AS JGSK,
        FAKER_VEHICLE_CN_PLATE_PROVINCE(vehicle.HPHM) AS XZQH,
        FAKER_SNOWFLAKE_VIID(
          coalesce(device.SXJSBBM, concat(device.KKBH, '01')),
          '02',
          FAKER_DATETIME_FORMAT(FAKER_DATETIME_NOW(), 'yyyyMMddHHmmss'),
          '02'
        ) AS VIID
      FROM seq
      INNER JOIN vehicle ON 1 = 1
      INNER JOIN device ON 1 = 1

sinkExecutionPolicy:
  mode: FAIL_FAST

sink:
  writers:
    - type: kafka
      dataSourceId: parking_kafka
      target: car_detect_info
```

Migration notes:

- V1 field-by-field `SCRIPT` extraction becomes one SQL projection.
- Nested faker datetime chains become composable `FAKER_*` SQL functions.
- `SELECT` on constant reader pools should move into source policy only when exact selection semantics matter.
- `KAFKA` sink can stay on the existing writer shape and only moves into V2 sink placement.

## Example 2. `tocc/parking/02_parking_user_vehicle`

Source template:

- `data-generator-service/src/main/resources/template/tocc/parking/02_parking_user_vehicle.yaml`

Representative V1 patterns:

- `#faker.phoneNumber.cellPhone`
- `#faker.expression("#{date.past '1','DAYS','yyyy-MM-dd HH:mm:ss'}")`
- `#dataset.HPHM`

Suggested V2 shape:

```yaml
name: parking_user_vehicle_v2
sources:
  vehicle:
    type: iterator
    iterator:
      type: constant
      dataset:
        - HPHM: 粤A11111
          HPYS: 0
          HPZL: 2
        - HPHM: 粤B22222
          HPYS: 1
          HPZL: 2

transform:
  type: sql
  sql: |
    SELECT
      FAKER_SNOWFLAKE() AS ID,
      FAKER_PHONE_CELL() AS PHONE,
      vehicle.HPHM AS PLATE_NUMBER,
      vehicle.HPYS AS PLATE_COLOR,
      vehicle.HPZL AS PLATE_TYPE,
      'S0A' AS STATUS,
      FAKER_DATE_PAST(1, 'yyyy-MM-dd HH:mm:ss') AS CREATE_TIME,
      FAKER_DATE_PAST(1, 'yyyy-MM-dd HH:mm:ss') AS UPDATE_TIME
    FROM vehicle

sink:
  writers:
    - type: console
```

Migration notes:

- direct field passthrough should not stay in separate stage chains
- `phoneNumber.cellPhone` and `date.past` are already covered by built-in V2 UDFs
- if `UPDATE_TIME` must equal `CREATE_TIME`, split into two transformers and carry the alias forward instead of re-evaluating the faker call

## Example 3. `idps/inet-cloud-control/03_VEHICLE_OPERATION`

Source template:

- `data-generator-service/src/main/resources/template/idps/inet-cloud-control/03_VEHICLE_OPERATION.yaml`

Representative V1 patterns:

- `#faker.datetime.parse(#faker.expression("#{date.past ...}"))`
- `#faker.datetime.format(#dataset)`
- `#faker.datetime.parse(#faker.datetime.afterMinutes(#dataset,120,640))`
- `#faker.datetime.now()`

Suggested V2 shape:

```yaml
name: vehicle_operation_v2
sources:
  base:
    type: query
    dataSourceId: vehicle_ds
    sql: |
      select plate_no, plate_type, safety, safety_phone
      from vehicle_operation_seed
    maxRows: 1000

transformers:
  - name: derive_time
    type: sql
    sql: |
      SELECT
        plate_no,
        plate_type,
        safety,
        safety_phone,
        FAKER_DATETIME_PARSE(
          FAKER_DATE_PAST(1, 'yyyy-MM-dd HH:mm:ss')
        ) AS START_TIME
      FROM base
  - name: project
    type: sql
    sql: |
      SELECT
        FAKER_SNOWFLAKE() AS ID,
        plate_no AS PLATE_NO,
        plate_type AS PLATE_TYPE,
        START_TIME,
        FAKER_DATETIME_PARSE(
          FAKER_DATETIME_AFTER_MINUTES(START_TIME, 120, 640)
        ) AS END_TIME,
        safety AS SAFETY_NAME,
        safety_phone AS SAFETY_PHONE,
        FAKER_NUMBER_BETWEEN(3000, 15000) AS OPERATION_MILEAGE,
        FAKER_DATETIME_NOW() AS REPORT_TIME
      FROM derive_time

sink:
  writers:
    - type: elasticsearch
      dataSourceId: vehicle_es
      target: vehicle_operation
      options:
        id: ID
```

Migration notes:

- when one derived field feeds another field, prefer multiple SQL transformers instead of rebuilding field-level dependency chains
- `QuerySourceVO` is the final V2 shape for database-backed inputs
- `datetime.parse/afterMinutes/now` can stay fully inside SQL now

## Example 4. `demo/14_结果映射样例`

Source template:

- `data-generator-service/src/main/resources/template/demo/14_结果映射样例.yaml`

Representative V1 patterns:

- weighted `READ`
- `MAPPING` stage

Suggested V2 shape:

```yaml
name: demo_mapping_v2
sources:
  input:
    type: iterator
    iterator:
      type: number
      from: 1
      to: 10
      step: 1

transform:
  type: sql
  sql: |
    SELECT
      FAKER_SNOWFLAKE() AS ID,
      CASE input.value
        WHEN 1 THEN 101
        WHEN 2 THEN 102
        WHEN 3 THEN 103
        WHEN 4 THEN 104
        WHEN 5 THEN 105
        WHEN 6 THEN 106
        WHEN 7 THEN 107
        WHEN 8 THEN 108
        WHEN 9 THEN 109
        WHEN 10 THEN 110
        ELSE 100
      END AS MAP_NO
    FROM input

sink:
  writers:
    - type: console
```

Migration notes:

- `MAPPING` is a direct `CASE` rewrite target
- weighted multi-reader `READ` should be modeled as source policy only when the reader pool itself must be preserved

## Example 5. `demo/18_数据库查询迭代器样例`

Source template:

- `data-generator-service/src/main/resources/template/demo/18_数据库查询迭代器样例.yaml`

Representative V1 patterns:

- `iterator.type=database`
- query params
- paging fields on iterator

Suggested V2 baseline shape:

```yaml
name: demo_query_source_v2
sources:
  iterator:
    type: query
    dataSourceId: utcs_dm
    sql: >
      select "id","crossing_id","crossing_name","longitude","latitude"
      from "UTCS"."utcs_bas_siteinfo_new_temp"
    pageIndex: 1
    pageSize: 3
    maxRows: 10
    params:
      - name: key1
        language:
          type: plain
          content: value1

transform:
  type: sql
  sql: |
    SELECT
      FAKER_SNOWFLAKE() AS ID,
      FAKER_TEXT(9, 10) AS STATUS,
      FAKER_DATE_PAST(1, 'yyyy-MM-dd HH:mm:ss') AS CREATE_TIME,
      iterator.id,
      iterator.crossing_id,
      iterator.crossing_name,
      iterator.longitude,
      iterator.latitude
    FROM iterator

sink:
  writers:
    - type: console
```

Migration notes:

- `DatabaseIterator` and `JdbcReader` should converge into `QuerySourceVO`
- current controller-side migration path already uses `SELECT * FROM iterator` as the safe baseline for single-query-source templates
- iterator-side `choose/otherwise/log` remains compatibility-only and should not be forced into V2 SQL

## Example 6. `demo/10_多个写入器样例`

Source template:

- `data-generator-service/src/main/resources/template/demo/10_多个写入器样例.yaml`

Representative V1 patterns:

- one data preparation path
- multiple JDBC writers

Suggested V2 shape:

```yaml
name: demo_multi_sink_v2
sources:
  area:
    type: query
    dataSourceId: system_manage
    sql: |
      SELECT AREA_CODE, AREA_NAME, DISTRICT_CODE, DISTRICT_NAME
      FROM SM_AREA
      WHERE PARENT_CODE = '4409'
    maxRows: 10
  district:
    type: query
    dataSourceId: system_manage
    sql: |
      SELECT CODE, NAME, PARENT_CODE
      FROM PC_DISTRICT

transform:
  type: sql
  sql: |
    SELECT
      FAKER_SNOWFLAKE() AS ID,
      area.AREA_NAME AS AREA_NAME,
      district.NAME AS DISTRICT_NAME,
      district.NAME AS AREA_DISTRICT_NAME
    FROM area
    INNER JOIN district
      ON area.DISTRICT_CODE = district.CODE

sinkExecutionPolicy:
  mode: FAIL_FAST

sinks:
  - writers:
      - type: console
  - writers:
      - type: jdbc
        dataSourceId: tocc_driving_school
        target: SM_TEST
        template: ID,AREA_NAME,DISTRICT_NAME,AREA_DISTRICT_NAME
  - writers:
      - type: jdbc
        dataSourceId: tocc_training
        target: SM_TEST
        template: ID,AREA_NAME,DISTRICT_NAME,AREA_DISTRICT_NAME
```

Migration notes:

- V2 should prefer one relational transform plus multi-sink fan-out
- multi-sink failure handling is explicit through `sinkExecutionPolicy.mode`
- this is already a better long-term shape than V1 writer duplication around field chains

## Example 7. `demo/16_AI生成样例`

Source template:

- `data-generator-service/src/main/resources/template/demo/16_AI生成样例.yaml`

Representative V1 patterns:

- `READ -> AI reader`
- `provider.type=OLLAMA`
- parser by class name
- AI-generated list-like values reused as in-memory datasets

Suggested V2 shape:

```yaml
name: demo_ai_source_v2
sources:
  titles:
    type: ai
    api: http://172.25.20.12:11434
    provider:
      type: OLLAMA
      options:
        model: qwen2
    prompt: >-
      生成10个中文培训计划标题，返回逗号分隔的值列表。
    parser: org.gensokyo.data.ai.parser.ListOutputParser
  locations:
    type: ai
    api: http://172.25.20.12:11434
    provider:
      type: OLLAMA
      options:
        model: qwen2
    prompt: >-
      生成10条广州车辆轨迹，返回 RFC8259 兼容 JSON 数组。
    parser: org.gensokyo.data.ai.parser.ListOutputParser

transformers:
  - name: titles_project
    type: sql
    sql: |
      SELECT content AS TITLE
      FROM titles
  - name: locations_project
    type: sql
    sql: |
      SELECT content AS LOCATION
      FROM locations

sink:
  writers:
    - type: console
```

Migration notes:

- V2 should prefer first-class `AiSourceVO` instead of keeping AI access inside V1 `READ` stage chains.
- Current `OLLAMA` support is service-wired through `OllamaAiRuntimeBridge`; parser can be resolved by Spring bean name or parser class name.
- When the parser returns structured rows instead of plain strings, declare `schema` explicitly and query the returned columns directly.
- Two AI sources can stay independent in V2; if they need relational composition later, use multiple named sources plus SQL joins or staged transforms.

## Source Policy Mapping

Current V2 `SourcePolicyVO` is a source materialization policy, not a byte-for-byte clone of V1 `SELECT` stage consumption semantics.

| V1 Selector | V1 Core Semantics | Current V2 Mapping | Current Status | Notes |
|---|---|---|---|---|
| `REPEAT_ORDER` | ordered selection, wraps and repeats | `selectionStrategy=REPEAT_ORDER` + `limit` | Partial | current V2 returns ordered rows, but does not model per-call wraparound state |
| `ONCE_ORDER` | ordered selection, removes selected rows | `selectionStrategy=ONCE_ORDER` + `limit` | Partial | current V2 orders and limits, but does not consume rows across calls |
| `MULTIPLE_ORDER` | ordered selection, repeat N times before consuming next row | `selectionStrategy=MULTIPLE_ORDER` + `limit` | Partial | current V2 treats it as ordered materialization alias only |
| `REPEAT_RANDOM` | random selection with replacement | `selectionStrategy=REPEAT_RANDOM` + `limit` | Partial | current V2 uses deterministic shuffle + limit, not per-call random draws |
| `ONCE_RANDOM` | random selection without replacement | `selectionStrategy=ONCE_RANDOM` + `limit` | Partial | current V2 uses deterministic shuffle + limit, not consumptive random removal |
| reader `EQUAL` | fair/random reader choice | no exact V2 equivalent | Partial | usually migrate by collapsing readers into explicit sources or accepting source-policy approximation |
| reader `WEIGHT` | weighted reader choice | no exact V2 equivalent | Partial | keep compatibility path unless weighted reader-pool behavior is truly required |

Current recommended migration rule:

- If the old template only needs stable ordered or shuffled materialization, use `SourcePolicyVO`.
- If the old template depends on per-call depletion, repeated reuse count, or reader-pool weight fairness, keep the path compatibility-first for now or rewrite the business intent explicitly in V2 source design.

## Recommended Migration Order

1. Move `DATABASE/JDBC/CSV/JSON/EXCEL` inputs into named `sources`.
2. Collapse field-level `SCRIPT/MAPPING/CONDITION/CONVERT` chains into one or more SQL transformers.
3. Replace the observed high-frequency faker/SpEL subset with built-in `FAKER_*` functions.
4. Keep `LOG/PAUSE/SHARED/iterator choose/otherwise/generator scheduling` on the compatibility path until a separate orchestration model exists.
5. Move outputs into `sink` or `sinks` and set `sinkExecutionPolicy.mode` explicitly.

## Current Boundary

Already practical for direct V2 migration:

- single-query and multi-query source templates
- file-backed CSV / JSON / Excel sources
- console / JDBC / Kafka / Elasticsearch / CSV / JSON / Excel sinks
- row-local mapping, condition, convert, and common faker-based SQL rewrites
- multi-sink fan-out with `FAIL_FAST` or `CONTINUE_ON_ERROR`

Still better treated as compatibility-only for now:

- procedural JavaScript stages
- pause/log/shared orchestration behavior
- iterator-local branch execution
- exact V1 weighted reader-pool semantics where the old random/fairness contract must be preserved byte-for-byte

## Notes

- V2 `iterator.type=constant` examples should use `dataset` and optional `repeat`, matching the current implemented `ConstantIteratorVO` shape.
- The examples here prefer readable business identifiers and preferred V2 authoring shape over byte-for-byte converter output.
