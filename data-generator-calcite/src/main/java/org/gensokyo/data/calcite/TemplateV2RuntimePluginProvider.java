package org.gensokyo.data.calcite;

public interface TemplateV2RuntimePluginProvider {
    default TemplateV2RuntimePlugin createPlugin() {
        return createPlugin(TemplateV2RuntimeContext.empty());
    }

    TemplateV2RuntimePlugin createPlugin(TemplateV2RuntimeContext context);
}
