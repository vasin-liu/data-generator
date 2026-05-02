package org.gensokyo.data.calcite;

import org.gensokyo.data.kafka.support.DynamicKafkaTemplateRegistry;
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

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(kafkaTemplate, Mockito.times(2)).send(topicCaptor.capture(), valueCaptor.capture());
        Assertions.assertEquals(List.of("topic_v2", "topic_v2"), topicCaptor.getAllValues());
        Assertions.assertEquals(List.of("device=d1,value=10", "device=d2,value=20"), valueCaptor.getAllValues());
    }

    @Test
    void writesRowsToKafkaAsJsonLikeValueWhenTemplateIsBlank() {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplate();
        WriterVO writer = kafkaWriter();

        new KafkaRowSinkAdapter(kafkaTemplate, writer).write(schema(), List.of(
                new Row(Map.of("device", "d1", "value", 10))
        ));

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(kafkaTemplate).send(Mockito.eq("topic_v2"), valueCaptor.capture());
        Assertions.assertTrue(valueCaptor.getValue().contains("\"device\":\"d1\""));
        Assertions.assertTrue(valueCaptor.getValue().contains("\"value\":10"));
    }

    @Test
    void providerContributesKafkaSinkFactoryWhenRuntimeServiceExists() {
        DynamicKafkaTemplateRegistry registry = new DynamicKafkaTemplateRegistry("main",
                Map.of("main", kafkaTemplate()));

        TemplateV2RuntimePlugin plugin = new KafkaTemplateTemplateV2RuntimePluginProvider()
                .createPlugin(new TemplateV2RuntimeContext(
                        new NoopRuntimeJdbcEndpointResolver(),
                        new TemplateV2RuntimeServices(null, registry, null),
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
                        new TemplateV2RuntimeServices(null, null, null),
                        List.of(),
                        getClass().getClassLoader()
                ));

        Assertions.assertTrue(plugin.sinkFactories().isEmpty());
    }

    @Test
    void runnerWritesTransformRowsToKafkaSinkFromRuntimeProvider() {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplate();
        DynamicKafkaTemplateRegistry registry = new DynamicKafkaTemplateRegistry("main",
                Map.of("main", kafkaTemplate));
        TemplateV2RuntimeContext context = new TemplateV2RuntimeContext(
                new NoopRuntimeJdbcEndpointResolver(),
                new TemplateV2RuntimeServices(null, registry, null),
                List.of(),
                getClass().getClassLoader()
        );
        TemplateV2RuntimeRegistry runtimeRegistry = new TemplateV2RuntimeRegistryFactory().fromPlugins(List.of(
                new DefaultTemplateV2RuntimePlugin(),
                new KafkaTemplateTemplateV2RuntimePluginProvider().createPlugin(context)
        ));

        new TemplateV2Runner(runtimeRegistry).run(template());

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(kafkaTemplate, Mockito.times(2)).send(Mockito.eq("topic_v2"), valueCaptor.capture());
        Assertions.assertEquals(List.of("value=2", "value=3"), valueCaptor.getAllValues());
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, String> kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
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
