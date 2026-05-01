package org.gensokyo.data.calcite;

public class StaticTemplateV2RuntimePluginProvider implements TemplateV2RuntimePluginProvider {
    private final TemplateV2RuntimePlugin plugin;

    public StaticTemplateV2RuntimePluginProvider(TemplateV2RuntimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public TemplateV2RuntimePlugin createPlugin(TemplateV2RuntimeContext context) {
        return plugin;
    }
}
