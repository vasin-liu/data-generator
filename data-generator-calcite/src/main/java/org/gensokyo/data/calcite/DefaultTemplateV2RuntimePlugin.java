package org.gensokyo.data.calcite;

import java.util.List;

public class DefaultTemplateV2RuntimePlugin implements TemplateV2RuntimePlugin {
    @Override
    public TemplateV2RuntimePluginDescriptor descriptor() {
        return TemplateV2RuntimePluginDescriptor.builder("builtin-default")
                .version("builtin")
                .hostVersionRange("current")
                .provider("gensokyo")
                .capability(TemplateV2PluginCapability.source("iterator"))
                .capability(TemplateV2PluginCapability.source("ai"))
                .capability(TemplateV2PluginCapability.transform("sql"))
                .capability(TemplateV2PluginCapability.sink("console"))
                .build();
    }

    @Override
    public List<V2SourceFactory> sourceFactories() {
        return List.of(new IteratorSourceFactory(), new AiSourceFactory());
    }

    @Override
    public List<V2TransformFactory> transformFactories() {
        return List.of(new SqlTransformFactory());
    }

    @Override
    public List<V2SinkFactory> sinkFactories() {
        return List.of(new ConsoleSinkFactory());
    }
}
