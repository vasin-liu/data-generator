package org.gensokyo.data.calcite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TemplateV2RuntimeRegistryFactory {

    public TemplateV2RuntimeRegistry createDefault() {
        return fromProviders(TemplateV2RuntimePlugins.loadProviders(), TemplateV2RuntimeContext.empty());
    }

    public TemplateV2RuntimeRegistry fromProviders(List<TemplateV2RuntimePluginProvider> providers) {
        return fromProviders(providers, TemplateV2RuntimeContext.empty());
    }

    public TemplateV2RuntimeRegistry fromProviders(List<TemplateV2RuntimePluginProvider> providers,
                                                   TemplateV2RuntimeContext context) {
        List<TemplateV2RuntimePlugin> plugins = new ArrayList<>();
        for (TemplateV2RuntimePluginProvider provider : providers) {
            plugins.add(provider.createPlugin(context));
        }
        return fromPlugins(plugins);
    }

    public TemplateV2RuntimeRegistry fromPlugins(List<TemplateV2RuntimePlugin> plugins) {
        List<TemplateV2RuntimePlugin> deduplicatedPlugins = deduplicatePlugins(plugins);
        TemplateV2RuntimePluginContractValidator.validate(deduplicatedPlugins);

        List<V2SourceFactory> sourceFactories = new ArrayList<>();
        List<V2TransformFactory> transformFactories = new ArrayList<>();
        List<V2SinkFactory> sinkFactories = new ArrayList<>();

        for (TemplateV2RuntimePlugin plugin : deduplicatedPlugins) {
            sourceFactories.addAll(plugin.sourceFactories());
            transformFactories.addAll(plugin.transformFactories());
            sinkFactories.addAll(plugin.sinkFactories());
        }

        return new TemplateV2RuntimeRegistry(sourceFactories, transformFactories, sinkFactories);
    }

    private List<TemplateV2RuntimePlugin> deduplicatePlugins(List<TemplateV2RuntimePlugin> plugins) {
        Map<String, TemplateV2RuntimePlugin> deduplicated = new LinkedHashMap<>();
        for (TemplateV2RuntimePlugin plugin : plugins) {
            deduplicated.putIfAbsent(plugin.descriptor().id(), plugin);
        }
        return List.copyOf(deduplicated.values());
    }
}
