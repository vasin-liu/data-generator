package org.gensokyo.data.calcite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TemplateV2RuntimePluginContractValidator {
    private TemplateV2RuntimePluginContractValidator() {
    }

    public static void validate(List<TemplateV2RuntimePlugin> plugins) {
        Map<String, TemplateV2RuntimePluginDescriptor> pluginIds = new HashMap<>();
        Map<TemplateV2PluginCapability, TemplateV2RuntimePluginDescriptor> capabilities = new HashMap<>();

        for (TemplateV2RuntimePlugin plugin : plugins) {
            TemplateV2RuntimePluginDescriptor descriptor = plugin.descriptor();
            TemplateV2RuntimePluginDescriptor previousPlugin = pluginIds.putIfAbsent(descriptor.id(), descriptor);
            if (previousPlugin != null) {
                throw new TemplateV2RuntimePluginContractException(
                        "duplicate Template V2 plugin id: " + descriptor.id());
            }
            for (TemplateV2PluginCapability capability : descriptor.capabilities()) {
                TemplateV2RuntimePluginDescriptor previousCapability = capabilities.putIfAbsent(capability, descriptor);
                if (previousCapability != null) {
                    throw new TemplateV2RuntimePluginContractException(
                            "duplicate Template V2 plugin capability: " + capability.kind() + ":" + capability.key()
                                    + " claimed by " + previousCapability.id() + " and " + descriptor.id());
                }
            }
        }
    }
}
