/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.plugin.DefaultTemplateV2RuntimePlugin;
import org.gensokyo.data.calcite.plugin.KafkaTemplateTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.support.EmbeddedKafkaTestSupport;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.calcite.support.InMemoryCatalog;
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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * End-to-end {@link TemplateV2Runner} test with iterator source, SQL transform, and Kafka sink
 * against an embedded KRaft broker (no Mockito on {@link org.springframework.kafka.core.KafkaTemplate}).
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class TemplateV2RunnerKafkaEmbeddedTests {

    private String topic;

    @BeforeAll
    static void acquireBroker() {
        EmbeddedKafkaTestSupport.acquire();
    }

    @BeforeEach
    void createTopic() {
        topic = EmbeddedKafkaTestSupport.createTopic("runner-kafka");
    }

    @AfterAll
    static void releaseBroker() {
        EmbeddedKafkaTestSupport.release();
    }

    @Test
    void runnerWritesFilteredRowsToEmbeddedKafkaTopic() {
        var kafkaTemplate = EmbeddedKafkaTestSupport.kafkaTemplate();
        InMemoryCatalog catalog = InMemoryCatalog.kafkaOnly("main", kafkaTemplate);
        TemplateV2RuntimeContext context = new TemplateV2RuntimeContext(
                new NoopRuntimeJdbcEndpointResolver(),
                new TemplateV2RuntimeServices(null, catalog),
                List.of(),
                getClass().getClassLoader());
        TemplateV2RuntimeRegistry runtimeRegistry = new TemplateV2RuntimeRegistryFactory().fromPlugins(List.of(
                new DefaultTemplateV2RuntimePlugin(),
                new KafkaTemplateTemplateV2RuntimePluginProvider().createPlugin(context)));

        new TemplateV2Runner(runtimeRegistry).run(template());

        List<String> payloads = EmbeddedKafkaTestSupport.consumePayloads(
                topic, "runner-kafka-" + UUID.randomUUID(), Duration.ofSeconds(15));
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
}
