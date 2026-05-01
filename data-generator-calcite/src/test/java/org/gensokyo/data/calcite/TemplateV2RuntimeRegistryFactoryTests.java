package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class TemplateV2RuntimeRegistryFactoryTests {

    @Test
    void loadsSpiPluginFactoriesIntoDefaultRegistry() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("spi-plugin-demo");
        template.setSources(Map.of("spi_source", new SpiOnlySourceVO()));
        template.setTransformers(List.of(new SpiOnlyTransformVO()));

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new SpiOnlyWriterVO()));
        template.setSinks(List.of(sink));

        TemplateV2RunResult result = new TemplateV2Runner(new TemplateV2RuntimeRegistryFactory().createDefault()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals("spi-value", result.getRows().getFirst().getString("spi_col"));
    }

    @Test
    void buildsRegistryFromProviders() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("provider-plugin-demo");
        template.setSources(Map.of("spi_source", new SpiOnlySourceVO()));
        template.setTransformers(List.of(new SpiOnlyTransformVO()));

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new SpiOnlyWriterVO()));
        template.setSinks(List.of(sink));

        TemplateV2RuntimeRegistry registry = new TemplateV2RuntimeRegistryFactory().fromProviders(List.of(
                context -> new TestSpiRuntimePlugin()
        ));

        TemplateV2RunResult result = new TemplateV2Runner(registry).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals("spi-value", result.getRows().getFirst().getString("spi_col"));
    }

    @Test
    void rejectsDuplicatePluginCapabilities() {
        TemplateV2RuntimePlugin first = new TemplateV2RuntimePlugin() {
            @Override
            public TemplateV2RuntimePluginDescriptor descriptor() {
                return TemplateV2RuntimePluginDescriptor.builder("plugin-a")
                        .capability(TemplateV2PluginCapability.sink("shared"))
                        .build();
            }
        };
        TemplateV2RuntimePlugin second = new TemplateV2RuntimePlugin() {
            @Override
            public TemplateV2RuntimePluginDescriptor descriptor() {
                return TemplateV2RuntimePluginDescriptor.builder("plugin-b")
                        .capability(TemplateV2PluginCapability.sink("shared"))
                        .build();
            }
        };

        Assertions.assertThrows(TemplateV2RuntimePluginContractException.class,
                () -> new TemplateV2RuntimeRegistryFactory().fromPlugins(List.of(first, second)));
    }

    @Test
    void deduplicatesSamePluginIdBeforeCapabilityValidation() {
        TemplateV2RuntimePlugin first = new TestSpiRuntimePlugin();
        TemplateV2RuntimePlugin second = new TestSpiRuntimePlugin();

        TemplateV2RuntimeRegistry registry = new TemplateV2RuntimeRegistryFactory()
                .fromPlugins(List.of(first, second));

        Assertions.assertDoesNotThrow(() -> registry.createSource("spi_source", new SpiOnlySourceVO()));
        Assertions.assertDoesNotThrow(() -> registry.createSink(new SpiOnlyWriterVO()));
        RowSource source = new SpiSourceFactory().create("spi_source", new SpiOnlySourceVO());
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addTable("spi_source", source.schema(), source.rows());
        Assertions.assertDoesNotThrow(() -> registry.applyTransform(new SpiOnlyTransformVO(), context));
    }

    static final class SpiOnlySourceVO extends SourceVO {
        SpiOnlySourceVO() {
            setType("SPI_ONLY_SOURCE");
        }
    }

    static final class SpiOnlyTransformVO extends TransformVO {
        SpiOnlyTransformVO() {
            setType("SPI_ONLY_TRANSFORM");
        }
    }

    static final class SpiOnlyWriterVO extends WriterVO {
        SpiOnlyWriterVO() {
            setType("SPI_ONLY_SINK");
        }
    }

    public static final class TestSpiRuntimePlugin implements TemplateV2RuntimePlugin {
        @Override
        public TemplateV2RuntimePluginDescriptor descriptor() {
            return TemplateV2RuntimePluginDescriptor.builder("test-spi-runtime-plugin")
                    .provider("test")
                    .version("1.0.0")
                    .hostVersionRange("current")
                    .capability(TemplateV2PluginCapability.source("spi_only_source"))
                    .capability(TemplateV2PluginCapability.transform("spi_only_transform"))
                    .capability(TemplateV2PluginCapability.sink("spi_only_sink"))
                    .build();
        }

        @Override
        public List<V2SourceFactory> sourceFactories() {
            return List.of(new SpiSourceFactory());
        }

        @Override
        public List<V2TransformFactory> transformFactories() {
            return List.of(new SpiTransformFactory());
        }

        @Override
        public List<V2SinkFactory> sinkFactories() {
            return List.of(new SpiSinkFactory());
        }
    }

    static final class SpiSourceFactory implements V2SourceFactory {
        @Override
        public boolean supports(SourceVO source) {
            return source instanceof SpiOnlySourceVO;
        }

        @Override
        public RowSource create(String name, SourceVO source) {
            return new RowSource() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public RowSchema schema() {
                    RowSchema schema = new RowSchema();
                    schema.setColumns(List.of(new ColumnDef("spi_col", "VARCHAR", true)));
                    return schema;
                }

                @Override
                public List<Row> rows() {
                    return List.of(new Row(Map.of("spi_col", "spi-value")));
                }
            };
        }
    }

    static final class SpiTransformFactory implements V2TransformFactory {
        @Override
        public boolean supports(TransformVO transform) {
            return transform instanceof SpiOnlyTransformVO;
        }

        @Override
        public CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context) {
            return new CalciteRowTransformer("SELECT spi_col FROM spi_source").transform(context);
        }
    }

    static final class SpiSinkFactory implements V2SinkFactory {
        @Override
        public boolean supports(WriterVO writer) {
            return writer instanceof SpiOnlyWriterVO;
        }

        @Override
        public RowSink create(WriterVO writer) {
            return (schema, rows) -> Assertions.assertFalse(rows.isEmpty());
        }
    }
}
