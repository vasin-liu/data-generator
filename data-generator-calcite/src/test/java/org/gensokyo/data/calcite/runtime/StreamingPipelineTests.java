/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.parser.DefaultCsvParser;
import org.gensokyo.data.calcite.sink.ConsoleSinkFactory;
import org.gensokyo.data.calcite.sink.CsvSinkFactory;
import org.gensokyo.data.calcite.sink.JdbcSinkFactory;
import org.gensokyo.data.calcite.sink.JsonSinkFactory;
import org.gensokyo.data.calcite.source.CsvRowSource;
import org.gensokyo.data.calcite.source.CsvSourceFactory;
import org.gensokyo.data.calcite.source.JsonSourceFactory;
import org.gensokyo.data.calcite.source.QuerySourceFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.JsonSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
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
 * Integration tests for {@link StreamingPipeline} and {@link TemplateV2Runner} streaming mode.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
class StreamingPipelineTests {

    private static final int ROW_COUNT = 500;
    private static final int CSV_ROW_COUNT = 2_500;
    private static final int CSV_TO_SINK_ROW_COUNT = 3_000;
    private static final int CSV_TO_SINK_CHUNK_SIZE = 500;
    private static final int DEFAULT_FILE_CHUNK_SIZE = EffectiveExecutionPolicy.DEFAULT_FILE_SOURCE_CHUNK_SIZE;

    @Test
    void streamsCsvSourceInBatches(@TempDir Path tempDir) throws Exception {
        int chunkSize = 100;
        Path csv = writeCsvFixture(tempDir, ROW_COUNT);

        CsvSourceVO source = new CsvSourceVO();
        source.setPath(csv.toString());
        source.setHeader(true);

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("STREAMING");
        executionPolicy.setSourceChunkSize(chunkSize);
        executionPolicy.setSinkBatchSize(chunkSize);
        executionPolicy.setMaxRowsInMemory(ROW_COUNT + 1);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("streaming-csv-batches");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("incoming", source));
        template.setTransformers(List.of(passthroughCsvTransform()));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(fileSourceRegistry()).run(template);

