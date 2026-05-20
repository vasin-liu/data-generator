/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.ArrayList;
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

    private static EmbeddedKafkaBroker embeddedKafka;

    private String topic;

    @BeforeAll
    static void startEmbeddedKafka() {
        embeddedKafka = new EmbeddedKafkaKraftBroker(1, 1);
        embeddedKafka.afterPropertiesSet();
    }

    @BeforeEach
    void createTopic() {
        topic = "topic-v2-" + UUID.randomUUID().toString().replace("-", "");
        embeddedKafka.addTopics(new NewTopic(topic, 1, (short) 1));
    }

    @AfterAll
    static void stopEmbeddedKafka() {
        if (embeddedKafka != null) {
            embeddedKafka.destroy();
        }
    }

    @Test
    void writesTemplatedPayloadToEmbeddedTopic() {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplate();
        WriterVO writer = writerForTopic();
        writer.setTemplate("device=${device},value=${value}");

        new KafkaRowSinkAdapter(kafkaTemplate, writer).write(
                schema(),
                List.of(new Row(Map.of("device", "d1", "value", 10))));

        ConsumerRecord<String, String> record = consumeSingleRecord();
        Assertions.assertEquals(topic, record.topic());
        Assertions.assertEquals("device=d1,value=10", record.value());
    }

    @Test
    void writesBatchToEmbeddedTopic() {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplate();
        WriterVO writer = writerForTopic();

        new KafkaRowSinkAdapter(kafkaTemplate, writer).writeBatch(
                schema(),
                List.of(
                        new Row(Map.of("device", "d1", "value", 10)),
                        new Row(Map.of("device", "d2", "value", 20))),
                2);

        try (Consumer<String, String> consumer = consumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topic);
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
            List<String> payloads = new ArrayList<>();
            records.forEach(record -> payloads.add(record.value()));
            Assertions.assertEquals(2, payloads.size());
            Assertions.assertTrue(payloads.stream().anyMatch(value -> value.contains("\"device\":\"d1\"")));
            Assertions.assertTrue(payloads.stream().anyMatch(value -> value.contains("\"device\":\"d2\"")));
        }
    }

    private static KafkaTemplate<String, String> kafkaTemplate() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
    }

    private static Consumer<String, String> consumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("kafka-sink-embedded-test", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
    }

    private ConsumerRecord<String, String> consumeSingleRecord() {
        try (Consumer<String, String> consumer = consumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topic);
            return KafkaTestUtils.getSingleRecord(consumer, topic, Duration.ofSeconds(10));
        }
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
