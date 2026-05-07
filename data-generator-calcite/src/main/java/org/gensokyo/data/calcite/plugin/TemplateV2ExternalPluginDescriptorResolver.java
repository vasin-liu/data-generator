package org.gensokyo.data.calcite.plugin;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

public class TemplateV2ExternalPluginDescriptorResolver {
    public static final String DESCRIPTOR_PATH = "META-INF/data-generator/template-v2-plugin.properties";

    public TemplateV2RuntimePluginDescriptor resolve(Path jarPath, ClassLoader classLoader) {
        Objects.requireNonNull(jarPath, "jarPath");
        Objects.requireNonNull(classLoader, "classLoader");
        try {
            URL resource = classLoader.getResource(DESCRIPTOR_PATH);
            if (resource == null) {
                return fallback(jarPath);
            }
            Properties properties = new Properties();
            try (InputStream inputStream = resource.openStream()) {
                properties.load(inputStream);
            }
            return fromProperties(jarPath, properties);
        } catch (Exception ignored) {
            return fallback(jarPath);
        }
    }

    private TemplateV2RuntimePluginDescriptor fromProperties(Path jarPath, Properties properties) {
        TemplateV2RuntimePluginDescriptor.Builder builder = TemplateV2RuntimePluginDescriptor
                .builder(value(properties, "plugin.id", fallbackId(jarPath)))
                .version(value(properties, "plugin.version", "unspecified"))
                .hostVersionRange(value(properties, "plugin.host-version-range", "unspecified"))
                .provider(value(properties, "plugin.provider", "external"));
        for (TemplateV2PluginCapability capability : parseCapabilities(properties.getProperty("plugin.capabilities"))) {
            builder.capability(capability);
        }
        return builder.build();
    }

    private TemplateV2RuntimePluginDescriptor fallback(Path jarPath) {
        return TemplateV2RuntimePluginDescriptor.builder(fallbackId(jarPath))
                .version("unspecified")
                .hostVersionRange("unspecified")
                .provider("external")
                .build();
    }

    private String fallbackId(Path jarPath) {
        String fileName = jarPath.getFileName() == null ? "external-plugin" : jarPath.getFileName().toString();
        return fileName.endsWith(".jar") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }

    private String value(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Set<TemplateV2PluginCapability> parseCapabilities(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        Set<TemplateV2PluginCapability> capabilities = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            String normalized = token == null ? "" : token.trim();
            if (normalized.isBlank()) {
                continue;
            }
            String[] parts = normalized.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            String kind = parts[0].trim().toUpperCase(Locale.ROOT);
            String key = parts[1].trim();
            capabilities.add(new TemplateV2PluginCapability(TemplateV2PluginCapabilityKind.valueOf(kind), key));
        }
        return capabilities;
    }
}
