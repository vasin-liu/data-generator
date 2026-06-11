/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.calcite.support.EmbeddedKafkaTestSupport;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Integration tests for {@link KafkaRowSinkAdapter} against an in-process embedded Kafka broker.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
class KafkaRowSinkAdapterEmbeddedTests {

    private String topic;

    @BeforeAll
    static void acquireBroker() {
        EmbeddedKafkaTestSupport.acquire();
    }

    @BeforeEach
    void createTopic() {
        topic = EmbeddedKafkaTestSupport.createTopic("topic-v2");
    }

    @AfterAll
    static void releaseBroker() {
        EmbeddedKafkaTestSupport.release();
    }

    @Test
    void writesTemplatedPayloadToEmbeddedTopic() {
        KafkaTemplate<String, String> kafkaTemplate = EmbeddedKafkaTestSupport.kafkaTemplate();
        WriterVO writer = writerForTopic();
        writer.setTemplate("device=${device},value=${value}");

        new KafkaRowSinkAdapter(kafkaTemplate, writer).write(
                schema(),
                List.of(new Row(Map.of("device", "d1", "value", 10))));

        List<String> payloads = EmbeddedKafkaTestSupport.consumePayloads(
                topic, "kafka-sink-single-" + UUID.randomUUID(), Duration.ofSeconds(10));
        Assertions.assertEquals(1, payloads.size());
        Assertions.assertEquals("device=d1,value=10", payloads.getFirst());
    }

    @Test
    void writesBatchToEmbeddedTopic() {
        KafkaTemplate<String, String> kafkaTemplate = EmbeddedKafkaTestSupport.kafkaTemplate();
        WriterVO writer = writerForTopic();

        new KafkaRowSinkAdapter(kafkaTemplate, writer).writeBatch(
                schema(),
                List.of(
                        new Row(Map.of("device", "d1", "value", 10)),
                        new Row(Map.of("device", "d2", "value", 20))),
                2);

        List<String> payloads = EmbeddedKafkaTestSupport.consumePayloads(
                topic, "kafka-sink-batch-" + UUID.randomUUID(), Duration.ofSeconds(10));
        Assertions.assertEquals(2, payloads.size());
        Assertions.assertTrue(payloads.stream().anyMatch(value -> value.contains("\"device\":\"d1\"")));
        Assertions.assertTrue(payloads.stream().anyMatch(value -> value.contains("\"device\":\"d2\"")));
    }

    private WriterVO writerForTopic() {
        WriterVO writer = new WriterVO();
        writer.setType("KAFKA");
        writer.setDataSourceId("main");
        writer.setTarget(topic);
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
