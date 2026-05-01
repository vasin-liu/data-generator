package org.gensokyo.data.calcite;

import org.pf4j.ExtensionPoint;

public interface Pf4jTemplateV2RuntimeExtension extends ExtensionPoint {
    TemplateV2RuntimePluginProvider provider();
}
