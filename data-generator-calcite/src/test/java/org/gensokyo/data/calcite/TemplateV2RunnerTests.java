package org.gensokyo.data.calcite;

import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.iterator.ConstantIteratorVO;
import org.gensokyo.data.iterator.DateTimeIteratorVO;
import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.ExcelSheetSourceVO;
import org.gensokyo.data.model.v2.ExcelSourceVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.JsonSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class TemplateV2RunnerTests {

    @Test
    void runsSingleSourceSingleTransformSingleSinkTemplate() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2");
        template.setSources(Map.of("seed", numberSource(1, 5, 1)));
        template.setTransformers(List.of(sql("SELECT value, value + 10 AS shifted FROM seed WHERE value >= 4")));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("4", result.getRows().get(0).getString("value"));
        Assertions.assertEquals("14", result.getRows().get(0).getString("shifted"));
        Assertions.assertEquals("15", result.getRows().get(1).getString("shifted"));
    }

    @Test
    void supportsMultipleSourcesInSingleSqlContext() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2");
        template.setSources(Map.of(
                "left_input", numberSource(1, 2, 1),
                "right_input", numberSource(3, 4, 1)
        ));
        template.setTransformers(List.of(sql("SELECT value FROM right_input WHERE value >= 4")));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals("4", result.getRows().get(0).getString("value"));
    }

    @Test
    void runsConstantIteratorSourceThroughSqlTransform() {
        ConstantIteratorVO iterator = new ConstantIteratorVO();
        iterator.setDataset(List.of("alpha", "beta"));
        iterator.setRepeat(2);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-constant-iterator");
        template.setSources(Map.of("seed", source));
        template.setTransformers(List.of(sql("SELECT value FROM seed WHERE value = 'beta'")));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("beta", result.getRows().getFirst().getString("value"));
    }

    @Test
    void runsDatetimeIteratorSourceThroughSqlTransform() {
        DateTimeIteratorVO iterator = new DateTimeIteratorVO();
        iterator.setFrom(LocalDateTime.parse("2026-05-01T00:00:00"));
        iterator.setTo(LocalDateTime.parse("2026-05-03T00:00:00"));
        iterator.setStep(1);
        iterator.setUnit(ChronoUnit.DAYS);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-datetime-iterator");
        template.setSources(Map.of("seed", source));
        template.setTransformers(List.of(sql("SELECT value FROM seed WHERE value >= TIMESTAMP '2026-05-02 00:00:00'")));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("2026-05-02T00:00", result.getRows().getFirst().getString("value"));
    }

    @Test
    void supportsInnerJoinAcrossMultipleSources() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-join");
        template.setSources(Map.of(
                "left_input", numberSource(1, 3, 1),
                "right_input", numberSource(2, 4, 1)
        ));
        template.setTransformers(List.of(sql("""
                SELECT l.value AS left_value, r.value AS right_value
                FROM left_input AS l
                INNER JOIN right_input AS r ON l.value = r.value
                WHERE r.value >= 2
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("2", result.getRows().get(0).getString("left_value"));
        Assertions.assertEquals("2", result.getRows().get(0).getString("right_value"));
        Assertions.assertEquals("3", result.getRows().get(1).getString("left_value"));
        Assertions.assertEquals("3", result.getRows().get(1).getString("right_value"));
    }

    @Test
    void supportsLeftJoinAcrossMultipleSources() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-left-join");
        template.setSources(Map.of(
                "left_input", numberSource(1, 3, 1),
                "right_input", numberSource(2, 2, 1)
        ));
        template.setTransformers(List.of(sql("""
                SELECT l.value AS left_value, r.value AS right_value
                FROM left_input AS l
                LEFT JOIN right_input AS r ON l.value = r.value
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(3, result.getRows().size());
        Assertions.assertEquals("1", result.getRows().get(0).getString("left_value"));
        Assertions.assertNull(result.getRows().get(0).getString("right_value"));
        Assertions.assertEquals("2", result.getRows().get(1).getString("left_value"));
        Assertions.assertEquals("2", result.getRows().get(1).getString("right_value"));
        Assertions.assertEquals("3", result.getRows().get(2).getString("left_value"));
        Assertions.assertNull(result.getRows().get(2).getString("right_value"));
    }

    @Test
    void supportsOrderByAndLimit() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-order-limit");
        template.setSources(Map.of("seed", numberSource(1, 5, 1)));
        template.setTransformers(List.of(sql("""
                SELECT value
                FROM seed
                ORDER BY value DESC
                LIMIT 2
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("5", result.getRows().get(0).getString("value"));
        Assertions.assertEquals("4", result.getRows().get(1).getString("value"));
    }

    @Test
    void supportsOrderByOffsetAndFetch() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-order-offset-fetch");
        template.setSources(Map.of("seed", numberSource(1, 5, 1)));
        template.setTransformers(List.of(sql("""
                SELECT value
                FROM seed
                ORDER BY value DESC
                OFFSET 1 ROWS FETCH NEXT 2 ROWS ONLY
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("4", result.getRows().get(0).getString("value"));
        Assertions.assertEquals("3", result.getRows().get(1).getString("value"));
    }

    @Test
    void supportsCaseWhenAndNullPredicates() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-case-null");
        template.setSources(Map.of("nullable_seed", new RegistryOnlySourceVO()));
        template.setTransformers(List.of(sql("""
                SELECT name,
                       CASE WHEN score IS NULL THEN 'missing'
                            WHEN score >= 20 THEN 'high'
                            ELSE 'low'
                       END AS bucket
                FROM nullable_seed
                WHERE score IS NULL OR score IS NOT NULL
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(nullableRegistry()).run(template);

        Assertions.assertEquals(3, result.getRows().size());
        Assertions.assertEquals("missing", result.getRows().get(0).getString("bucket"));
        Assertions.assertEquals("low", result.getRows().get(1).getString("bucket"));
        Assertions.assertEquals("high", result.getRows().get(2).getString("bucket"));
    }

    @Test
    void supportsFirstBatchSqlFunctions() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-functions");
        template.setSources(Map.of("nullable_seed", new RegistryOnlySourceVO()));
        template.setTransformers(List.of(sql("""
                SELECT name,
                       COALESCE(score, 0) AS score_or_zero,
                       CONCAT(UPPER(name), CONCAT('-', LOWER('TAIL'))) AS label,
                       TRIM('  padded  ') AS trimmed
                FROM nullable_seed
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(nullableRegistry()).run(template);

        Assertions.assertEquals(3, result.getRows().size());
        Assertions.assertEquals("0", result.getRows().get(0).getString("score_or_zero"));
        Assertions.assertEquals("EMPTY-tail", result.getRows().get(0).getString("label"));
        Assertions.assertEquals("padded", result.getRows().get(0).getString("trimmed"));
        Assertions.assertEquals("10", result.getRows().get(1).getString("score_or_zero"));
        Assertions.assertEquals("BETA-tail", result.getRows().get(2).getString("label"));
    }

    @Test
    void supportsConversionOrientedSqlFunctions() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-conversion-functions");
        template.setSources(Map.of("nullable_seed", new RegistryOnlySourceVO()));
        template.setTransformers(List.of(sql("""
                SELECT name,
                       NULLIF(name, 'empty') AS normalized_name,
                       CHAR_LENGTH(name) AS name_length,
                       SUBSTRING(name, 1, 2) AS name_prefix,
                       ABS(score - 15) AS score_distance,
                       FLOOR(score / 4) AS score_floor,
                       CEIL(score / 4) AS score_ceil,
                       ROUND(score / 4, 0) AS score_round
                FROM nullable_seed
                WHERE score IS NOT NULL
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(nullableRegistry()).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("alpha", result.getRows().get(0).getString("normalized_name"));
        Assertions.assertEquals("5", result.getRows().get(0).getString("name_length"));
        Assertions.assertEquals("al", result.getRows().get(0).getString("name_prefix"));
        Assertions.assertEquals("5", result.getRows().get(0).getString("score_distance"));
        Assertions.assertEquals("2", result.getRows().get(0).getString("score_floor"));
        Assertions.assertEquals("3", result.getRows().get(0).getString("score_ceil"));
        Assertions.assertEquals("3", result.getRows().get(0).getString("score_round"));
        Assertions.assertEquals("be", result.getRows().get(1).getString("name_prefix"));
    }

    @Test
    void supportsLikeInBetweenAndNotPredicates() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-like-in-between-not");
        template.setSources(Map.of("nullable_seed", new RegistryOnlySourceVO()));
        template.setTransformers(List.of(sql("""
                SELECT name, score
                FROM nullable_seed
                WHERE NOT (name LIKE 'e%')
                  AND name IN ('alpha', 'beta')
                  AND score BETWEEN 10 AND 20
                ORDER BY score DESC
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(nullableRegistry()).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("beta", result.getRows().get(0).getString("name"));
        Assertions.assertEquals("20", result.getRows().get(0).getString("score"));
        Assertions.assertEquals("alpha", result.getRows().get(1).getString("name"));
        Assertions.assertEquals("10", result.getRows().get(1).getString("score"));
    }

    @Test
    void supportsGroupByHavingAndAggregates() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-group-by");
        template.setSources(Map.of("aggregate_seed", new AggregateSourceVO()));
        template.setTransformers(List.of(sql("""
                SELECT category,
                       COUNT(*) AS row_count,
                       COUNT(amount) AS amount_count,
                       SUM(amount) AS amount_sum,
                       AVG(amount) AS amount_avg,
                       MIN(amount) AS amount_min,
                       MAX(amount) AS amount_max
                FROM aggregate_seed
                GROUP BY category
                HAVING COUNT(*) >= 1
                ORDER BY row_count DESC, category ASC
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(aggregateRegistry()).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Row categoryA = result.getRows().get(0);
        Assertions.assertEquals("a", categoryA.getString("category"));
        Assertions.assertEquals("2", categoryA.getString("row_count"));
        Assertions.assertEquals("2", categoryA.getString("amount_count"));
        Assertions.assertEquals("30", categoryA.getString("amount_sum"));
        Assertions.assertEquals("15", categoryA.getString("amount_avg"));
        Assertions.assertEquals("10", categoryA.getString("amount_min"));
        Assertions.assertEquals("20", categoryA.getString("amount_max"));

        Row categoryB = result.getRows().get(1);
        Assertions.assertEquals("b", categoryB.getString("category"));
        Assertions.assertEquals("1", categoryB.getString("row_count"));
        Assertions.assertEquals("1", categoryB.getString("amount_count"));
        Assertions.assertEquals("5", categoryB.getString("amount_sum"));
        Assertions.assertEquals("5", categoryB.getString("amount_avg"));
        Assertions.assertEquals("5", categoryB.getString("amount_min"));
        Assertions.assertEquals("5", categoryB.getString("amount_max"));
    }

    @Test
    void supportsDateOrientedSqlFunctions() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-date-functions");
        template.setSources(Map.of("date_seed", new DateSourceVO()));
        template.setTransformers(List.of(sql("""
                SELECT event_date,
                       V2_FORMAT_DATE('%Y-%m-%d', event_date) AS formatted_date,
                       V2_DATE_ADD(event_date, 2) AS date_plus_two,
                       V2_DATE_SUB(event_date, 1) AS date_minus_one,
                       V2_DATE_DIFF(event_date, base_date) AS days_from_base,
                       YEAR(event_date) AS event_year,
                       MONTH(event_date) AS event_month,
                       DAYOFMONTH(event_date) AS event_day
                FROM date_seed
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(dateRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Row row = result.getRows().getFirst();
        Assertions.assertEquals("2026-05-02", row.getString("event_date"));
        Assertions.assertEquals("2026-05-02", row.getString("formatted_date"));
        Assertions.assertEquals("2026-05-04", row.getString("date_plus_two"));
        Assertions.assertEquals("2026-05-01", row.getString("date_minus_one"));
        Assertions.assertEquals("1", row.getString("days_from_base"));
        Assertions.assertEquals("2026", row.getString("event_year"));
        Assertions.assertEquals("5", row.getString("event_month"));
        Assertions.assertEquals("2", row.getString("event_day"));
    }

    @Test
    void supportsFirstFakerCompatibilityFunctionBatch() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-faker-functions");
        template.setSources(Map.of("date_seed", new DateSourceVO()));
        template.setTransformers(List.of(sql("""
                SELECT FAKER_SNOWFLAKE() AS snowflake_id,
                       FAKER_TEXT(5, 8) AS random_text,
                       FAKER_NUMBER_BETWEEN(40, 60) AS ranged_number,
                       FAKER_PHONE_CELL() AS phone_cell,
                       FAKER_DATE_PAST(1, 'yyyy-MM-dd HH:mm:ss') AS past_time,
                       FAKER_DATETIME_NOW() AS now_text,
                       FAKER_DATETIME_SECONDS() AS epoch_seconds,
                       FAKER_DATETIME_MINUS_DAYS(event_time, 1) AS minus_days,
                       FAKER_DATETIME_MINUS_HOURS(event_time, 2) AS minus_hours,
                       FAKER_DATETIME_PLUS_HOURS(event_time, 3) AS plus_hours,
                       FAKER_DATETIME_FORMAT(event_time, 'yyMMddHHmmss') AS compact_time
                FROM date_seed
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(dateRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Row row = result.getRows().getFirst();
        Assertions.assertTrue(Long.parseLong(row.getString("snowflake_id")) > 0);
        Assertions.assertFalse(row.getString("random_text").isBlank());
        int rangedNumber = Integer.parseInt(row.getString("ranged_number"));
        Assertions.assertTrue(rangedNumber >= 40 && rangedNumber < 60);
        Assertions.assertTrue(row.getString("phone_cell").matches("1\\d{10}"));
        Assertions.assertEquals(19, row.getString("past_time").length());
        Assertions.assertEquals(19, row.getString("now_text").length());
        Assertions.assertTrue(Long.parseLong(row.getString("epoch_seconds")) > 0);
        Assertions.assertEquals("2026-05-01 10:20:30", row.getString("minus_days"));
        Assertions.assertEquals("2026-05-02 08:20:30", row.getString("minus_hours"));
        Assertions.assertEquals("2026-05-02 13:20:30", row.getString("plus_hours"));
        Assertions.assertEquals("260502102030", row.getString("compact_time"));
    }

    @Test
    void supportsFakerDatetimeMigrationPathForLegacyCompactStrings() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-faker-legacy-datetime");
        template.setSources(Map.of("legacy_time_seed", new LegacyCompactTimeSourceVO()));
        template.setTransformers(List.of(sql("""
                SELECT raw_time,
                       FAKER_DATETIME_MINUS_HOURS(raw_time, 2) AS minus_hours,
                       FAKER_DATETIME_PLUS_HOURS(raw_time, 1) AS plus_hours,
                       FAKER_DATETIME_FORMAT(raw_time, 'yyyy-MM-dd HH:mm:ss') AS normalized_time
                FROM legacy_time_seed
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(legacyTimeRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Row row = result.getRows().getFirst();
        Assertions.assertEquals("2026-05-02 08:20:30", row.getString("minus_hours"));
        Assertions.assertEquals("2026-05-02 11:20:30", row.getString("plus_hours"));
        Assertions.assertEquals("2026-05-02 10:20:30", row.getString("normalized_time"));
    }

    @Test
    void supportsSecondFakerCompatibilityFunctionBatch() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-faker-second-batch");
        template.setSources(Map.of("vehicle_seed", new VehicleSourceVO()));
        template.setTransformers(List.of(sql("""
                SELECT plate,
                       pass_time,
                       FAKER_DATETIME_PARSE(pass_time) AS parsed_time,
                       FAKER_DATETIME_AFTER_MINUTES(pass_time, 30, 120) AS delayed_time,
                       FAKER_VEHICLE_CN_PLATE_PROVINCE(plate) AS province_code,
                       FAKER_SNOWFLAKE_VIID(device_id, '02', compact_time, '02') AS viid_code
                FROM vehicle_seed
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(vehicleRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Row row = result.getRows().getFirst();
        Assertions.assertEquals("2026-05-02 10:20:30", row.getString("parsed_time"));
        LocalDateTime delayed = LocalDateTime.parse(row.getString("delayed_time").replace(' ', 'T'));
        LocalDateTime base = LocalDateTime.parse("2026-05-02T10:20:30");
        long delayMinutes = java.time.Duration.between(base, delayed).toMinutes();
        Assertions.assertTrue(delayMinutes >= 30 && delayMinutes < 120);
        Assertions.assertEquals("5", row.getString("province_code"));
        String viidCode = row.getString("viid_code");
        Assertions.assertTrue(viidCode.startsWith("44010000001102260502102030"));
        Assertions.assertTrue(viidCode.matches("44010000001102260502102030\\d+02\\d+"));
    }

    @Test
    void supportsThirdFakerDatetimeCompatibilityBatch() {
        LocalDateTime before = LocalDateTime.now().withNano(0);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-faker-third-batch");
        template.setSources(Map.of("date_seed", new DateSourceVO()));
        template.setTransformers(List.of(sql("""
                SELECT FAKER_DATETIME_MINUS_DAYS(3) AS minus_days_now,
                       FAKER_DATETIME_MINUS_HOURS(5) AS minus_hours_now,
                       FAKER_DATETIME_PLUS_HOURS(7) AS plus_hours_now,
                       FAKER_DATETIME_PLUS_DAYS(event_time, 2) AS plus_days,
                       FAKER_DATETIME_MINUS_MINUTES(event_time, 15) AS minus_minutes,
                       FAKER_DATETIME_PLUS_SECONDS(event_time, 45) AS plus_seconds,
                       FAKER_DATETIME_FORMAT(event_time) AS default_formatted,
                       FAKER_DATETIME_BEFORE_MINUTES(event_time, 10, 20) AS before_minutes,
                       FAKER_DATETIME_AFTER_HOURS(event_time, 2, 5) AS after_hours,
                       FAKER_DATETIME_AFTER_MINUTES(30, 60) AS after_minutes_now
                FROM date_seed
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(dateRegistry()).run(template);
        LocalDateTime after = LocalDateTime.now().withNano(0);

        Assertions.assertEquals(1, result.getRows().size());
        Row row = result.getRows().getFirst();
        Assertions.assertEquals("2026-05-04 10:20:30", row.getString("plus_days"));
        Assertions.assertEquals("2026-05-02 10:05:30", row.getString("minus_minutes"));
        Assertions.assertEquals("2026-05-02 10:21:15", row.getString("plus_seconds"));
        Assertions.assertEquals("2026-05-02 10:20:30", row.getString("default_formatted"));

        LocalDateTime minusDaysNow = LocalDateTime.parse(row.getString("minus_days_now").replace(' ', 'T'));
        Assertions.assertFalse(minusDaysNow.isBefore(before.minusDays(3)));
        Assertions.assertFalse(minusDaysNow.isAfter(after.minusDays(3)));

        LocalDateTime minusHoursNow = LocalDateTime.parse(row.getString("minus_hours_now").replace(' ', 'T'));
        Assertions.assertFalse(minusHoursNow.isBefore(before.minusHours(5)));
        Assertions.assertFalse(minusHoursNow.isAfter(after.minusHours(5)));

        LocalDateTime plusHoursNow = LocalDateTime.parse(row.getString("plus_hours_now").replace(' ', 'T'));
        Assertions.assertFalse(plusHoursNow.isBefore(before.plusHours(7)));
        Assertions.assertFalse(plusHoursNow.isAfter(after.plusHours(7)));

        LocalDateTime base = LocalDateTime.parse("2026-05-02T10:20:30");
        LocalDateTime beforeMinutes = LocalDateTime.parse(row.getString("before_minutes").replace(' ', 'T'));
        long beforeMinutesDiff = java.time.Duration.between(beforeMinutes, base).toMinutes();
        Assertions.assertTrue(beforeMinutesDiff >= 10 && beforeMinutesDiff < 20);

        LocalDateTime afterHours = LocalDateTime.parse(row.getString("after_hours").replace(' ', 'T'));
        long afterHoursDiff = java.time.Duration.between(base, afterHours).toHours();
        Assertions.assertTrue(afterHoursDiff >= 2 && afterHoursDiff < 5);

        LocalDateTime afterMinutesNow = LocalDateTime.parse(row.getString("after_minutes_now").replace(' ', 'T'));
        long nowMinutesDiff = java.time.Duration.between(before, afterMinutesNow).toMinutes();
        Assertions.assertTrue(nowMinutesDiff >= 30 && nowMinutesDiff <= 60);
    }

    @Test
    void supportsCustomSqlFunctionsThroughRegistry() {
        TemplateV2SqlFunctionRegistry registry = TemplateV2SqlFunctionRegistry.builtIn()
                .with(new TemplateV2SqlFunction("V2_WRAP", ReturnTypes.VARCHAR_NULLABLE, OperandTypes.ANY,
                        context -> "[" + context.stringArgument(0) + "]"));
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-custom-function");
        template.setSources(Map.of("nullable_seed", new RegistryOnlySourceVO()));
        template.setTransformers(List.of(sql("""
                SELECT V2_WRAP(name) AS wrapped_name
                FROM nullable_seed
                WHERE score IS NOT NULL
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RuntimeRegistry runtimeRegistry = new TemplateV2RuntimeRegistry(
                List.of(new RegistryOnlySourceFactory()),
                List.of(new SqlTransformFactory(registry)),
                List.of(new ConsoleSinkFactory())
        );

        TemplateV2RunResult result = new TemplateV2Runner(runtimeRegistry).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("[alpha]", result.getRows().get(0).getString("wrapped_name"));
        Assertions.assertEquals("[beta]", result.getRows().get(1).getString("wrapped_name"));
    }

    @Test
    void readsCsvSourceThroughSqlTransform() throws Exception {
        Path csv = Files.createTempFile("template-v2-source", ".csv");
        Files.writeString(csv, """
                name,score,city
                alpha,10,"New York"
                beta,20,"Paris, FR"
                """);
        CsvSourceVO source = new CsvSourceVO();
        source.setPath(csv.toString());
        source.setSchema(schema(
                new org.gensokyo.data.model.v2.ColumnDef("name", "VARCHAR", false),
                new org.gensokyo.data.model.v2.ColumnDef("score", "BIGINT", false),
                new org.gensokyo.data.model.v2.ColumnDef("city", "VARCHAR", true)
        ));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-csv-source");
        template.setSources(Map.of("people", source));
        template.setTransformers(List.of(sql("""
                SELECT name, city, score + 1 AS score_next
                FROM people
                WHERE score >= 20
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals("beta", result.getRows().getFirst().getString("name"));
        Assertions.assertEquals("Paris, FR", result.getRows().getFirst().getString("city"));
        Assertions.assertEquals("21", result.getRows().getFirst().getString("score_next"));
    }

    @Test
    void readsCsvSourceThroughInjectedParser() throws Exception {
        Path csv = Files.createTempFile("template-v2-source-custom-parser", ".csv");
        Files.writeString(csv, """
                ignored
                """);
        CsvSourceVO source = new CsvSourceVO();
        source.setPath(csv.toString());
        source.setHeader(false);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-csv-parser");
        template.setSources(Map.of("people", source));
        template.setTransformers(List.of(sql("SELECT c1 AS name, c2 + 1 AS score_next FROM people")));
        template.setSinks(List.of(consoleSink()));

        CsvParser parser = (csvSource, lines) -> List.of(List.of("gamma", "30"));
        TemplateV2RuntimeRegistry runtimeRegistry = new TemplateV2RuntimeRegistry(
                List.of(new CsvSourceFactory(parser)),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory())
        );
        TemplateV2RunResult result = new TemplateV2Runner(runtimeRegistry).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals("gamma", result.getRows().getFirst().getString("name"));
        Assertions.assertEquals("31", result.getRows().getFirst().getString("score_next"));
    }

    @Test
    void readsJsonSourceThroughSqlTransform() throws Exception {
        Path json = Files.createTempFile("template-v2-source", ".json");
        Files.writeString(json, """
                [
                  {"name":"alpha","score":10,"active":true},
                  {"name":"beta","score":20,"active":false}
                ]
                """);
        JsonSourceVO source = new JsonSourceVO();
        source.setPath(json.toString());
        source.setSchema(schema(
                new org.gensokyo.data.model.v2.ColumnDef("name", "VARCHAR", false),
                new org.gensokyo.data.model.v2.ColumnDef("score", "BIGINT", false),
                new org.gensokyo.data.model.v2.ColumnDef("active", "BOOLEAN", true)
        ));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-json-source");
        template.setSources(Map.of("people", source));
        template.setTransformers(List.of(sql("""
                SELECT name, score + 1 AS score_next
                FROM people
                WHERE score >= 20
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals("beta", result.getRows().getFirst().getString("name"));
        Assertions.assertEquals("21", result.getRows().getFirst().getString("score_next"));
    }

    @Test
    void readsSingleJsonObjectAsOneRow() throws Exception {
        Path json = Files.createTempFile("template-v2-single-source", ".json");
        Files.writeString(json, """
                {"name":"single","score":7}
                """);
        JsonSourceVO source = new JsonSourceVO();
        source.setPath(json.toString());

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-json-single-source");
        template.setSources(Map.of("person", source));
        template.setTransformers(List.of(sql("SELECT name, score + 3 AS score_next FROM person")));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals("single", result.getRows().getFirst().getString("name"));
        Assertions.assertEquals("10", result.getRows().getFirst().getString("score_next"));
    }

    @Test
    void readsJsonSourceFromRootSelector() throws Exception {
        Path json = Files.createTempFile("template-v2-root-source", ".json");
        Files.writeString(json, """
                {
                  "payload": {
                    "people": [
                      {"name":"alpha","score":10},
                      {"name":"beta","score":20}
                    ]
                  }
                }
                """);
        JsonSourceVO source = new JsonSourceVO();
        source.setPath(json.toString());
        source.setRoot("payload.people");

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-json-root-source");
        template.setSources(Map.of("people", source));
        template.setTransformers(List.of(sql("SELECT name, score + 1 AS score_next FROM people WHERE score >= 20")));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals("beta", result.getRows().getFirst().getString("name"));
        Assertions.assertEquals("21", result.getRows().getFirst().getString("score_next"));
    }

    @Test
    void readsJsonSourceThroughInjectedParser() throws Exception {
        Path json = Files.createTempFile("template-v2-json-custom-parser", ".json");
        Files.writeString(json, "{}");
        JsonSourceVO source = new JsonSourceVO();
        source.setPath(json.toString());

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-json-parser");
        template.setSources(Map.of("people", source));
        template.setTransformers(List.of(sql("SELECT name, score + 1 AS score_next FROM people")));
        template.setSinks(List.of(consoleSink()));

        JsonParser parser = (jsonSource, content) -> List.of(Map.of("name", "gamma", "score", 30));
        TemplateV2RuntimeRegistry runtimeRegistry = new TemplateV2RuntimeRegistry(
                List.of(new JsonSourceFactory(parser)),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory())
        );
        TemplateV2RunResult result = new TemplateV2Runner(runtimeRegistry).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals("gamma", result.getRows().getFirst().getString("name"));
        Assertions.assertEquals("31", result.getRows().getFirst().getString("score_next"));
    }

    @Test
    void readsExcelSourceThroughSqlTransform() throws Exception {
        Path excel = Files.createTempFile("template-v2-source", ".xlsx");
        writeSheet(excel, "Sheet1", List.of(
                excelRow("name", "score"),
                excelRow("alpha", "10"),
                excelRow("beta", "20")
        ));

        ExcelSheetSourceVO sheet = new ExcelSheetSourceVO("Sheet1");
        sheet.setStartRow(1);
        sheet.setEndRow(3);

        ExcelSourceVO source = new ExcelSourceVO();
        source.setPath(excel.toString());
        source.setSheets(List.of(sheet));
        source.setSchema(schema(
                new org.gensokyo.data.model.v2.ColumnDef("name", "VARCHAR", false),
                new org.gensokyo.data.model.v2.ColumnDef("score", "BIGINT", false)
        ));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-excel-source");
        template.setSources(Map.of("people", source));
        template.setTransformers(List.of(sql("""
                SELECT name, score + 1 AS score_next
                FROM people
                WHERE score >= 20
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals("beta", result.getRows().getFirst().getString("name"));
        Assertions.assertEquals("21", result.getRows().getFirst().getString("score_next"));
    }

    @Test
    void writesCsvSinkFromTransformedRows() throws Exception {
        Path csv = Files.createTempFile("template-v2-sink", ".csv");
        WriterVO writer = new WriterVO();
        writer.setType("CSV");
        writer.setTarget(csv.toString());

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-csv-sink");
        template.setSources(Map.of("seed", numberSource(1, 2, 1)));
        template.setTransformers(List.of(sql("SELECT value, value + 10 AS shifted FROM seed")));
        template.setSinks(List.of(sink));

        new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(List.of("value,shifted", "1,11", "2,12"), Files.readAllLines(csv));
    }

    @Test
    void writesJsonSinkFromTransformedRows() throws Exception {
        Path json = Files.createTempFile("template-v2-sink", ".json");
        WriterVO writer = new WriterVO();
        writer.setType("JSON");
        writer.setTarget(json.toString());

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-json-sink");
        template.setSources(Map.of("seed", numberSource(1, 2, 1)));
        template.setTransformers(List.of(sql("SELECT value, value + 10 AS shifted FROM seed")));
        template.setSinks(List.of(sink));

        new TemplateV2Runner(defaultRegistry()).run(template);

        String content = Files.readString(json);
        Assertions.assertTrue(content.contains("\"value\":1"));
        Assertions.assertTrue(content.contains("\"shifted\":11"));
        Assertions.assertTrue(content.startsWith("["));
        Assertions.assertTrue(content.endsWith("]"));
    }

    @Test
    void writesExcelSinkFromTransformedRows() throws Exception {
        Path excel = Files.createTempFile("template-v2-sink", ".xlsx");
        WriterVO writer = new WriterVO();
        writer.setType("EXCEL");
        writer.setTarget(excel.toString());
        writer.setOptions(Map.of("name", "Output"));

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-excel-sink");
        template.setSources(Map.of("seed", numberSource(1, 2, 1)));
        template.setTransformers(List.of(sql("SELECT value, value + 10 AS shifted FROM seed")));
        template.setSinks(List.of(sink));

        new TemplateV2Runner(defaultRegistry()).run(template);

        try (var input = Files.newInputStream(excel); var workbook = new XSSFWorkbook(input)) {
            var sheet = workbook.getSheet("Output");
            Assertions.assertNotNull(sheet);
            Assertions.assertEquals("value", sheet.getRow(0).getCell(0).getStringCellValue());
            Assertions.assertEquals("shifted", sheet.getRow(0).getCell(1).getStringCellValue());
            Assertions.assertEquals(1, (int) sheet.getRow(1).getCell(0).getNumericCellValue());
            Assertions.assertEquals(11, (int) sheet.getRow(1).getCell(1).getNumericCellValue());
            Assertions.assertEquals(2, (int) sheet.getRow(2).getCell(0).getNumericCellValue());
            Assertions.assertEquals(12, (int) sheet.getRow(2).getCell(1).getNumericCellValue());
        }
    }

    @Test
    void resolvesTransformAndSinkThroughRuntimeRegistry() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("calcite_runner_registry"));
        jdbcTemplate.getJdbcTemplate().execute("create table sink_output_registry(source_value bigint)");

        RegistryOnlyWriterVO writer = new RegistryOnlyWriterVO();
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-registry");
        template.setSources(Map.of("seed", numberSource(1, 2, 1)));
        template.setTransformers(List.of(new RegistryOnlyTransformVO()));
        template.setSinks(List.of(sink));

        TemplateV2RuntimeRegistry registry = new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
                List.of(new RegistryOnlyTransformFactory()),
                List.of(new RegistryOnlySinkFactory(jdbcTemplate))
        );

        new TemplateV2Runner(registry).run(template);

        List<Map<String, Object>> rows = jdbcTemplate.getJdbcTemplate()
                .queryForList("select source_value from sink_output_registry order by source_value");
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals("1", rows.get(0).get("SOURCE_VALUE").toString());
        Assertions.assertEquals("2", rows.get(1).get("SOURCE_VALUE").toString());
    }

    @Test
    void writesRowsIntoJdbcSink() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("calcite_runner_sink"));
        jdbcTemplate.getJdbcTemplate().execute("create table sink_output(source_value bigint, shifted_value bigint)");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("sink_output");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-jdbc-sink");
        template.setSources(Map.of("seed", numberSource(1, 3, 1)));
        template.setTransformers(List.of(sql("SELECT value AS source_value, value + 10 AS shifted_value FROM seed WHERE value >= 2")));
        template.setSinks(List.of(sink));

        TemplateV2RunResult result = new TemplateV2Runner(jdbcRegistry(jdbcTemplate)).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        List<Map<String, Object>> rows = jdbcTemplate.getJdbcTemplate()
                .queryForList("select source_value, shifted_value from sink_output order by source_value");
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals("2", rows.get(0).get("SOURCE_VALUE").toString());
        Assertions.assertEquals("12", rows.get(0).get("SHIFTED_VALUE").toString());
        Assertions.assertEquals("3", rows.get(1).get("SOURCE_VALUE").toString());
        Assertions.assertEquals("13", rows.get(1).get("SHIFTED_VALUE").toString());
    }

    @Test
    void writesRowsIntoJdbcSinkUsingTemplateMapping() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("calcite_runner_sink_template"));
        jdbcTemplate.getJdbcTemplate().execute("create table sink_output_mapped(col_a bigint, col_b bigint)");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("sink_output_mapped");
        writer.setTemplate("col_b:shifted_value,col_a:source_value");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-jdbc-sink-template");
        template.setSources(Map.of("seed", numberSource(1, 3, 1)));
        template.setTransformers(List.of(sql("SELECT value AS source_value, value + 10 AS shifted_value FROM seed WHERE value >= 2")));
        template.setSinks(List.of(sink));

        new TemplateV2Runner(jdbcRegistry(jdbcTemplate)).run(template);

        List<Map<String, Object>> rows = jdbcTemplate.getJdbcTemplate()
                .queryForList("select col_a, col_b from sink_output_mapped order by col_a");
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals("2", rows.get(0).get("COL_A").toString());
        Assertions.assertEquals("12", rows.get(0).get("COL_B").toString());
        Assertions.assertEquals("3", rows.get(1).get("COL_A").toString());
        Assertions.assertEquals("13", rows.get(1).get("COL_B").toString());
    }

    @Test
    void writesRowsIntoJdbcSinkUsingResolvedEndpoint() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("calcite_runner_sink_resolved"));
        jdbcTemplate.getJdbcTemplate().execute("create table sink_output_resolved(source_value bigint)");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("resolved-sink");
        writer.setTarget("sink_output_resolved");
        writer.setTemplate("source_value:value");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-jdbc-sink-resolved");
        template.setSources(Map.of("seed", numberSource(1, 2, 1)));
        template.setTransformers(List.of(sql("SELECT value FROM seed")));
        template.setSinks(List.of(sink));

        RuntimeJdbcEndpointResolver resolver = new RuntimeJdbcEndpointResolver() {
            @Override
            public String resolveSourceDataSourceId(org.gensokyo.data.model.v2.QuerySourceVO source) {
                return source.getDataSourceId();
            }

            @Override
            public String resolveSinkDataSourceId(JdbcWriterVO ignored) {
                return "resolved-sink";
            }
        };

        TemplateV2RuntimeRegistry registry = new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory(), new JdbcSinkFactory(jdbcTemplate, resolver))
        );

        new TemplateV2Runner(registry).run(template);

        List<Map<String, Object>> rows = jdbcTemplate.getJdbcTemplate()
                .queryForList("select source_value from sink_output_resolved order by source_value");
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals("1", rows.get(0).get("SOURCE_VALUE").toString());
        Assertions.assertEquals("2", rows.get(1).get("SOURCE_VALUE").toString());
    }

    @Test
    void failsFastWhenSinkExecutionPolicyIsDefault() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("calcite_runner_sink_fail_fast"));
        jdbcTemplate.getJdbcTemplate().execute("create table sink_output_fail_fast(source_value bigint)");

        WriteStageVO failingSink = new WriteStageVO();
        failingSink.setWriters(List.of(new FailingWriterVO()));

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("sink_output_fail_fast");
        writer.setTemplate("source_value:value");

        WriteStageVO jdbcSink = new WriteStageVO();
        jdbcSink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-sink-fail-fast");
        template.setSources(Map.of("seed", numberSource(1, 2, 1)));
        template.setTransformers(List.of(sql("SELECT value FROM seed")));
        template.setSinks(List.of(failingSink, jdbcSink));

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> new PolicyAwareTemplateV2Runner(jdbcRegistry(jdbcTemplate)).run(template));
        Assertions.assertTrue(exception.getMessage().contains("sink index [0]"));
        Assertions.assertTrue(exception.getMessage().contains("writer index [0]"));
        Assertions.assertTrue(exception.getMessage().contains("type [FAILING]"));
        Assertions.assertTrue(exception.getMessage().contains("target [failing_target]"));
        Assertions.assertEquals("Intentional sink failure", exception.getCause().getMessage());

        Integer count = jdbcTemplate.getJdbcTemplate().queryForObject("select count(*) from sink_output_fail_fast", Integer.class);
        Assertions.assertEquals(0, count);
    }

    @Test
    void continuesOnErrorWhenSinkExecutionPolicyAllowsIt() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("calcite_runner_sink_continue"));
        jdbcTemplate.getJdbcTemplate().execute("create table sink_output_continue(source_value bigint)");

        WriteStageVO failingSink = new WriteStageVO();
        failingSink.setWriters(List.of(new FailingWriterVO()));

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("sink_output_continue");
        writer.setTemplate("source_value:value");

        WriteStageVO jdbcSink = new WriteStageVO();
        jdbcSink.setWriters(List.of(writer));

        SinkExecutionPolicyVO policy = new SinkExecutionPolicyVO();
        policy.setMode("CONTINUE_ON_ERROR");

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-sink-continue");
        template.setSinkExecutionPolicy(policy);
        template.setSources(Map.of("seed", numberSource(1, 2, 1)));
        template.setTransformers(List.of(sql("SELECT value FROM seed")));
        template.setSinks(List.of(failingSink, jdbcSink));

        new PolicyAwareTemplateV2Runner(jdbcRegistry(jdbcTemplate)).run(template);

        Integer count = jdbcTemplate.getJdbcTemplate().queryForObject("select count(*) from sink_output_continue", Integer.class);
        Assertions.assertEquals(2, count);
    }

    private IteratorSourceVO numberSource(long from, long to, int step) {
        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(from);
        iterator.setTo(to);
        iterator.setStep(step);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);
        return source;
    }

    private SqlTransformVO sql(String sql) {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql(sql);
        return transform;
    }

    private WriteStageVO consoleSink() {
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));
        return sink;
    }

    private RowSchema schema(org.gensokyo.data.model.v2.ColumnDef... columns) {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(columns));
        return schema;
    }

    private DriverManagerDataSource dataSource(String name) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + name + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private TemplateV2RuntimeRegistry defaultRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory(), new CsvSourceFactory(), new ExcelSourceFactory(), new JsonSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory(), new CsvSinkFactory(), new ExcelSinkFactory(), new JsonSinkFactory())
        );
    }

    private void writeSheet(Path excel, String sheetName, List<Map<String, String>> rows) throws Exception {
        XSSFWorkbook workbook;
        if (Files.exists(excel) && Files.size(excel) > 0) {
            try (var input = Files.newInputStream(excel)) {
                workbook = new XSSFWorkbook(input);
            }
        } else {
            workbook = new XSSFWorkbook();
        }
        try (workbook) {
            var existing = workbook.getSheet(sheetName);
            if (existing != null) {
                workbook.removeSheetAt(workbook.getSheetIndex(existing));
            }
            var sheet = workbook.createSheet(sheetName);
            for (int i = 0; i < rows.size(); i++) {
                var row = sheet.createRow(i);
                int cellIndex = 0;
                for (String value : rows.get(i).values()) {
                    row.createCell(cellIndex++).setCellValue(value);
                }
            }
            try (OutputStream output = Files.newOutputStream(excel)) {
                workbook.write(output);
            }
        }
    }

    private Map<String, String> excelRow(String first, String second) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("c1", first);
        row.put("c2", second);
        return row;
    }

    private TemplateV2RuntimeRegistry jdbcRegistry(NamedParameterJdbcTemplate jdbcTemplate) {
        return new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory(),
                        new JdbcSinkFactory(jdbcTemplate, new NoopRuntimeJdbcEndpointResolver()))
        );
    }

    private TemplateV2RuntimeRegistry nullableRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new RegistryOnlySourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory())
        );
    }

    private TemplateV2RuntimeRegistry dateRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new DateSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory())
        );
    }

    private TemplateV2RuntimeRegistry legacyTimeRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new LegacyCompactTimeSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory())
        );
    }

    private TemplateV2RuntimeRegistry vehicleRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new VehicleSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory())
        );
    }

    private TemplateV2RuntimeRegistry aggregateRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new AggregateSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory())
        );
    }

    private static final class FailingWriterVO extends WriterVO {
        private FailingWriterVO() {
            setType("FAILING");
            setTarget("failing_target");
        }
    }

    private static final class RegistryOnlyTransformVO extends TransformVO {
        private RegistryOnlyTransformVO() {
            setType("REGISTRY_ONLY");
        }
    }

    private static final class RegistryOnlyWriterVO extends WriterVO {
        private RegistryOnlyWriterVO() {
            setType("REGISTRY_ONLY_SINK");
        }
    }

    private static final class RegistryOnlySourceVO extends org.gensokyo.data.model.v2.SourceVO {
        private RegistryOnlySourceVO() {
            setType("REGISTRY_ONLY_SOURCE");
        }
    }

    private static final class DateSourceVO extends org.gensokyo.data.model.v2.SourceVO {
        private DateSourceVO() {
            setType("DATE_SOURCE");
        }
    }

    private static final class LegacyCompactTimeSourceVO extends org.gensokyo.data.model.v2.SourceVO {
        private LegacyCompactTimeSourceVO() {
            setType("LEGACY_TIME_SOURCE");
        }
    }

    private static final class VehicleSourceVO extends org.gensokyo.data.model.v2.SourceVO {
        private VehicleSourceVO() {
            setType("VEHICLE_SOURCE");
        }
    }

    private static final class AggregateSourceVO extends org.gensokyo.data.model.v2.SourceVO {
        private AggregateSourceVO() {
            setType("AGGREGATE_SOURCE");
        }
    }

    private static final class FailingRowSinkAdapter implements RowSink {
        @Override
        public void write(RowSchema schema, List<Row> rows) {
            throw new IllegalStateException("Intentional sink failure");
        }
    }

    private static final class PolicyAwareTemplateV2Runner extends TemplateV2Runner {
        private PolicyAwareTemplateV2Runner(TemplateV2RuntimeRegistry runtimeRegistry) {
            super(runtimeRegistry);
        }

        @Override
        protected RowSink createSink(TemplateV2RuntimeRegistry runtimeRegistry, WriterVO writer) {
            if (writer instanceof FailingWriterVO) {
                return new FailingRowSinkAdapter();
            }
            return super.createSink(runtimeRegistry, writer);
        }
    }

    private static final class RegistryOnlyTransformFactory implements V2TransformFactory {
        @Override
        public boolean supports(TransformVO transform) {
            return transform instanceof RegistryOnlyTransformVO;
        }

        @Override
        public CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context) {
            return new CalciteRowTransformer("SELECT value AS source_value FROM seed").transform(context);
        }
    }

    private static final class RegistryOnlySourceFactory implements V2SourceFactory {
        @Override
        public boolean supports(org.gensokyo.data.model.v2.SourceVO source) {
            return source instanceof RegistryOnlySourceVO;
        }

        @Override
        public RowSource create(String name, org.gensokyo.data.model.v2.SourceVO source) {
            return new RowSource() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public RowSchema schema() {
                    RowSchema schema = new RowSchema();
                    schema.setColumns(List.of(
                            new org.gensokyo.data.model.v2.ColumnDef("name", "VARCHAR", false),
                            new org.gensokyo.data.model.v2.ColumnDef("score", "BIGINT", true)
                    ));
                    return schema;
                }

                @Override
                public List<Row> rows() {
                    return List.of(
                            new Row(row("name", "empty", "score", null)),
                            new Row(row("name", "alpha", "score", 10)),
                            new Row(row("name", "beta", "score", 20))
                    );
                }
            };
        }

        private Map<String, Object> row(String firstKey, Object firstValue, String secondKey, Object secondValue) {
            Map<String, Object> values = new java.util.LinkedHashMap<>();
            values.put(firstKey, firstValue);
            values.put(secondKey, secondValue);
            return values;
        }
    }

    private static final class DateSourceFactory implements V2SourceFactory {
        @Override
        public boolean supports(org.gensokyo.data.model.v2.SourceVO source) {
            return source instanceof DateSourceVO;
        }

        @Override
        public RowSource create(String name, org.gensokyo.data.model.v2.SourceVO source) {
            return new RowSource() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public RowSchema schema() {
                    RowSchema schema = new RowSchema();
                    schema.setColumns(List.of(
                            new org.gensokyo.data.model.v2.ColumnDef("event_date", "DATE", false),
                            new org.gensokyo.data.model.v2.ColumnDef("base_date", "DATE", false),
                            new org.gensokyo.data.model.v2.ColumnDef("event_time", "VARCHAR", false)
                    ));
                    return schema;
                }

                @Override
                public List<Row> rows() {
                    return List.of(new Row(Map.of(
                            "event_date", java.time.LocalDate.parse("2026-05-02"),
                            "base_date", java.time.LocalDate.parse("2026-05-01"),
                            "event_time", "2026-05-02 10:20:30"
                    )));
                }
            };
        }
    }

    private static final class LegacyCompactTimeSourceFactory implements V2SourceFactory {
        @Override
        public boolean supports(org.gensokyo.data.model.v2.SourceVO source) {
            return source instanceof LegacyCompactTimeSourceVO;
        }

        @Override
        public RowSource create(String name, org.gensokyo.data.model.v2.SourceVO source) {
            return new RowSource() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public RowSchema schema() {
                    RowSchema schema = new RowSchema();
                    schema.setColumns(List.of(
                            new org.gensokyo.data.model.v2.ColumnDef("raw_time", "VARCHAR", false)
                    ));
                    return schema;
                }

                @Override
                public List<Row> rows() {
                    return List.of(new Row(Map.of("raw_time", "260502102030")));
                }
            };
        }
    }

    private static final class VehicleSourceFactory implements V2SourceFactory {
        @Override
        public boolean supports(org.gensokyo.data.model.v2.SourceVO source) {
            return source instanceof VehicleSourceVO;
        }

        @Override
        public RowSource create(String name, org.gensokyo.data.model.v2.SourceVO source) {
            return new RowSource() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public RowSchema schema() {
                    RowSchema schema = new RowSchema();
                    schema.setColumns(List.of(
                            new org.gensokyo.data.model.v2.ColumnDef("plate", "VARCHAR", false),
                            new org.gensokyo.data.model.v2.ColumnDef("pass_time", "VARCHAR", false),
                            new org.gensokyo.data.model.v2.ColumnDef("compact_time", "VARCHAR", false),
                            new org.gensokyo.data.model.v2.ColumnDef("device_id", "VARCHAR", false)
                    ));
                    return schema;
                }

                @Override
                public List<Row> rows() {
                    return List.of(new Row(Map.of(
                            "plate", "\u7ca4A12345",
                            "pass_time", "2026-05-02 10:20:30",
                            "compact_time", "260502102030",
                            "device_id", "440100000011"
                    )));
                }
            };
        }
    }

    private static final class AggregateSourceFactory implements V2SourceFactory {
        @Override
        public boolean supports(org.gensokyo.data.model.v2.SourceVO source) {
            return source instanceof AggregateSourceVO;
        }

        @Override
        public RowSource create(String name, org.gensokyo.data.model.v2.SourceVO source) {
            return new RowSource() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public RowSchema schema() {
                    RowSchema schema = new RowSchema();
                    schema.setColumns(List.of(
                            new org.gensokyo.data.model.v2.ColumnDef("category", "VARCHAR", false),
                            new org.gensokyo.data.model.v2.ColumnDef("amount", "BIGINT", true)
                    ));
                    return schema;
                }

                @Override
                public List<Row> rows() {
                    return List.of(
                            new Row(Map.of("category", "a", "amount", 10)),
                            new Row(Map.of("category", "a", "amount", 20)),
                            new Row(Map.of("category", "b", "amount", 5))
                    );
                }
            };
        }
    }

    private static final class RegistryOnlySinkFactory implements V2SinkFactory {
        private final NamedParameterJdbcTemplate jdbcTemplate;

        private RegistryOnlySinkFactory(NamedParameterJdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public boolean supports(WriterVO writer) {
            return writer instanceof RegistryOnlyWriterVO;
        }

        @Override
        public RowSink create(WriterVO writer) {
            return (schema, rows) -> {
                Map<String, ?>[] batch = rows.stream()
                        .map(row -> Map.of("source_value", row.get("source_value")))
                        .toArray(Map[]::new);
                jdbcTemplate.batchUpdate("insert into sink_output_registry(source_value) values(:source_value)", batch);
            };
        }
    }
}
