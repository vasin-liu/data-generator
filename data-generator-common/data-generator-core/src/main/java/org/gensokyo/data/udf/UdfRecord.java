/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot of one versioned UDF registry entry.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
public final class UdfRecord {

    private final String udfId;
    private final String version;
    private final UdfType type;
    private final UdfLifecycleState state;
    private final byte[] payload;
    private final Map<String, String> metadata;
    private final Instant registeredAt;
    private final Instant publishedAt;
    private final Instant deprecatedAt;

    private UdfRecord(Builder builder) {
        this.udfId = builder.udfId;
        this.version = builder.version;
        this.type = builder.type;
        this.state = builder.state;
        this.payload = builder.payload == null ? new byte[0] : builder.payload.clone();
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(builder.metadata));
        this.registeredAt = builder.registeredAt;
        this.publishedAt = builder.publishedAt;
        this.deprecatedAt = builder.deprecatedAt;
    }

    /**
     * @return reverse-DNS stable identifier
     */
    public String udfId() {
        return udfId;
    }

    /**
     * @return semver version
     */
    public String version() {
        return version;
    }

    /**
     * @return UDF type
     */
    public UdfType type() {
        return type;
    }

    /**
     * @return lifecycle state
     */
    public UdfLifecycleState state() {
        return state;
    }

    /**
     * @return defensive copy of inline artifact bytes
     */
    public byte[] payload() {
        return payload.clone();
    }

    /**
     * @return optional metadata (e.g. {@code sqlName} for SQL UDFs)
     */
    public Map<String, String> metadata() {
        return metadata;
    }

    /**
     * @return registration timestamp
     */
    public Instant registeredAt() {
        return registeredAt;
    }

    /**
     * @return publish timestamp when published
     */
    public Instant publishedAt() {
        return publishedAt;
    }

    /**
     * @return deprecation timestamp when deprecated
     */
    public Instant deprecatedAt() {
        return deprecatedAt;
    }

    /**
     * @return builder pre-filled from this record
     */
    public Builder toBuilder() {
        return new Builder()
                .udfId(udfId)
                .version(version)
                .type(type)
                .state(state)
                .payload(payload)
                .metadata(metadata)
                .registeredAt(registeredAt)
                .publishedAt(publishedAt)
                .deprecatedAt(deprecatedAt);
    }

    /**
     * Mutable builder for {@link UdfRecord}.
     */
    public static final class Builder {
        private String udfId;
        private String version;
        private UdfType type;
        private UdfLifecycleState state = UdfLifecycleState.DRAFT;
        private byte[] payload = new byte[0];
        private Map<String, String> metadata = new LinkedHashMap<>();
        private Instant registeredAt = Instant.now();
        private Instant publishedAt;
        private Instant deprecatedAt;

        /**
         * @param udfId reverse-DNS id
         * @return this builder
         */
        public Builder udfId(String udfId) {
            this.udfId = udfId;
            return this;
        }

        /**
         * @param version semver
         * @return this builder
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * @param type UDF type
         * @return this builder
         */
        public Builder type(UdfType type) {
            this.type = type;
            return this;
        }

        /**
         * @param state lifecycle state
         * @return this builder
         */
        public Builder state(UdfLifecycleState state) {
            this.state = state;
            return this;
        }

        /**
         * @param payload artifact bytes
         * @return this builder
         */
        public Builder payload(byte[] payload) {
            this.payload = payload;
            return this;
        }

        /**
         * @param metadata metadata map
         * @return this builder
         */
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
            return this;
        }

        /**
         * @param registeredAt registration time
         * @return this builder
         */
        public Builder registeredAt(Instant registeredAt) {
            this.registeredAt = registeredAt;
            return this;
        }

        /**
         * @param publishedAt publish time
         * @return this builder
         */
        public Builder publishedAt(Instant publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        /**
         * @param deprecatedAt deprecation time
         * @return this builder
         */
        public Builder deprecatedAt(Instant deprecatedAt) {
            this.deprecatedAt = deprecatedAt;
            return this;
        }

        /**
         * @return immutable record
         */
        public UdfRecord build() {
            Objects.requireNonNull(udfId, "udfId");
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(registeredAt, "registeredAt");
            return new UdfRecord(this);
        }
    }
}
