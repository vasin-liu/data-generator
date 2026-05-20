/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.plugin.DefaultTemplateV2RuntimePlugin;
import org.gensokyo.data.calcite.plugin.KafkaTemplateTemplateV2RuntimePluginProvider;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.kafka.support.DynamicKafkaTemplateRegistry;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
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
 * End-to-end {@link TemplateV2Runner} test with iterator source, SQL transform, and Kafka sink
 * against an embedded KRaft broker (no Mockito on {@link KafkaTemplate}).
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class TemplateV2RunnerKafkaEmbeddedTests {

    private static EmbeddedKafkaBroker embeddedKafka;

    private String topic;

    @BeforeAll
    static void startEmbeddedKafka() {
        embeddedKafka = new EmbeddedKafkaKraftBroker(1, 1);
        embeddedKafka.afterPropertiesSet();
    }

    @BeforeEach
    void createTopic() {
        topic = "runner-kafka-" + UUID.randomUUID().toString().replace("-", "");
        embeddedKafka.addTopics(new NewTopic(topic, 1, (short) 1));
    }

    @AfterAll
    static void stopEmbeddedKafka() {
        if (embeddedKafka != null) {
            embeddedKafka.destroy();
        }
    }

    @Test
    void runnerWritesFilteredRowsToEmbeddedKafkaTopic() {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplate();
        DynamicKafkaTemplateRegistry registry = new DynamicKafkaTemplateRegistry("main", Map.of("main", kafkaTemplate));
        TemplateV2RuntimeContext context = new TemplateV2RuntimeContext(
                new NoopRuntimeJdbcEndpointResolver(),
                new TemplateV2RuntimeServices(null, registry, null),
                List.of(),
                getClass().getClassLoader());
        TemplateV2RuntimeRegistry runtimeRegistry = new TemplateV2RuntimeRegistryFactory().fromPlugins(List.of(
                new DefaultTemplateV2RuntimePlugin(),
                new KafkaTemplateTemplateV2RuntimePluginProvider().createPlugin(context)));

        new TemplateV2Runner(runtimeRegistry).run(template());

        List<String> payloads = consumePayloads();
        Assertions.assertEquals(2, payloads.size());
        Assertions.assertEquals(List.of("value=2", "value=3"), payloads.stream().sorted().toList());
    }

    private TemplateV2VO template() {
        WriterVO writer = new WriterVO();
        writer.setType("KAFKA");
        writer.setDataSourceId("main");
        writer.setTarget(topic);
        writer.setTemplate("value=${value}");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT value FROM seed WHERE value >= 2");

        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(1);
        iterator.setTo(3);
        iterator.setStep(1);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("kafka-v2-embedded-runner");
        template.setSources(Map.of("seed", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }

    private static KafkaTemplate<String, String> kafkaTemplate() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
    }

    private List<String> consumePayloads() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "runner-kafka-embedded-" + UUID.randomUUID(), "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps)
                .createConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topic);
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));
            List<String> payloads = new ArrayList<>();
            records.forEach(record -> payloads.add(record.value()));
            return payloads;
        }
    }
}
