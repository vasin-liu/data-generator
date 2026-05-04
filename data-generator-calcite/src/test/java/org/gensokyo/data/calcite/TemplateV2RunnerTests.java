package org.gensokyo.data.calcite;

import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.CsvSourceVO;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
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
                List.of(new IteratorSourceFactory(), new CsvSourceFactory(), new JsonSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory(), new CsvSinkFactory(), new JsonSinkFactory())
        );
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
                            new org.gensokyo.data.model.v2.ColumnDef("base_date", "DATE", false)
                    ));
                    return schema;
                }

                @Override
                public List<Row> rows() {
                    return List.of(new Row(Map.of(
                            "event_date", java.time.LocalDate.parse("2026-05-02"),
                            "base_date", java.time.LocalDate.parse("2026-05-01")
                    )));
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
