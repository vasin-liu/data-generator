package org.gensokyo.data.calcite;

public final class TemplateV2RuntimePluginDescriptors {
    private TemplateV2RuntimePluginDescriptors() {
    }

    public static TemplateV2RuntimePluginDescriptor builtIn(String id) {
        return TemplateV2RuntimePluginDescriptor.builder(id)
                .version("builtin")
                .hostVersionRange("current")
                .provider("gensokyo")
                .build();
    }
}
