/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.parser.DefaultCsvParser;
import org.gensokyo.data.calcite.runtime.EffectiveExecutionPolicy;
import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.Row;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link ChunkedCsvRowSource} and policy-aware {@link CsvSourceFactory}.
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
class ChunkedCsvRowSourceTests {

    private static final int CHUNK_SIZE = 1_000;
    private static final int ROW_COUNT = 5_000;

    @TempDir
    Path tempDir;

    @Test
    void readsMoreRowsThanOneChunkWithoutMaterializingAllRows() throws Exception {
        Path csv = writeLargeCsv(false);

        ChunkedCsvRowSource rowSource = new ChunkedCsvRowSource(
                "csv", csvSource(csv), new DefaultCsvParser(), CHUNK_SIZE);

        int total = 0;
        while (rowSource.hasNextChunk()) {
            List<Row> chunk = rowSource.nextChunk(CHUNK_SIZE);
            total += chunk.size();
            Assertions.assertTrue(chunk.size() <= CHUNK_SIZE);
            Assertions.assertTrue(rowSource.rows().isEmpty(), "rows() must not materialize file");
        }
        Assertions.assertEquals(ROW_COUNT, total);
        Assertions.assertEquals(ROW_COUNT, rowSource.rowsReadSoFar());
        Assertions.assertTrue(rowSource.schema().contains("id"));
        Assertions.assertTrue(rowSource.schema().contains("name"));
    }

    @Test
    void stripsUtf8BomFromHeaderAndParsesFirstDataRow() throws Exception {
        Path csv = tempDir.resolve("bom.csv");
        String body = "\uFEFFid,name\n1,alpha\n";
        Files.writeString(csv, body, StandardCharsets.UTF_8);

        ChunkedCsvRowSource rowSource = new ChunkedCsvRowSource(
                "bom", csvSource(csv), new DefaultCsvParser(), CHUNK_SIZE);

        Assertions.assertTrue(rowSource.hasNextChunk());
        List<Row> chunk = rowSource.nextChunk(CHUNK_SIZE);
        Assertions.assertEquals(1, chunk.size());
        Assertions.assertEquals("1", chunk.getFirst().get("id"));
        Assertions.assertEquals("alpha", chunk.getFirst().get("name"));
        Assertions.assertEquals("id", rowSource.schema().getColumns().getFirst().getName());
    }

    @Test
    void factoryReturnsChunkedSourceForChunkedPolicy() throws Exception {
        CsvSourceFactory factory = new CsvSourceFactory();
        ExecutionPolicyVO policyVo = new ExecutionPolicyVO();
        policyVo.setMode("CHUNKED");
        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(policyVo);

        RowSource source = factory.create("t", csvSource(tempDir.resolve("unused.csv")), policy);
        Assertions.assertInstanceOf(ChunkedCsvRowSource.class, source);
        Assertions.assertEquals(CHUNK_SIZE, ((ChunkedCsvRowSource) source).defaultChunkSize());
    }

    @Test
    void factoryReturnsInMemorySourceForInMemoryPolicy() throws Exception {
        CsvSourceFactory factory = new CsvSourceFactory();
        ExecutionPolicyVO policyVo = new ExecutionPolicyVO();
        policyVo.setMode("IN_MEMORY");
        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(policyVo);

        RowSource source = factory.create("t", csvSource(tempDir.resolve("unused.csv")), policy);
        Assertions.assertInstanceOf(CsvRowSource.class, source);
    }

    private Path writeLargeCsv(boolean withBom) throws Exception {
        Path csv = tempDir.resolve("large.csv");
        List<String> lines = new ArrayList<>();
        lines.add("id,name");
        for (int i = 0; i < ROW_COUNT; i++) {
            lines.add(i + ",n" + i);
        }
        String content = String.join("\n", lines) + "\n";
        if (withBom) {
            content = "\uFEFF" + content;
        }
        Files.writeString(csv, content, StandardCharsets.UTF_8);
        return csv;
    }

    private CsvSourceVO csvSource(Path path) {
        CsvSourceVO source = new CsvSourceVO();
        source.setPath(path.toString());
        source.setHeader(true);
        return source;
    }
}
