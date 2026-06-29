/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.parser.DefaultJsonParser;
import org.gensokyo.data.calcite.runtime.EffectiveExecutionPolicy;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.JsonSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Tests for {@link ChunkedJsonRowSource} and policy-aware {@link JsonSourceFactory}.
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
class ChunkedJsonRowSourceTests {

    private static final int CHUNK_SIZE = 1_000;
    private static final int ROW_COUNT = 10_000;

    @TempDir
    Path tempDir;

    @Test
    void readsNdjsonRowsInChunksWithoutMaterializingAllRows() throws Exception {
        Path ndjson = writeLargeNdjson();

        ChunkedJsonRowSource rowSource = new ChunkedJsonRowSource(
                "orders", jsonSource(ndjson, null), new DefaultJsonParser(), CHUNK_SIZE);

        int total = 0;
        while (rowSource.hasNextChunk()) {
            List<Row> chunk = rowSource.nextChunk(CHUNK_SIZE);
            total += chunk.size();
            Assertions.assertTrue(chunk.size() <= CHUNK_SIZE);
            Assertions.assertTrue(rowSource.rows().isEmpty(), "rows() must not materialize file");
        }
        Assertions.assertEquals(ROW_COUNT, total);
        Assertions.assertEquals(ROW_COUNT, rowSource.rowsReadSoFar());
        Assertions.assertTrue(rowSource.schema().contains("order_id"));
    }

    @Test
    void readsJsonArrayInChunksWithoutMaterializingAllRows() throws Exception {
        Path arrayFile = writeLargeJsonArray();

        ChunkedJsonRowSource rowSource = new ChunkedJsonRowSource(
                "people", jsonSource(arrayFile, "array"), new DefaultJsonParser(), CHUNK_SIZE);

        int total = 0;
        while (rowSource.hasNextChunk()) {
            List<Row> chunk = rowSource.nextChunk(CHUNK_SIZE);
            total += chunk.size();
            Assertions.assertTrue(chunk.size() <= CHUNK_SIZE);
        }
        Assertions.assertEquals(ROW_COUNT, total);
        Assertions.assertEquals(ROW_COUNT, rowSource.rowsReadSoFar());
    }

    @Test
    void parsesOrdersNdjsonFixtureIncrementally() throws Exception {
        Path fixture = copyFixture("fixtures/orders.ndjson");

        ChunkedJsonRowSource rowSource = new ChunkedJsonRowSource(
                "orders", jsonSource(fixture, "ndjson"), new DefaultJsonParser(), CHUNK_SIZE);

        List<Row> rows = new ArrayList<>();
        while (rowSource.hasNextChunk()) {
            rows.addAll(rowSource.nextChunk(CHUNK_SIZE));
        }
        Assertions.assertEquals(3, rows.size());
        Assertions.assertEquals("o1", rows.getFirst().get("order_id"));
        Assertions.assertEquals("o3", rows.get(2).get("order_id"));
    }

    @Test
    void malformedNdjsonLineThrowsWithLineNumber() throws Exception {
        Path ndjson = tempDir.resolve("bad.ndjson");
        Files.writeString(ndjson, "{\"id\":1}\nnot-json\n", StandardCharsets.UTF_8);

        ChunkedJsonRowSource rowSource = new ChunkedJsonRowSource(
                "bad", jsonSource(ndjson, "ndjson"), new DefaultJsonParser(), CHUNK_SIZE);

        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> rowSource.nextChunk(CHUNK_SIZE));
        Assertions.assertTrue(failure.getMessage().contains("line [2]"));
    }

    @Test
    void honorsMaxRowsAcrossChunks() throws Exception {
        Path ndjson = writeLargeNdjson();
        JsonSourceVO source = jsonSource(ndjson, "ndjson");
        source.setMaxRows(1_500L);

        ChunkedJsonRowSource rowSource = new ChunkedJsonRowSource(
                "capped", source, new DefaultJsonParser(), CHUNK_SIZE);

        int total = 0;
        while (rowSource.hasNextChunk()) {
            total += rowSource.nextChunk(CHUNK_SIZE).size();
        }
        Assertions.assertEquals(1_500, total);
        Assertions.assertEquals(1_500, rowSource.rowsReadSoFar());
    }

    @Test
    void formatFieldIsAvailableOnJsonSourceVo() {
        JsonSourceVO source = new JsonSourceVO();
        source.setFormat("ndjson");
        Assertions.assertEquals("ndjson", source.getFormat());
        source.setFormat("array");
        Assertions.assertEquals("array", source.getFormat());
    }

    @Test
    void factoryReturnsChunkedSourceForChunkedPolicy() {
        JsonSourceFactory factory = new JsonSourceFactory();
        ExecutionPolicyVO policyVo = new ExecutionPolicyVO();
        policyVo.setMode("CHUNKED");
        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(policyVo);

        RowSource source = factory.create("t", jsonSource(tempDir.resolve("unused.json"), null), policy);
        Assertions.assertInstanceOf(ChunkedJsonRowSource.class, source);
        Assertions.assertEquals(CHUNK_SIZE, ((ChunkedJsonRowSource) source).defaultChunkSize());
    }

    @Test
    void factoryReturnsInMemorySourceForInMemoryPolicy() {
        JsonSourceFactory factory = new JsonSourceFactory();
        ExecutionPolicyVO policyVo = new ExecutionPolicyVO();
        policyVo.setMode("IN_MEMORY");
        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(policyVo);

        RowSource source = factory.create("t", jsonSource(tempDir.resolve("unused.json"), null), policy);
        Assertions.assertInstanceOf(JsonRowSource.class, source);
    }

    private Path writeLargeNdjson() throws Exception {
        Path ndjson = tempDir.resolve("large.ndjson");
        String body = IntStream.range(0, ROW_COUNT)
                .mapToObj(i -> "{\"order_id\":\"o" + i + "\",\"amount\":" + i + "}")
                .collect(Collectors.joining("\n"));
        Files.writeString(ndjson, body + "\n", StandardCharsets.UTF_8);
        return ndjson;
    }

    private Path writeLargeJsonArray() throws Exception {
        Path arrayFile = tempDir.resolve("large-array.json");
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < ROW_COUNT; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append("{\"id\":").append(i).append(",\"name\":\"n").append(i).append("\"}");
        }
        builder.append(']');
        Files.writeString(arrayFile, builder.toString(), StandardCharsets.UTF_8);
        return arrayFile;
    }

    private Path copyFixture(String resourcePath) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            Assertions.assertNotNull(input, "missing test resource " + resourcePath);
            Path target = tempDir.resolve("orders.ndjson");
            Files.copy(input, target);
            return target;
        }
    }

    private JsonSourceVO jsonSource(Path path, String format) {
        JsonSourceVO source = new JsonSourceVO();
        source.setPath(path.toString());
        source.setFormat(format);
        return source;
    }
}
