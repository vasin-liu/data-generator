package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.codec.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.gensokyo.data.calcite.support.InMemoryCatalog;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;

/**
 * Fast unit tests for Kafka sink adapters (Mockito on {@link KafkaTemplate}).
 * End-to-end runner coverage uses {@link org.gensokyo.data.calcite.runtime.TemplateV2RunnerKafkaEmbeddedTests}.
 */
class KafkaSinkFactoryTests {

    @Test
    void writesRowsToKafkaUsingWriterTemplate() {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplate();
        WriterVO writer = kafkaWriter();
        writer.setTemplate("device=${device},value=${value}");

        new KafkaRowSinkAdapter(kafkaTemplate, writer).write(schema(), List.of(
                new Row(Map.of("device", "d1", "value", 10)),
                new Row(Map.of("device", "d2", "value", 20))
        ));

        ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = producerRecordCaptor();
        Mockito.verify(kafkaTemplate, Mockito.times(2)).send(recordCaptor.capture());
        Assertions.assertEquals(List.of("topic_v2", "topic_v2"),
                recordCaptor.getAllValues().stream().map(ProducerRecord::topic).toList());
        Assertions.assertEquals(List.of("device=d1,value=10", "device=d2,value=20"),
                recordCaptor.getAllValues().stream().map(ProducerRecord::value).toList());
    }

    @Test
    void writesRowsToKafkaAsJsonLikeValueWhenTemplateIsBlank() {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplate();
        WriterVO writer = kafkaWriter();

        new KafkaRowSinkAdapter(kafkaTemplate, writer).write(schema(), List.of(
                new Row(Map.of("device", "d1", "value", 10))
        ));

        ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = producerRecordCaptor();
        Mockito.verify(kafkaTemplate).send(recordCaptor.capture());
        Assertions.assertEquals("topic_v2", recordCaptor.getValue().topic());
        Assertions.assertTrue(recordCaptor.getValue().value().contains("\"device\":\"d1\""));
        Assertions.assertTrue(recordCaptor.getValue().value().contains("\"value\":10"));
    }

    @Test
    void writesRowsToKafkaWithResolvedKeyAndHeaders() {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplate();
        WriterVO writer = kafkaWriter();
        writer.setTemplate("value=${value}");
        writer.setOptions(Map.of(
                "key", "${device}",
                "headers", Map.of("source", "v2", "device", "${device}")
        ));

        new KafkaRowSinkAdapter(kafkaTemplate, writer).write(schema(), List.of(
                new Row(Map.of("device", "d1", "value", 10))
        ));

        ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = producerRecordCaptor();
        Mockito.verify(kafkaTemplate).send(recordCaptor.capture());
        ProducerRecord<String, String> record = recordCaptor.getValue();
        Assertions.assertEquals("topic_v2", record.topic());
        Assertions.assertEquals("d1", record.key());
        Assertions.assertEquals("value=10", record.value());
        Assertions.assertEquals("v2", headerValue(record, "source"));
        Assertions.assertEquals("d1", headerValue(record, "device"));
    }

    @Test
    void providerContributesKafkaSinkFactoryWhenRuntimeServiceExists() {
        InMemoryCatalog catalog = InMemoryCatalog.kafkaOnly("main", kafkaTemplate());

        TemplateV2RuntimePlugin plugin = new KafkaTemplateTemplateV2RuntimePluginProvider()
                .createPlugin(new TemplateV2RuntimeContext(
                        new NoopRuntimeJdbcEndpointResolver(),
                        new TemplateV2RuntimeServices(null, catalog),
                        List.of(),
                        getClass().getClassLoader()
                ));

        Assertions.assertEquals(1, plugin.sinkFactories().size());
        Assertions.assertTrue(plugin.sinkFactories().getFirst() instanceof KafkaSinkFactory);
        Assertions.assertTrue(plugin.sinkFactories().getFirst().supports(kafkaWriter()));
    }

    @Test
    void providerContributesNoFactoriesWhenKafkaRuntimeServiceIsMissing() {
        TemplateV2RuntimePlugin plugin = new KafkaTemplateTemplateV2RuntimePluginProvider()
                .createPlugin(new TemplateV2RuntimeContext(
                        new NoopRuntimeJdbcEndpointResolver(),
                        new TemplateV2RuntimeServices(null, null),
                        List.of(),
                        getClass().getClassLoader()
                ));

        Assertions.assertTrue(plugin.sinkFactories().isEmpty());
    }

    @Test
    void runnerWritesTransformRowsToKafkaSinkFromRuntimeProvider() {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplate();
        InMemoryCatalog catalog = InMemoryCatalog.kafkaOnly("main", kafkaTemplate);
        TemplateV2RuntimeContext context = new TemplateV2RuntimeContext(
                new NoopRuntimeJdbcEndpointResolver(),
                new TemplateV2RuntimeServices(null, catalog),
                List.of(),
                getClass().getClassLoader()
        );
        TemplateV2RuntimeRegistry runtimeRegistry = new TemplateV2RuntimeRegistryFactory().fromPlugins(List.of(
                new DefaultTemplateV2RuntimePlugin(),
                new KafkaTemplateTemplateV2RuntimePluginProvider().createPlugin(context)
        ));

        new TemplateV2Runner(runtimeRegistry).run(template());

        ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = producerRecordCaptor();
        Mockito.verify(kafkaTemplate, Mockito.times(2)).send(recordCaptor.capture());
        Assertions.assertEquals(List.of("value=2", "value=3"),
                recordCaptor.getAllValues().stream().map(ProducerRecord::value).toList());
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, String> kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<ProducerRecord<String, String>> producerRecordCaptor() {
        return ArgumentCaptor.forClass((Class) ProducerRecord.class);
    }

    private String headerValue(ProducerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private WriterVO kafkaWriter() {
        WriterVO writer = new WriterVO();
        writer.setType("KAFKA");
        writer.setDataSourceId("main");
        writer.setTarget("topic_v2");
        return writer;
    }

    private TemplateV2VO template() {
        WriterVO writer = kafkaWriter();
        writer.setTemplate("value=${value}");
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT value FROM seed WHERE value >= 2");

        TemplateV2VO template = new TemplateV2VO();
        template.setName("kafka-v2-runtime-demo");
        template.setSources(Map.of("seed", numberSource()));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }

    private IteratorSourceVO numberSource() {
        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(1);
        iterator.setTo(3);
        iterator.setStep(1);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);
        return source;
    }

    private RowSchema schema() {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(
                new ColumnDef("device", "VARCHAR", false),
                new ColumnDef("value", "BIGINT", false)
        ));
        return schema;
    }
}
