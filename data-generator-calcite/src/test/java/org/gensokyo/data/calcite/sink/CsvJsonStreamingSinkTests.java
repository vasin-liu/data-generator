/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-chunk streaming flush tests for {@link CsvRowSinkAdapter} and {@link JsonRowSinkAdapter}.
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
class CsvJsonStreamingSinkTests {

    private static final int CHUNK_SIZE = 1_000;
    private static final int CHUNK_COUNT = 5;
    private static final int TOTAL_ROWS = CHUNK_SIZE * CHUNK_COUNT;

    @Test
    void csvStreamingFiveChunksProduces5000DataRows(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("out.csv");
        CsvRowSinkAdapter sink = streamingCsvSink(csv);
        RowSchema schema = idNameSchema();

        for (int chunk = 0; chunk < CHUNK_COUNT; chunk++) {
            sink.write(schema, chunkRows(chunk * CHUNK_SIZE, CHUNK_SIZE));
        }
        sink.finish();

        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        Assertions.assertEquals(TOTAL_ROWS + 1, lines.size());
        Assertions.assertEquals("id,name", lines.getFirst());
    }

    @Test
    void ndjsonStreamingFiveChunksProduces5000Lines(@TempDir Path tempDir) throws Exception {
        Path json = tempDir.resolve("out.ndjson");
        JsonRowSinkAdapter sink = streamingJsonSink(json, Map.of("mode", "ndjson"));
        RowSchema schema = idNameSchema();

        for (int chunk = 0; chunk < CHUNK_COUNT; chunk++) {
            sink.write(schema, chunkRows(chunk * CHUNK_SIZE, CHUNK_SIZE));
        }
        sink.finish();

        List<String> lines = Files.readAllLines(json, StandardCharsets.UTF_8);
        Assertions.assertEquals(TOTAL_ROWS, lines.size());
    }

    @Test
    void fileSizeGrowsIncrementallyBeforePipelineCompletes(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("growing.csv");
        CsvRowSinkAdapter sink = streamingCsvSink(csv);
        RowSchema schema = idNameSchema();

        sink.write(schema, chunkRows(0, CHUNK_SIZE));
        long sizeAfterFirstChunk = Files.size(csv);

        for (int chunk = 1; chunk < CHUNK_COUNT; chunk++) {
            sink.write(schema, chunkRows(chunk * CHUNK_SIZE, CHUNK_SIZE));
        }
        sink.finish();
        long finalSize = Files.size(csv);

        Assertions.assertTrue(sizeAfterFirstChunk > 0);
        Assertions.assertTrue(sizeAfterFirstChunk < finalSize);
    }

    @Test
    void arrayMultiChunkProducesValidJsonArray(@TempDir Path tempDir) throws Exception {
        Path json = tempDir.resolve("out.json");
        JsonRowSinkAdapter sink = streamingJsonSink(json, Map.of());
        RowSchema schema = idNameSchema();

        sink.write(schema, chunkRows(0, CHUNK_SIZE));
        sink.write(schema, chunkRows(CHUNK_SIZE, CHUNK_SIZE));
        sink.finish();

        String content = Files.readString(json, StandardCharsets.UTF_8);
        Assertions.assertTrue(content.trim().endsWith("]"));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode array = mapper.readTree(content);
        Assertions.assertTrue(array.isArray());
        Assertions.assertEquals(2 * CHUNK_SIZE, array.size());
    }

    @Test
    void arrayFinishClosesBracketWhenPipelineEndsEarly(@TempDir Path tempDir) throws Exception {
        Path json = tempDir.resolve("partial.json");
        JsonRowSinkAdapter sink = streamingJsonSink(json, Map.of());
        RowSchema schema = idNameSchema();

        sink.write(schema, chunkRows(0, 50));
        // Simulate pipeline finalize hook after a single chunk (failure or early exit).
        sink.finish();

        String content = Files.readString(json, StandardCharsets.UTF_8);
        Assertions.assertTrue(content.trim().endsWith("]"));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode array = mapper.readTree(content);
        Assertions.assertEquals(50, array.size());
    }

    private static CsvRowSinkAdapter streamingCsvSink(Path path) {
        CsvRowSinkAdapter sink = new CsvRowSinkAdapter(writer(path, "CSV", Map.of()));
        sink.enableStreaming();
        return sink;
    }

    private static JsonRowSinkAdapter streamingJsonSink(Path path, Map<String, Object> options) {
        JsonRowSinkAdapter sink = new JsonRowSinkAdapter(writer(path, "JSON", options));
        sink.enableStreaming();
        return sink;
    }

    private static WriterVO writer(Path path, String type, Map<String, Object> options) {
        WriterVO writer = new WriterVO();
        writer.setType(type);
        writer.setTarget(path.toString());
        writer.setOptions(options);
        return writer;
    }

    private static RowSchema idNameSchema() {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(
                new ColumnDef("id", "BIGINT", true),
                new ColumnDef("name", "VARCHAR", true)));
        return schema;
    }

    private static List<Row> chunkRows(int startId, int count) {
        List<Row> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int id = startId + i;
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("id", id);
            values.put("name", "n" + id);
            rows.add(new Row(values));
        }
        return rows;
    }
}
