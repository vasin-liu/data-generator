package org.gensokyo.data.calcite;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Pf4jTemplateV2RuntimePluginProvider implements TemplateV2RuntimePluginProvider, AutoCloseable {
    private final Pf4jRuntimeExtensionLocator extensionLocator;

    public Pf4jTemplateV2RuntimePluginProvider(Pf4jRuntimeExtensionLocator extensionLocator) {
        this.extensionLocator = Objects.requireNonNull(extensionLocator, "extensionLocator");
    }

    @Override
    public TemplateV2RuntimePlugin createPlugin(TemplateV2RuntimeContext context) {
        extensionLocator.start();
        List<TemplateV2RuntimePlugin> plugins = new ArrayList<>();
        for (Pf4jTemplateV2RuntimeExtension extension : extensionLocator.loadExtensions()) {
            TemplateV2RuntimePluginProvider provider = Objects.requireNonNull(extension.provider(),
                    "PF4J Template V2 extension [" + extension.getClass().getName()
                            + "] must return a plugin provider");
            TemplateV2RuntimePlugin plugin = Objects.requireNonNull(provider.createPlugin(context),
                    "PF4J Template V2 extension [" + extension.getClass().getName()
                            + "] provider [" + provider.getClass().getName()
                            + "] must return a runtime plugin");
            plugins.add(new DescriptorAwareTemplateV2RuntimePlugin(plugin, plugin.descriptor()));
        }
        TemplateV2RuntimePluginContractValidator.validate(plugins);
        return new DirectoryAwareTemplateV2RuntimePluginProvider.CompositeTemplateV2RuntimePlugin(plugins);
    }

    @Override
    public void close() {
        extensionLocator.close();
    }
}