        Assertions.assertTrue(result.getRows().isEmpty());
        Assertions.assertEquals("STREAMING", result.getMetrics().getExecutionMode());
        Assertions.assertEquals(ROW_COUNT, result.getMetrics().getTotalRowsRead());
        Assertions.assertEquals(ROW_COUNT, result.getMetrics().getRowsWritten());
        Assertions.assertTrue(result.getMetrics().getPeakRowsInMemory() <= chunkSize);
        Assertions.assertEquals(ROW_COUNT / chunkSize, result.getMetrics().getChunksProcessed());
    }

    @Test
    void streamsNdjsonSourceInBatches(@TempDir Path tempDir) throws Exception {
        int chunkSize = 100;
        Path ndjson = writeNdjsonFixture(tempDir, ROW_COUNT);

        JsonSourceVO source = new JsonSourceVO();
        source.setPath(ndjson.toString());
        source.setFormat("ndjson");

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("STREAMING");
        executionPolicy.setSourceChunkSize(chunkSize);
        executionPolicy.setSinkBatchSize(chunkSize);
        executionPolicy.setMaxRowsInMemory(ROW_COUNT + 1);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("streaming-ndjson-batches");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("incoming", source));
        template.setTransformers(List.of(passthroughNdjsonTransform()));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(jsonSourceRegistry()).run(template);

        Assertions.assertTrue(result.getRows().isEmpty());
        Assertions.assertEquals("STREAMING", result.getMetrics().getExecutionMode());
        Assertions.assertEquals(ROW_COUNT, result.getMetrics().getTotalRowsRead());
        Assertions.assertEquals(ROW_COUNT, result.getMetrics().getRowsWritten());
        Assertions.assertTrue(result.getMetrics().getPeakRowsInMemory() <= chunkSize);
        Assertions.assertEquals(ROW_COUNT / chunkSize, result.getMetrics().getChunksProcessed());
    }

    @Test
    void streamsCsvSourceToConsoleSinkInBatches(@TempDir Path tempDir) throws Exception {
        Path csv = writeCsvFixture(tempDir, CSV_ROW_COUNT);

        CsvSourceVO source = new CsvSourceVO();
        source.setPath(csv.toString());
        source.setHeader(true);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT id, name FROM incoming");

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("STREAMING");
        // D-03: omit sourceChunkSize — file sources default to 1000
        executionPolicy.setSinkBatchSize(500);
        executionPolicy.setMaxRowsInMemory(CSV_ROW_COUNT + 1);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("streaming-csv-demo");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("incoming", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(fileSourceRegistry()).run(template);

        Assertions.assertTrue(result.getRows().isEmpty());
        Assertions.assertNotNull(result.getMetrics());
        Assertions.assertEquals("STREAMING", result.getMetrics().getExecutionMode());
        Assertions.assertEquals(CSV_ROW_COUNT, result.getMetrics().getTotalRowsRead());
        Assertions.assertEquals(CSV_ROW_COUNT, result.getMetrics().getRowsWritten());
        Assertions.assertTrue(result.getMetrics().getPeakRowsInMemory() <= DEFAULT_FILE_CHUNK_SIZE);
        Assertions.assertTrue(result.getMetrics().getChunksProcessed() > 1);
        int expectedChunks = (CSV_ROW_COUNT + DEFAULT_FILE_CHUNK_SIZE - 1) / DEFAULT_FILE_CHUNK_SIZE;
        Assertions.assertEquals(expectedChunks, result.getMetrics().getChunksProcessed());
    }

    @Test
    void streamsCsvSourceToCsvSinkInBatches(@TempDir Path tempDir) throws Exception {
        Path inputCsv = writeCsvFixture(tempDir, CSV_TO_SINK_ROW_COUNT);
        Path outputCsv = tempDir.resolve("out.csv");

        CsvSourceVO source = new CsvSourceVO();
        source.setPath(inputCsv.toString());
        source.setHeader(true);

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("STREAMING");
        executionPolicy.setSourceChunkSize(CSV_TO_SINK_CHUNK_SIZE);
        executionPolicy.setSinkBatchSize(CSV_TO_SINK_CHUNK_SIZE);
        executionPolicy.setMaxRowsInMemory(CSV_TO_SINK_ROW_COUNT + 1);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("streaming-csv-to-csv");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("incoming", source));
        template.setTransformers(List.of(passthroughCsvTransform()));
        template.setSinks(List.of(csvSink(outputCsv)));

        TemplateV2RunResult result = new TemplateV2Runner(csvToFileSinkRegistry()).run(template);

        Assertions.assertTrue(result.getRows().isEmpty());
        Assertions.assertEquals("STREAMING", result.getMetrics().getExecutionMode());
        Assertions.assertEquals(CSV_TO_SINK_ROW_COUNT, result.getMetrics().getTotalRowsRead());
        Assertions.assertEquals(CSV_TO_SINK_ROW_COUNT, result.getMetrics().getRowsWritten());
        Assertions.assertTrue(result.getMetrics().getChunksProcessed() >= 6);

        List<String> lines = Files.readAllLines(outputCsv, StandardCharsets.UTF_8);
        Assertions.assertEquals(CSV_TO_SINK_ROW_COUNT + 1, lines.size());
    }

    @Test
    void streamsCsvSourceToNdjsonSinkInBatches(@TempDir Path tempDir) throws Exception {
        Path inputCsv = writeCsvFixture(tempDir, CSV_TO_SINK_ROW_COUNT);
        Path outputJson = tempDir.resolve("out.ndjson");

        CsvSourceVO source = new CsvSourceVO();
        source.setPath(inputCsv.toString());
        source.setHeader(true);

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("STREAMING");
        executionPolicy.setSourceChunkSize(CSV_TO_SINK_CHUNK_SIZE);
        executionPolicy.setSinkBatchSize(CSV_TO_SINK_CHUNK_SIZE);
        executionPolicy.setMaxRowsInMemory(CSV_TO_SINK_ROW_COUNT + 1);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("streaming-csv-to-ndjson");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("incoming", source));
        template.setTransformers(List.of(passthroughCsvTransform()));
        template.setSinks(List.of(ndjsonSink(outputJson)));

        TemplateV2RunResult result = new TemplateV2Runner(csvToFileSinkRegistry()).run(template);

        Assertions.assertEquals(CSV_TO_SINK_ROW_COUNT, result.getMetrics().getRowsWritten());
        Assertions.assertTrue(result.getMetrics().getChunksProcessed() >= 6);

        List<String> lines = Files.readAllLines(outputJson, StandardCharsets.UTF_8);
        Assertions.assertEquals(CSV_TO_SINK_ROW_COUNT, lines.size());
    }

    @Test
    void rejectsCsvSourceWithoutChunkedRowSource(@TempDir Path tempDir) throws Exception {
        Path csv = writeCsvFixture(tempDir, 10);

        CsvSourceVO source = new CsvSourceVO();
        source.setPath(csv.toString());
        source.setHeader(true);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("streaming-csv-in-memory-source");
        template.setExecutionPolicy(streamingPolicy());
        template.setSources(Map.of("incoming", source));
        template.setTransformers(List.of(passthroughCsvTransform()));
        template.setSinks(List.of(consoleSink()));

        // Factory that always materializes in memory — simulates missing explicit CHUNKED/STREAMING dispatch.
        CsvSourceFactory inMemoryOnlyFactory = new CsvSourceFactory(new DefaultCsvParser()) {
            @Override
            public org.gensokyo.data.calcite.RowSource create(
                    String name,
                    org.gensokyo.data.model.v2.SourceVO sourceVo,
                    EffectiveExecutionPolicy policy) {
                return new CsvRowSource(name, (CsvSourceVO) sourceVo, new DefaultCsvParser());
            }
        };
        TemplateV2RuntimeRegistry registry = new TemplateV2RuntimeRegistry(
                List.of(inMemoryOnlyFactory),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new TemplateV2Runner(registry).run(template));
        Assertions.assertTrue(exception.getMessage().contains("CHUNKED")
                || exception.getMessage().contains("STREAMING"));
        Assertions.assertTrue(exception.getMessage().contains("auto-promoted"));
    }

    @Test
    void rejectsMultipleCsvSourcesInStreamingMode(@TempDir Path tempDir) throws Exception {
        Path csv = writeCsvFixture(tempDir, 5);
        CsvSourceVO source = new CsvSourceVO();
        source.setPath(csv.toString());
        source.setHeader(true);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("streaming-multi-csv");
        template.setExecutionPolicy(streamingPolicy());
        template.setSources(Map.of("left", source, "right", source));
        template.setTransformers(List.of(passthroughCsvTransform()));
        template.setSinks(List.of(consoleSink()));

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new TemplateV2Runner(fileSourceRegistry()).run(template));
        Assertions.assertTrue(exception.getMessage().contains("exactly one source"));
    }

    @Test
    void streamsQuerySourceToJdbcSinkInBatches() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        jdbcTemplate.getJdbcTemplate().execute("create table source_t(id bigint, name varchar(20))");
        jdbcTemplate.getJdbcTemplate().execute("create table target_t(id bigint, name varchar(20))");
        StringBuilder insert = new StringBuilder("insert into source_t(id, name) values ");
        for (int i = 0; i < ROW_COUNT; i++) {
            if (i > 0) {
                insert.append(',');
            }
            insert.append('(').append(i).append(", 'n").append(i).append("')");
        }
        jdbcTemplate.getJdbcTemplate().execute(insert.toString());

        TemplateV2VO template = streamingJdbcTemplate(jdbcTemplate);
        TemplateV2RunResult result = new TemplateV2Runner(streamingRegistry(jdbcTemplate)).run(template);

        Assertions.assertTrue(result.getRows().isEmpty());
        Assertions.assertNotNull(result.getMetrics());
        Assertions.assertEquals("STREAMING", result.getMetrics().getExecutionMode());
        Assertions.assertEquals(ROW_COUNT, result.getMetrics().getRowsWritten());
        Assertions.assertEquals(ROW_COUNT, result.getMetrics().getTotalRowsRead());
        Assertions.assertTrue(result.getMetrics().getPeakRowsInMemory() <= 100);
        Assertions.assertEquals(ROW_COUNT, countRows(jdbcTemplate, "target_t"));
        Assertions.assertEquals(ROW_COUNT / 100, result.getMetrics().getChunksProcessed());
    }

    @Test
    void rejectsMultipleSources() {
        QuerySourceVO source = querySource("select 1 as id");
        SqlTransformVO transform = passthroughTransform();

        ExecutionPolicyVO executionPolicy = streamingPolicy();
        TemplateV2VO template = new TemplateV2VO();
        template.setName("streaming-multi-source");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("left", source, "right", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(jdbcSink()));

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new StreamingPipeline().run(template, EffectiveExecutionPolicy.resolve(executionPolicy),
                        streamingRegistry(new NamedParameterJdbcTemplate(dataSource()))));
        Assertions.assertTrue(exception.getMessage().contains("exactly one source"));
    }

    @Test
    void rejectsBroadcastJoinShape() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        jdbcTemplate.getJdbcTemplate().execute("create table dim_t(id bigint)");
        jdbcTemplate.getJdbcTemplate().execute("create table fact_t(id bigint)");
        jdbcTemplate.getJdbcTemplate().execute("insert into dim_t(id) values (1)");
        jdbcTemplate.getJdbcTemplate().execute("insert into fact_t(id) values (1)");

        QuerySourceVO dim = querySource("select id from dim_t");
        dim.setMaxRows(10L);
        QuerySourceVO fact = querySource("select id from fact_t");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("""
                select f.id
                from fact f
                left join dim d on f.id = d.id
                """);

        ExecutionPolicyVO executionPolicy = streamingPolicy();
        TemplateV2VO template = new TemplateV2VO();
        template.setName("streaming-broadcast-join");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("dim", dim, "fact", fact));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(jdbcSink()));

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new TemplateV2Runner(streamingRegistry(jdbcTemplate)).run(template));
        Assertions.assertTrue(exception.getMessage().contains("exactly one source")
                || exception.getMessage().contains("BROADCAST_JOIN"));
    }

    private static TemplateV2VO streamingJdbcTemplate(NamedParameterJdbcTemplate jdbcTemplate) {
        QuerySourceVO source = querySource("select id, name from source_t order by id");

        SqlTransformVO transform = passthroughTransform();

        ExecutionPolicyVO executionPolicy = streamingPolicy();

        TemplateV2VO template = new TemplateV2VO();
        template.setName("streaming-pipeline-demo");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("t", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(jdbcSink()));
        return template;
    }

    private static ExecutionPolicyVO streamingPolicy() {
        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("STREAMING");
        executionPolicy.setSourceChunkSize(100);
        executionPolicy.setSinkBatchSize(100);
        executionPolicy.setMaxRowsInMemory(ROW_COUNT + 1);
        return executionPolicy;
    }

    private static QuerySourceVO querySource(String sql) {
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql(sql);
        return source;
    }

    private static SqlTransformVO passthroughTransform() {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select id, name from t");
        return transform;
    }

    private static SqlTransformVO passthroughCsvTransform() {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT id, name FROM incoming");
        return transform;
    }

    private static SqlTransformVO passthroughNdjsonTransform() {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT order_id, amount FROM incoming");
        return transform;
    }

    private static WriteStageVO consoleSink() {
        ConsoleWriterVO writer = new ConsoleWriterVO();
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));
        return sink;
    }

    private static TemplateV2RuntimeRegistry fileSourceRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new CsvSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }

    private static TemplateV2RuntimeRegistry jsonSourceRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new JsonSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }

    private static TemplateV2RuntimeRegistry csvToFileSinkRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new CsvSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new CsvSinkFactory(), new JsonSinkFactory()));
    }

    private static WriteStageVO csvSink(Path outputPath) {
        WriterVO writer = new WriterVO();
        writer.setType("CSV");
        writer.setTarget(outputPath.toString());
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));
        return sink;
    }

    private static WriteStageVO ndjsonSink(Path outputPath) {
        WriterVO writer = new WriterVO();
        writer.setType("JSON");
        writer.setTarget(outputPath.toString());
        writer.setOptions(Map.of("mode", "ndjson"));
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));
        return sink;
    }

    private static Path writeCsvFixture(Path tempDir, int rowCount) throws Exception {
        Path csv = tempDir.resolve("streaming.csv");
        List<String> lines = new ArrayList<>();
        lines.add("id,name");
        for (int i = 0; i < rowCount; i++) {
            lines.add(i + ",n" + i);
        }
        Files.writeString(csv, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return csv;
    }

    private static Path writeNdjsonFixture(Path tempDir, int rowCount) throws Exception {
        Path ndjson = tempDir.resolve("streaming.ndjson");
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            lines.add("{\"order_id\":\"o" + i + "\",\"amount\":" + i + "}");
        }
        Files.writeString(ndjson, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return ndjson;
    }

    private static WriteStageVO jdbcSink() {
        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("target_t");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));
        return sink;
    }

    private static TemplateV2RuntimeRegistry streamingRegistry(NamedParameterJdbcTemplate jdbcTemplate) {
        return new TemplateV2RuntimeRegistry(
                List.of(new QuerySourceFactory(jdbcTemplate)),
                List.of(new SqlTransformFactory()),
                List.of(new JdbcSinkFactory(jdbcTemplate, new NoopRuntimeJdbcEndpointResolver())));
    }

    private static long countRows(NamedParameterJdbcTemplate jdbcTemplate, String table) {
        Long count = jdbcTemplate.getJdbcTemplate().queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0L : count;
    }

    private DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:calcite_streaming_pipeline;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
