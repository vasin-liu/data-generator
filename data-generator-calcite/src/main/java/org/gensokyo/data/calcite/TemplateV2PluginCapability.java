package org.gensokyo.data.calcite;

import java.util.Locale;
import java.util.Objects;

public record TemplateV2PluginCapability(TemplateV2PluginCapabilityKind kind, String key) {
    public TemplateV2PluginCapability {
        kind = Objects.requireNonNull(kind, "kind");
        key = normalizeKey(key);
    }

    public static TemplateV2PluginCapability source(String key) {
        return new TemplateV2PluginCapability(TemplateV2PluginCapabilityKind.SOURCE, key);
    }

    public static TemplateV2PluginCapability transform(String key) {
        return new TemplateV2PluginCapability(TemplateV2PluginCapabilityKind.TRANSFORM, key);
    }

    public static TemplateV2PluginCapability sink(String key) {
        return new TemplateV2PluginCapability(TemplateV2PluginCapabilityKind.SINK, key);
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("capability key must not be blank");
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }
}
