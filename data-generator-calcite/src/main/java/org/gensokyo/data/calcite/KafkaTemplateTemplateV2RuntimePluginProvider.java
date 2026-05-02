package org.gensokyo.data.calcite;

import java.util.List;

public final class KafkaTemplateTemplateV2RuntimePluginProvider implements TemplateV2RuntimePluginProvider {
    @Override
    public TemplateV2RuntimePlugin createPlugin(TemplateV2RuntimeContext context) {
        if (context.runtimeServices().kafkaTemplateRegistry() == null) {
            return new TemplateV2RuntimePlugin() {
            };
        }
        return new TemplateV2RuntimePlugin() {
            @Override
            public List<V2SinkFactory> sinkFactories() {
                return List.of(new KafkaSinkFactory(context.runtimeServices()));
            }
        };
    }
}
