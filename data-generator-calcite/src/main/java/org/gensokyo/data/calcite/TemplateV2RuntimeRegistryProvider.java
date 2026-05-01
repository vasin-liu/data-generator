package org.gensokyo.data.calcite;

public interface TemplateV2RuntimeRegistryProvider {
    TemplateV2RuntimeRegistry current();

    TemplateV2RuntimeRegistry refresh();
}
