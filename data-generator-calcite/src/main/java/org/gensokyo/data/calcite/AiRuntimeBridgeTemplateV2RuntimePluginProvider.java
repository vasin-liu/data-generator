package org.gensokyo.data.calcite;

import java.util.List;

public final class AiRuntimeBridgeTemplateV2RuntimePluginProvider implements TemplateV2RuntimePluginProvider {
    @Override
    public TemplateV2RuntimePlugin createPlugin(TemplateV2RuntimeContext context) {
        if (context.runtimeServices().aiRuntimeBridge() == null) {
            return new TemplateV2RuntimePlugin() {
            };
        }
        return new TemplateV2RuntimePlugin() {
            @Override
            public List<V2SourceFactory> sourceFactories() {
                return List.of(new AiSourceFactory(context.runtimeServices().aiRuntimeBridge()));
            }
        };
    }
}
