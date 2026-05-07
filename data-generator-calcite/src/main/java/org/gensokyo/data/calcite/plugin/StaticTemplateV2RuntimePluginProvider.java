package org.gensokyo.data.calcite.plugin;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

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
