/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.support;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
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
 * Shared embedded KRaft broker for calcite integration tests (reference-counted lifecycle).
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class EmbeddedKafkaTestSupport {

    private static EmbeddedKafkaBroker broker;
    private static int references;

    private EmbeddedKafkaTestSupport() {
    }

    /**
     * Starts the embedded broker on first use and increments the reference count.
     */
    public static synchronized void acquire() {
        if (broker == null) {
            broker = new EmbeddedKafkaKraftBroker(1, 1);
            broker.afterPropertiesSet();
        }
        references++;
    }

    /**
     * Decrements the reference count and stops the broker when no tests hold a reference.
     */
    public static synchronized void release() {
        if (broker != null && --references <= 0) {
            broker.destroy();
            broker = null;
            references = 0;
        }
    }

    /**
     * Returns the shared broker instance (call {@link #acquire()} first).
     *
     * @return embedded Kafka broker
     */
    public static synchronized EmbeddedKafkaBroker broker() {
        if (broker == null) {
            throw new IllegalStateException("Embedded Kafka not started; call acquire() from @BeforeAll");
        }
        return broker;
    }

    /**
     * Creates a unique topic on the shared broker.
     *
     * @param namePrefix logical prefix for the topic name
     * @return topic name
     */
    public static String createTopic(String namePrefix) {
        String topic = namePrefix + "-" + UUID.randomUUID().toString().replace("-", "");
        broker().addTopics(new NewTopic(topic, 1, (short) 1));
        return topic;
    }

    /**
     * Builds a producer {@link KafkaTemplate} connected to the shared broker.
     *
     * @return Kafka template
     */
    public static KafkaTemplate<String, String> kafkaTemplate() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(broker());
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
    }

    /**
     * Consumes all records currently available on a topic (earliest offset).
     *
     * @param topic        topic name
     * @param consumerGroup unique consumer group id
     * @param timeout      poll timeout
     * @return record payloads in poll order
     */
    public static List<String> consumePayloads(String topic, String consumerGroup, Duration timeout) {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(consumerGroup, "true", broker());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps)
                .createConsumer()) {
            broker().consumeFromAnEmbeddedTopic(consumer, topic);
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, timeout);
            List<String> payloads = new ArrayList<>();
            records.forEach(record -> payloads.add(record.value()));
            return payloads;
        }
    }
}
