/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.sink.ConsoleSinkFactory;
import org.gensokyo.data.calcite.sink.CsvSinkFactory;
import org.gensokyo.data.calcite.sink.JsonSinkFactory;
import org.gensokyo.data.calcite.source.CsvSourceFactory;
import org.gensokyo.data.calcite.source.JsonSourceFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.JsonSourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * OOM proof integration tests for large CSV/JSON streaming under a 256&nbsp;MB heap (D-06, D-24).
 * <p>
 * Operator fixture bar: {@value #ROW_COUNT} rows and at least {@value #MIN_FIXTURE_BYTES} bytes per file
 * must complete without OOM when {@code executionPolicy.mode} is {@code CHUNKED} or {@code STREAMING}.
 * Run with {@code -Xmx256m}, for example:
 * {@code .\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=CsvJsonStreamingOomIT "-Dsurefire.argLine=-Xmx256m" -q}
 * </p>
 * <p>
 * {@code IN_MEMORY} on the same fixture is expected to OOM at this heap size; that path is intentionally
 * not a CI gate.
 * </p>
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
@Tag("oom")
class CsvJsonStreamingOomIT {

    /** Documented operator row-count bar (D-06). */
    static final int ROW_COUNT = 100_000;

    /** Documented minimum fixture size in bytes (D-06). */
    static final long MIN_FIXTURE_BYTES = 10L * 1024 * 1024;

    private static final int CHUNK_SIZE = EffectiveExecutionPolicy.DEFAULT_FILE_SOURCE_CHUNK_SIZE;

    /** Peak in-memory rows should stay within a small multiple of the read chunk. */
    private static final int PEAK_SAFETY_FACTOR = 2;

    private static Path fixtureDir;
    private static Path csvFixture;
    private static Path ndjsonFixture;

    /**
     * Materializes large CSV and NDJSON fixtures once for all OOM proof methods.
     */
    @BeforeAll
    static void generateFixtures() throws IOException {
        fixtureDir = Files.createTempDirectory("csv-json-oom-");
        csvFixture = writeLargeCsv(fixtureDir.resolve("oom.csv"), ROW_COUNT);
        ndjsonFixture = writeLargeNdjson(fixtureDir.resolve("oom.ndjson"), ROW_COUNT);

        long csvBytes = Files.size(csvFixture);
        long ndjsonBytes = Files.size(ndjsonFixture);
        if (csvBytes < MIN_FIXTURE_BYTES) {
            throw new AssertionError(
                    "CSV fixture below D-06 bar: " + csvBytes + " bytes (need >= " + MIN_FIXTURE_BYTES + ")");
        }
        if (ndjsonBytes < MIN_FIXTURE_BYTES) {
            throw new AssertionError(
                    "NDJSON fixture below D-06 bar: " + ndjsonBytes + " bytes (need >= " + MIN_FIXTURE_BYTES + ")");
        }
    }

    /**
     * Removes generated fixtures after the class completes.
     */
    @AfterAll
    static void deleteFixtures() throws IOException {
        if (fixtureDir != null && Files.exists(fixtureDir)) {
            try (var paths = Files.walk(fixtureDir)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    }
                    catch (IOException ignored) {
                        // Best-effort temp cleanup after OOM proof run.
                    }
                });
            }
        }
    }

    /**
     * CHUNKED CSV source completes at the documented OOM bar without retaining all rows in heap.
     */
    @Test
    void chunkedCsvCompletesUnder256mHeap() {
        TemplateV2RunResult result = runCsvTemplate("CHUNKED");
        assertOomBarOutcome(result, ROW_COUNT, CHUNK_SIZE);
    }

    /**
     * STREAMING CSV source completes at the documented OOM bar.
     */
    @Test
    void streamingCsvCompletesUnder256mHeap() {
        TemplateV2RunResult result = runCsvTemplate("STREAMING");
        assertOomBarOutcome(result, ROW_COUNT, CHUNK_SIZE);
    }

    /**
     * STREAMING NDJSON source completes at the documented OOM bar (W-02 JSON path).
     */
    @Test
    void streamingNdjsonCompletesUnder256mHeap() {
        TemplateV2RunResult result = runNdjsonTemplate();
        assertOomBarOutcome(result, ROW_COUNT, CHUNK_SIZE);
    }

    private static TemplateV2RunResult runCsvTemplate(String mode) {
        CsvSourceVO source = new CsvSourceVO();
        source.setPath(csvFixture.toString());
        source.setHeader(true);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT id, payload FROM incoming");

        ExecutionPolicyVO executionPolicy = executionPolicy(mode);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("oom-csv-" + mode.toLowerCase());
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("incoming", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(consoleSink()));

        return new TemplateV2Runner(fileSourceRegistry()).run(template);
    }

    private static TemplateV2RunResult runNdjsonTemplate() {
        JsonSourceVO source = new JsonSourceVO();
        source.setPath(ndjsonFixture.toString());
        source.setFormat("ndjson");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT order_id, amount, payload FROM incoming");

        ExecutionPolicyVO executionPolicy = executionPolicy("STREAMING");

        TemplateV2VO template = new TemplateV2VO();
        template.setName("oom-ndjson-streaming");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("incoming", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(ndjsonConsoleSink()));

        return new TemplateV2Runner(jsonSourceRegistry()).run(template);
    }

    private static ExecutionPolicyVO executionPolicy(String mode) {
        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode(mode);
        executionPolicy.setSourceChunkSize(CHUNK_SIZE);
        executionPolicy.setSinkBatchSize(CHUNK_SIZE);
        executionPolicy.setMaxRowsInMemory(ROW_COUNT + 1);
        return executionPolicy;
    }

    private static void assertOomBarOutcome(TemplateV2RunResult result, int expectedRows, int chunkSize) {
        Assertions.assertTrue(result.getRows().isEmpty(), "chunked/streaming runs must not retain all rows");
        Assertions.assertNotNull(result.getMetrics());
        Assertions.assertEquals(expectedRows, result.getMetrics().getTotalRowsRead());
        Assertions.assertEquals(expectedRows, result.getMetrics().getRowsWritten());
        int peakCap = chunkSize * PEAK_SAFETY_FACTOR;
        Assertions.assertTrue(
                result.getMetrics().getPeakRowsInMemory() <= peakCap,
                "peakRowsInMemory should stay bounded by chunk size * " + PEAK_SAFETY_FACTOR
                        + "; peak=" + result.getMetrics().getPeakRowsInMemory() + " cap=" + peakCap);
        int expectedChunks = (expectedRows + chunkSize - 1) / chunkSize;
        Assertions.assertEquals(expectedChunks, result.getMetrics().getChunksProcessed());
    }

    private static WriteStageVO consoleSink() {
        ConsoleWriterVO writer = new ConsoleWriterVO();
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));
        return sink;
    }

    private static WriteStageVO ndjsonConsoleSink() {
        return consoleSink();
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
                List.of(new ConsoleSinkFactory(), new JsonSinkFactory(), new CsvSinkFactory()));
    }

    private static Path writeLargeCsv(Path target, int rowCount) throws IOException {
        // Pad payload so ~100k rows exceed the 10 MB fixture bar without inflating row count.
        String payload = "x".repeat(100);
        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            writer.write("id,payload");
            writer.newLine();
            for (int i = 0; i < rowCount; i++) {
                writer.write(Integer.toString(i));
                writer.write(',');
                writer.write(payload);
                writer.newLine();
            }
        }
        return target;
    }

    private static Path writeLargeNdjson(Path target, int rowCount) throws IOException {
        String payload = "x".repeat(90);
        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            for (int i = 0; i < rowCount; i++) {
                writer.write("{\"order_id\":\"o");
                writer.write(Integer.toString(i));
                writer.write("\",\"amount\":");
                writer.write(Integer.toString(i));
                writer.write(",\"payload\":\"");
                writer.write(payload);
                writer.write("\"}");
                writer.newLine();
            }
        }
        return target;
    }
}
