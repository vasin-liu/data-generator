package org.gensokyo.data.calcite;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record TemplateV2RuntimePluginDescriptor(String id,
                                                String version,
                                                String hostVersionRange,
                                                String provider,
                                                Set<TemplateV2PluginCapability> capabilities) {
    public TemplateV2RuntimePluginDescriptor {
        id = normalize(id, "id");
        version = normalize(version, "version");
        hostVersionRange = normalize(hostVersionRange, "hostVersionRange");
        provider = normalize(provider, "provider");
        capabilities = capabilities == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(capabilities));
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    private static String normalize(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public static final class Builder {
        private final String id;
        private String version = "unspecified";
        private String hostVersionRange = "unspecified";
        private String provider = "unspecified";
        private final Set<TemplateV2PluginCapability> capabilities = new LinkedHashSet<>();

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder hostVersionRange(String hostVersionRange) {
            this.hostVersionRange = hostVersionRange;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder capability(TemplateV2PluginCapability capability) {
            this.capabilities.add(Objects.requireNonNull(capability, "capability"));
            return this;
        }

        public Builder capabilities(Iterable<TemplateV2PluginCapability> capabilities) {
            if (capabilities == null) {
                return this;
            }
            for (TemplateV2PluginCapability capability : capabilities) {
                capability(capability);
            }
            return this;
        }

        public TemplateV2RuntimePluginDescriptor build() {
            return new TemplateV2RuntimePluginDescriptor(id, version, hostVersionRange, provider, capabilities);
        }
    }
}
