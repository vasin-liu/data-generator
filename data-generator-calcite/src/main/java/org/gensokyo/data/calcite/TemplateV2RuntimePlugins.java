package org.gensokyo.data.calcite;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public final class TemplateV2RuntimePlugins {
    private TemplateV2RuntimePlugins() {
    }

    public static List<TemplateV2RuntimePlugin> load() {
        List<TemplateV2RuntimePlugin> plugins = new ArrayList<>();
        for (TemplateV2RuntimePluginProvider provider : loadProviders()) {
            plugins.add(provider.createPlugin(TemplateV2RuntimeContext.empty()));
        }
        return plugins;
    }

    public static List<TemplateV2RuntimePluginProvider> loadProviders() {
        List<TemplateV2RuntimePluginProvider> providers = new ArrayList<>();
        providers.add(new StaticTemplateV2RuntimePluginProvider(new DefaultTemplateV2RuntimePlugin()));
        providers.add(new AiRuntimeBridgeTemplateV2RuntimePluginProvider());
        providers.add(new DirectoryAwareTemplateV2RuntimePluginProvider());
        ServiceLoader.load(TemplateV2RuntimePluginProvider.class).forEach(providers::add);
        ServiceLoader.load(TemplateV2RuntimePlugin.class)
                .forEach(plugin -> providers.add(new StaticTemplateV2RuntimePluginProvider(plugin)));
        return providers;
    }
}
