/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.calcite.support.EmbeddedElasticsearchHttpSupport;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Integration tests for {@link ElasticsearchRowSinkAdapter} using an in-process HTTP bulk endpoint.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class ElasticsearchRowSinkAdapterHttpEmbeddedTests {

    private EmbeddedElasticsearchHttpSupport httpServer;

    @BeforeEach
    void startServer() throws Exception {
        httpServer = new EmbeddedElasticsearchHttpSupport();
        httpServer.start();
    }

    @AfterEach
    void stopServer() {
        if (httpServer != null) {
            httpServer.close();
        }
    }

    @Test
    void writesBulkPayloadToEmbeddedHttpEndpoint() throws Exception {
        WriterVO writer = elasticsearchWriter();
        writer.setTemplate("{\"device\":\"${device}\",\"value\":${value}}");

        try (var client = httpServer.restClient()) {
            new ElasticsearchRowSinkAdapter(client, writer).write(schema(), List.of(
                    new Row(Map.of("device", "d1", "value", 10)),
                    new Row(Map.of("device", "d2", "value", 20))));
        }

        Assertions.assertEquals(1, httpServer.bulkBodies().size());
        String payload = httpServer.bulkBodies().getFirst();
        Assertions.assertTrue(payload.contains("{\"index\":{\"_index\":\"metrics_v2\"}}"));
        Assertions.assertTrue(payload.contains("\"device\":\"d1\""));
        Assertions.assertTrue(payload.contains("\"device\":\"d2\""));
    }

    @Test
    void writeBatchUsesOneBulkRequestPerSlice() throws Exception {
        try (var client = httpServer.restClient()) {
            new ElasticsearchRowSinkAdapter(client, elasticsearchWriter()).writeBatch(
                    schema(),
                    List.of(
                            new Row(Map.of("device", "d1", "value", 10)),
                            new Row(Map.of("device", "d2", "value", 20)),
                            new Row(Map.of("device", "d3", "value", 30))),
                    1);
        }

        Assertions.assertEquals(3, httpServer.bulkBodies().size());
    }

    private static WriterVO elasticsearchWriter() {
        WriterVO writer = new WriterVO();
        writer.setType("ELASTICSEARCH");
        writer.setDataSourceId("main");
        writer.setTarget("metrics_v2");
        return writer;
    }

    private static RowSchema schema() {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(
                new ColumnDef("device", "VARCHAR", false),
                new ColumnDef("value", "BIGINT", false)));
        return schema;
    }
}
