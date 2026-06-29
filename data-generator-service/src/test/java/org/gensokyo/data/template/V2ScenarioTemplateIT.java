/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.runtime.SinkWriteMetric;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.JsonSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates and executes greenfield Template V2 scenario YAML shipped under {@code template/v2-scenarios/}.
 *
 * @author Gensokyo
 * @version 1.0.0
 * @since 2026-05-29
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class V2ScenarioTemplateIT {

    private static final String SCENARIO_ROOT = "template/v2-scenarios/";

    /** Row count for Phase 8 streaming CSV/NDJSON scenarios (fast IT). */
    private static final int STREAMING_FIXTURE_ROWS = 120;

    /** Row count for Phase 8 upsert scenarios (matches sourceChunkSize for even chunks). */
    private static final int UPSERT_FIXTURE_ROWS = 40;

    /** OOM fixture bar for plan 08-09 ({@value #LARGE_STREAMING_CSV_TARGET_BYTES} bytes). */
    private static final long LARGE_STREAMING_CSV_TARGET_BYTES = 10L * 1024L * 1024L;

    @Autowired
    private TemplateV2Runner templateV2Runner;

    @Autowired
    private RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * Scenario fixtures under {@code template/v2-scenarios/}.
     *
     * @return relative classpath paths for each greenfield scenario YAML
     */
    static Stream<String> scenarioResources() {
        return Stream.of(
                "scenario-a-synthetic.yaml",
                "scenario-b-lookup-join.yaml",
                "scenario-c-csv-export.yaml",
                "scenario-d-chunked-jdbc.yaml",
                "scenario-e-streaming-jdbc.yaml",
                "scenario-e-partial-sink.yaml",
                "scenario-f-streaming-csv.yaml",
                "scenario-f-streaming-ndjson.yaml",
                "scenario-g-upsert-pg.yaml",
                "scenario-g-upsert-mysql.yaml",
                "scenario-ai-inline.yaml",
                "scenario-inline-rows.yaml"
        ).map(name -> SCENARIO_ROOT + name);
    }

    /**
     * Loads each scenario from the classpath, validates structure, prepares H2 fixtures, and runs end to end.
     *
     * @param resourcePath classpath path to scenario YAML
     * @throws Exception when fixture setup or execution fails
     */
    @ParameterizedTest
    @MethodSource("scenarioResources")
    void validatesAndRunsGreenfieldScenario(String resourcePath) throws Exception {
        TemplateV2VO template = loadNormalizedTemplate(resourcePath);
        // H2 lacks PostgreSQL ON CONFLICT; skip PG upsert scenario until Testcontainers proof (08-09, W-01).
        if ("scenario-g-upsert-pg".equals(template.getName())) {
            Assumptions.assumeTrue(
                    h2SupportsPostgresUpsert(),
                    "H2 does not implement PostgreSQL ON CONFLICT; see ChunkedPipelinePostgresUpsertTests (08-09)");
        }
        prepareScenarioFixtures(template);
        TemplateV2Validator.validate(template);

        TemplateV2RunResult result = templateV2Runner.run(template);

        assertScenarioOutcome(resourcePath, template, result);
    }

    private TemplateV2VO loadNormalizedTemplate(String resourcePath) throws IOException {
        String yaml = readClasspath(resourcePath);
        TemplateV2DraftVO draft = new JacksonParser().parse(yaml, TemplateV2DraftVO.class);
        TemplateV2VO template = TemplateV2Normalizer.normalize(draft);
        assertThat(template).isNotNull();
        assertThat(template.getName()).isNotBlank();
        return template;
    }

    private void prepareScenarioFixtures(TemplateV2VO template) throws IOException {
        switch (template.getName()) {
            case "scenario-a-synthetic" -> {
                // Iterator-only scenario needs no JDBC seed data.
            }
            case "scenario-inline-rows" -> {
                // Inline rows are embedded in the template YAML.
            }
            case "scenario-b-lookup-join" -> {
                registerInlineSources(template);
                execOn("gf-b-orders",
                        "create table gf_orders(id bigint, customer_id bigint)",
                        "insert into gf_orders(id, customer_id) values (1, 10), (2, 20), (3, 30)");
                execOn("gf-b-customers",
                        "create table gf_customers(id bigint, name varchar(64))",
                        "insert into gf_customers(id, name) values (10, 'Ada'), (20, 'Bob')");
            }
            case "scenario-c-csv-export" -> {
                CsvSourceVO csvSource = (CsvSourceVO) template.getSources().get("incoming");
                csvSource.setPath(materializeCsvFixture().toString());
                registerInlineSinks(template);
                execOn("gf-c-target", "create table exported_orders(order_id bigint, customer varchar(64), amount decimal(10,2))");
            }
            case "scenario-d-chunked-jdbc" -> {
                registerInlineSources(template);
                registerInlineSinks(template);
                execOn("gf-d-source",
                        "create table gf_ledger(id bigint, label varchar(32))",
                        seedLedgerRows("gf_ledger", 120));
                execOn("gf-d-target", "create table gf_ledger_export(id bigint, label varchar(32))");
            }
            case "scenario-e-streaming-jdbc" -> {
                registerInlineSources(template);
                registerInlineSinks(template);
                execOn("gf-e-source",
                        "create table gf_ledger(id bigint, label varchar(32))",
                        seedLedgerRows("gf_ledger", 120));
                execOn("gf-e-target", "create table gf_ledger_export(id bigint, label varchar(32))");
            }
            case "scenario-e-partial-sink" -> {
                // Intentionally omit __missing_sink_target__ so the first JDBC sink fails.
                registerInlineSinks(template);
            }
            case "scenario-f-streaming-csv" -> {
                CsvSourceVO csvSource = (CsvSourceVO) template.getSources().get("incoming");
                csvSource.setPath(materializeClasspathFixture("streaming-orders.csv").toString());
                // Generator hook for 08-09 OOM proof (≥10 MB); moderate fixture used for this IT.
                materializeLargeStreamingCsvFixture();
            }
            case "scenario-f-streaming-ndjson" -> {
                JsonSourceVO jsonSource = (JsonSourceVO) template.getSources().get("events");
                jsonSource.setPath(materializeClasspathFixture("streaming-events.ndjson").toString());
            }
            case "scenario-g-upsert-pg" -> {
                registerInlineSources(template);
                registerInlineSinks(template);
                execOn("gf-g-upsert-pg-source",
                        "create table gf_upsert_source(id bigint primary key, name varchar(64))",
                        seedUpsertSourceRows(UPSERT_FIXTURE_ROWS, "n"));
                execOn("gf-g-upsert-pg-target",
                        "create table gf_upsert_target(id bigint primary key, name varchar(64))");
            }
            case "scenario-g-upsert-mysql" -> {
                registerInlineSources(template);
                registerInlineSinks(template);
                execOn("gf-g-upsert-mysql-source",
                        "create table gf_upsert_source(id bigint primary key, name varchar(64))",
                        seedUpsertSourceRows(UPSERT_FIXTURE_ROWS, "n"));
                execOn("gf-g-upsert-mysql-target",
                        "create table gf_upsert_target(id bigint primary key, name varchar(64))");
            }
            case "scenario-ai-inline" -> {
                // INLINE provider embeds rows in YAML; no external fixtures required.
            }
            default -> throw new IllegalArgumentException("Unknown scenario template: " + template.getName());
        }
    }

    private void assertScenarioOutcome(String resourcePath, TemplateV2VO template, TemplateV2RunResult result) {
        assertThat(result).isNotNull();
        switch (template.getName()) {
            case "scenario-a-synthetic" -> {
                assertThat(result.getRows()).hasSize(3);
                assertThat(result.getRows().getFirst().getString("score")).isEqualTo("10");
            }
            case "scenario-inline-rows" -> {
                assertThat(result.getRows()).hasSize(1);
                assertThat(result.getRows().getFirst().getString("label")).isEqualTo("United States");
            }
            case "scenario-b-lookup-join" -> {
                assertThat(result.getRows()).hasSize(3);
                assertThat(result.getRows().get(2).getString("customer_name")).isNull();
            }
            case "scenario-c-csv-export" -> {
                assertThat(countRows("gf-c-target", "exported_orders")).isEqualTo(3L);
                assertThat(result.getMetrics()).isNotNull();
                assertThat(result.getMetrics().getRowsWritten()).isEqualTo(3L);
            }
            case "scenario-d-chunked-jdbc" -> {
                assertThat(result.getMetrics()).isNotNull();
                assertThat(result.getMetrics().getExecutionMode()).isEqualTo("CHUNKED");
                assertThat(result.getMetrics().getRowsWritten()).isEqualTo(120L);
                assertThat(countRows("gf-d-target", "gf_ledger_export")).isEqualTo(120L);
            }
            case "scenario-e-streaming-jdbc" -> {
                assertThat(result.getMetrics()).isNotNull();
                assertThat(result.getMetrics().getExecutionMode()).isEqualTo("STREAMING");
                assertThat(result.getMetrics().getPeakRowsInMemory()).isGreaterThan(0);
                assertThat(result.getMetrics().getRowsWritten()).isEqualTo(120L);
                assertThat(countRows("gf-e-target", "gf_ledger_export")).isEqualTo(120L);
            }
            case "scenario-e-partial-sink" -> {
                assertThat(result.getMetrics()).isNotNull();
                Map<String, SinkWriteMetric> sinkMetrics = result.getMetrics().getSinkMetrics();
                SinkWriteMetric failingWriter = sinkMetrics.get("sink[0].writer[0]");
                SinkWriteMetric okWriter = sinkMetrics.get("sink[1].writer[0]");
                assertThat(failingWriter).isNotNull();
                assertThat(okWriter).isNotNull();
                assertThat(failingWriter.getRowsFailed()).isGreaterThan(0L);
                assertThat(failingWriter.getRowsOk()).isZero();
                assertThat(failingWriter.getLastErrorSample()).isNotBlank();
                // Phase 8 actionable sink errors: writer key, type, and target in errorSample (W-08, RW-04).
                assertThat(failingWriter.getLastErrorSample()).contains("sink[0].writer[0]");
                assertThat(failingWriter.getLastErrorSample()).containsIgnoringCase("jdbc");
                assertThat(failingWriter.getLastErrorSample()).contains("__missing_sink_target__");
                assertThat(okWriter.getRowsOk()).isEqualTo(3L);
                assertThat(okWriter.getRowsFailed()).isZero();
            }
            case "scenario-f-streaming-csv" -> {
                assertThat(result.getMetrics()).isNotNull();
                assertThat(result.getMetrics().getExecutionMode()).isEqualTo("STREAMING");
                assertThat(result.getMetrics().getPeakRowsInMemory()).isGreaterThan(0);
                assertThat(result.getMetrics().getRowsWritten()).isEqualTo((long) STREAMING_FIXTURE_ROWS);
            }
            case "scenario-f-streaming-ndjson" -> {
                assertThat(result.getMetrics()).isNotNull();
                assertThat(result.getMetrics().getExecutionMode()).isEqualTo("STREAMING");
                assertThat(result.getMetrics().getRowsWritten()).isEqualTo((long) STREAMING_FIXTURE_ROWS);
            }
            case "scenario-g-upsert-pg" -> assertUpsertScenarioOutcome(template, result, "gf-g-upsert-pg-target");
            case "scenario-g-upsert-mysql" -> assertUpsertScenarioOutcome(template, result, "gf-g-upsert-mysql-target");
            case "scenario-ai-inline" -> {
                assertThat(result.getRows()).hasSize(1);
                assertThat(result.getRows().getFirst().getString("name")).isEqualTo("beta");
                assertThat(result.getRows().getFirst().getString("score")).isEqualTo("20");
            }
            default -> throw new IllegalArgumentException("Unknown scenario resource: " + resourcePath);
        }
    }

    private void registerInlineSources(TemplateV2VO template) {
        for (SourceVO source : template.getSources().values()) {
            if (source instanceof QuerySourceVO querySource) {
                String id = runtimeJdbcEndpointResolver.resolveSourceDataSourceId(querySource);
                querySource.setDataSourceId(id);
            }
        }
    }

    private void registerInlineSinks(TemplateV2VO template) {
        if (template.getSinks() == null) {
            return;
        }
        for (var sink : template.getSinks()) {
            if (sink.getWriters() == null) {
                continue;
            }
            for (WriterVO writer : sink.getWriters()) {
                if (writer instanceof JdbcWriterVO jdbcWriter) {
                    String id = runtimeJdbcEndpointResolver.resolveSinkDataSourceId(jdbcWriter);
                    jdbcWriter.setDataSourceId(id);
                }
            }
        }
    }

    private void execOn(String dataSourceId, String... statements) {
        try {
            DynamicDataSourceContextHolder.push(dataSourceId);
            for (String sql : statements) {
                namedParameterJdbcTemplate.getJdbcTemplate().execute(sql);
            }
        }
        finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    private long countRows(String dataSourceId, String table) {
        try {
            DynamicDataSourceContextHolder.push(dataSourceId);
            Long count = namedParameterJdbcTemplate.getJdbcTemplate()
                    .queryForObject("select count(*) from " + table, Long.class);
            return count == null ? 0L : count;
        }
        finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    private static String seedLedgerRows(String table, int rowCount) {
        StringBuilder insert = new StringBuilder("insert into ").append(table).append("(id, label) values ");
        for (int i = 1; i <= rowCount; i++) {
            if (i > 1) {
                insert.append(',');
            }
            insert.append('(').append(i).append(", 'row-").append(i).append("')");
        }
        return insert.toString();
    }

    private static Path materializeCsvFixture() throws IOException {
        return materializeClasspathFixture("orders.csv");
    }

    private static Path materializeClasspathFixture(String fixtureName) throws IOException {
        ClassPathResource resource = new ClassPathResource("template/v2-scenarios/fixtures/" + fixtureName);
        String suffix = fixtureName.contains(".") ? fixtureName.substring(fixtureName.lastIndexOf('.')) : "";
        Path temp = Files.createTempFile("scenario-fixture-", suffix);
        try (InputStream input = resource.getInputStream()) {
            Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        return temp;
    }

    /**
     * Generates a large CSV fixture (≥10 MB) for OOM proof in plan 08-09 (D-06).
     *
     * @return path to the generated temp file
     * @throws IOException when the file cannot be written
     */
    static Path materializeLargeStreamingCsvFixture() throws IOException {
        Path temp = Files.createTempFile("large-streaming-orders-", ".csv");
        StringBuilder content = new StringBuilder("order_id,customer,amount\n");
        int row = 1;
        while (content.length() < LARGE_STREAMING_CSV_TARGET_BYTES) {
            content.append(row)
                    .append(",customer-")
                    .append(row)
                    .append(',')
                    .append(row * 1.25)
                    .append('\n');
            row++;
        }
        Files.writeString(temp, content, StandardCharsets.UTF_8);
        return temp;
    }

    private void assertUpsertScenarioOutcome(
            TemplateV2VO template, TemplateV2RunResult firstRun, String targetDataSourceId) {
        assertThat(firstRun.getMetrics()).isNotNull();
        assertThat(firstRun.getMetrics().getExecutionMode()).isEqualTo("CHUNKED");
        long countAfterFirst = countRows(targetDataSourceId, "gf_upsert_target");
        assertThat(countAfterFirst).isEqualTo((long) UPSERT_FIXTURE_ROWS);
        assertThat(firstRun.getMetrics().getRowsWritten()).isEqualTo((long) UPSERT_FIXTURE_ROWS);

        // Second run with changed non-key values must upsert without duplicate rows (D-15).
        updateUpsertSourceNames(template, "u");
        TemplateV2RunResult secondRun = templateV2Runner.run(template);
        assertThat(secondRun.getMetrics()).isNotNull();
        assertThat(countRows(targetDataSourceId, "gf_upsert_target")).isEqualTo(countAfterFirst);

        SinkWriteMetric sinkMetric = secondRun.getMetrics().getSinkMetrics().get("sink[0].writer[0]");
        assertThat(sinkMetric).isNotNull();
        assertThat(sinkMetric.getRowsUpserted()).isGreaterThan(0L);
    }

    private void updateUpsertSourceNames(TemplateV2VO template, String prefix) {
        String sourceDataSourceId = switch (template.getName()) {
            case "scenario-g-upsert-pg" -> "gf-g-upsert-pg-source";
            case "scenario-g-upsert-mysql" -> "gf-g-upsert-mysql-source";
            default -> throw new IllegalArgumentException("Not an upsert scenario: " + template.getName());
        };
        for (int i = 0; i < UPSERT_FIXTURE_ROWS; i++) {
            try {
                DynamicDataSourceContextHolder.push(sourceDataSourceId);
                namedParameterJdbcTemplate.getJdbcTemplate().update(
                        "update gf_upsert_source set name = ? where id = ?",
                        prefix + i,
                        i);
            }
            finally {
                DynamicDataSourceContextHolder.clear();
            }
        }
    }

    /**
     * Probes whether embedded H2 can execute PostgreSQL {@code ON CONFLICT} upsert SQL (W-01).
     *
     * @return {@code true} when H2 accepts {@code ON CONFLICT DO UPDATE}
     */
    private static boolean h2SupportsPostgresUpsert() {
        try {
            org.springframework.jdbc.datasource.DriverManagerDataSource dataSource =
                    new org.springframework.jdbc.datasource.DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:pg_upsert_probe;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
            jdbc.execute("create table upsert_probe(id int primary key, v int)");
            jdbc.execute("insert into upsert_probe(id, v) values (1, 1)");
            jdbc.execute("insert into upsert_probe(id, v) values (1, 2) on conflict (id) do update set v = 2");
            Integer value = jdbc.queryForObject("select v from upsert_probe where id = 1", Integer.class);
            return value != null && value == 2;
        }
        catch (RuntimeException ex) {
            return false;
        }
    }

    private static String seedUpsertSourceRows(int rowCount, String namePrefix) {
        StringBuilder insert = new StringBuilder("insert into gf_upsert_source(id, name) values ");
        for (int i = 0; i < rowCount; i++) {
            if (i > 0) {
                insert.append(',');
            }
            insert.append('(').append(i).append(", '").append(namePrefix).append(i).append("')");
        }
        return insert.toString();
    }

    private static String readClasspath(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
