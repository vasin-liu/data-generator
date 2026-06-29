/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.sink.ConsoleSinkFactory;
import org.gensokyo.data.calcite.sink.JdbcSinkFactory;
import org.gensokyo.data.calcite.source.CsvSourceFactory;
import org.gensokyo.data.calcite.source.QuerySourceFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Integration tests for {@link ChunkedPipeline} and {@link TemplateV2Runner} chunked mode.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
class ChunkedPipelineTests {

    private static final int ROW_COUNT = 10_000;
    private static final int CSV_ROW_COUNT = 3_000;
    private static final int CSV_CHUNK_SIZE = 1_000;

    @Test
    void chunkedModeWritesCsvSourceInBatches(@TempDir Path tempDir) throws Exception {
        Path csv = writeCsvFixture(tempDir, CSV_ROW_COUNT);

        CsvSourceVO source = new CsvSourceVO();
        source.setPath(csv.toString());
        source.setHeader(true);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT id, name FROM incoming");

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("CHUNKED");
        executionPolicy.setSourceChunkSize(CSV_CHUNK_SIZE);
        executionPolicy.setSinkBatchSize(500);
        executionPolicy.setMaxRowsInMemory(CSV_ROW_COUNT + 1);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("chunked-csv-demo");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("incoming", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RuntimeRegistry registry = new TemplateV2RuntimeRegistry(
                List.of(new CsvSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));

        TemplateV2RunResult result = new TemplateV2Runner(registry).run(template);

        Assertions.assertTrue(result.getRows().isEmpty());
        Assertions.assertNotNull(result.getMetrics());
        Assertions.assertEquals("CHUNKED", result.getMetrics().getExecutionMode());
        Assertions.assertEquals(CSV_ROW_COUNT, result.getMetrics().getTotalRowsRead());
        Assertions.assertEquals(CSV_ROW_COUNT, result.getMetrics().getRowsWritten());
        Assertions.assertEquals(CSV_ROW_COUNT / CSV_CHUNK_SIZE, result.getMetrics().getChunksProcessed());
    }

    @Test
    void chunkedModeWritesAllRowsInBatches() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        jdbcTemplate.getJdbcTemplate().execute("create table source_t(id bigint, name varchar(20))");
        jdbcTemplate.getJdbcTemplate().execute("create table target_t(id bigint, name varchar(20))");
        for (int batch = 0; batch < ROW_COUNT; batch += 1_000) {
            StringBuilder insert = new StringBuilder("insert into source_t(id, name) values ");
            for (int i = batch; i < Math.min(batch + 1_000, ROW_COUNT); i++) {
                if (i > batch) {
                    insert.append(',');
                }
                insert.append('(').append(i).append(", 'n").append(i).append("')");
            }
            jdbcTemplate.getJdbcTemplate().execute(insert.toString());
        }

        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql("select id, name from source_t order by id");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select id, name from t");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("target_t");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("CHUNKED");
        executionPolicy.setSourceChunkSize(2_000);
        executionPolicy.setSinkBatchSize(500);
        executionPolicy.setMaxRowsInMemory(ROW_COUNT + 1);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("chunked-pipeline-demo");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("t", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));

        TemplateV2RuntimeRegistry registry = new TemplateV2RuntimeRegistry(
                List.of(new QuerySourceFactory(jdbcTemplate)),
                List.of(new SqlTransformFactory()),
                List.of(new JdbcSinkFactory(jdbcTemplate, new NoopRuntimeJdbcEndpointResolver())));

        TemplateV2RunResult result = new TemplateV2Runner(registry).run(template);

        Assertions.assertTrue(result.getRows().isEmpty());
        Assertions.assertNotNull(result.getMetrics());
        Assertions.assertEquals("CHUNKED", result.getMetrics().getExecutionMode());
        Assertions.assertEquals(ROW_COUNT, result.getMetrics().getTotalRowsRead());
        Assertions.assertEquals(ROW_COUNT, countRows(jdbcTemplate, "target_t"));
        Assertions.assertEquals(ROW_COUNT / 2_000, result.getMetrics().getChunksProcessed());
    }

    private static WriteStageVO consoleSink() {
        ConsoleWriterVO writer = new ConsoleWriterVO();
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));
        return sink;
    }

    private static Path writeCsvFixture(Path tempDir, int rowCount) throws Exception {
        Path csv = tempDir.resolve("chunked.csv");
        List<String> lines = new ArrayList<>();
        lines.add("id,name");
        for (int i = 0; i < rowCount; i++) {
            lines.add(i + ",n" + i);
        }
        Files.writeString(csv, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return csv;
    }

    private static long countRows(NamedParameterJdbcTemplate jdbcTemplate, String table) {
        Long count = jdbcTemplate.getJdbcTemplate().queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0L : count;
    }

    private DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:calcite_chunked_pipeline;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
