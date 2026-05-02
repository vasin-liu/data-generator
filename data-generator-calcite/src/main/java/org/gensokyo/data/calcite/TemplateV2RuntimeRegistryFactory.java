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
        int providerIndex = 0;
        for (TemplateV2RuntimePluginProvider provider : providers) {
            try {
                plugins.add(provider.createPlugin(context));
            } catch (RuntimeException e) {
                throw new TemplateV2RuntimeRegistryBuildException("Failed to create Template V2 runtime plugin"
                        + " from provider index [" + providerIndex + "]"
                        + ", provider [" + provider.getClass().getName() + "]", e);
            }
            providerIndex++;
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
            sourceFactories.addAll(sourceFactories(plugin));
            transformFactories.addAll(transformFactories(plugin));
            sinkFactories.addAll(sinkFactories(plugin));
        }

        return new TemplateV2RuntimeRegistry(sourceFactories, transformFactories, sinkFactories);
    }

    private List<V2SourceFactory> sourceFactories(TemplateV2RuntimePlugin plugin) {
        try {
            return plugin.sourceFactories();
        } catch (RuntimeException e) {
            throw pluginBuildFailure("source factories", plugin, e);
        }
    }

    private List<V2TransformFactory> transformFactories(TemplateV2RuntimePlugin plugin) {
        try {
            return plugin.transformFactories();
        } catch (RuntimeException e) {
            throw pluginBuildFailure("transform factories", plugin, e);
        }
    }

    private List<V2SinkFactory> sinkFactories(TemplateV2RuntimePlugin plugin) {
        try {
            return plugin.sinkFactories();
        } catch (RuntimeException e) {
            throw pluginBuildFailure("sink factories", plugin, e);
        }
    }

    private TemplateV2RuntimeRegistryBuildException pluginBuildFailure(String phase,
                                                                       TemplateV2RuntimePlugin plugin,
                                                                       RuntimeException cause) {
        String pluginId;
        try {
            pluginId = plugin.descriptor().id();
        } catch (RuntimeException descriptorFailure) {
            pluginId = "<descriptor-unavailable>";
        }
        return new TemplateV2RuntimeRegistryBuildException("Failed to collect Template V2 runtime plugin "
                + phase + " from plugin [" + plugin.getClass().getName() + "]"
                + ", plugin id [" + pluginId + "]", cause);
    }

    private List<TemplateV2RuntimePlugin> deduplicatePlugins(List<TemplateV2RuntimePlugin> plugins) {
        Map<String, TemplateV2RuntimePlugin> deduplicated = new LinkedHashMap<>();
        for (TemplateV2RuntimePlugin plugin : plugins) {
            try {
                deduplicated.putIfAbsent(plugin.descriptor().id(), plugin);
            } catch (RuntimeException e) {
                throw new TemplateV2RuntimeRegistryBuildException("Failed to read Template V2 runtime plugin descriptor"
                        + " from plugin [" + plugin.getClass().getName() + "]", e);
            }
        }
        return List.copyOf(deduplicated.values());
    }
}
