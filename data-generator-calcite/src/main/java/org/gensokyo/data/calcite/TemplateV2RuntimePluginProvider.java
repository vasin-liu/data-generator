package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeContext;

public interface TemplateV2RuntimePluginProvider {
    default TemplateV2RuntimePlugin createPlugin() {
        return createPlugin(TemplateV2RuntimeContext.empty());
    }

    TemplateV2RuntimePlugin createPlugin(TemplateV2RuntimeContext context);
}
