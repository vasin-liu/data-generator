/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

import java.util.Objects;

/**
 * Kafka runtime resolution result. The producer template is an opaque adapter-owned object
 * (typically {@code KafkaTemplate}) to keep this API module free of Kafka client dependencies.
 *
 * @param connectionName catalog entry name
 * @param producerHandle opaque Kafka producer template from the kafka adapter
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public record KafkaResolvedConnection(String connectionName, Object producerHandle) implements ResolvedConnection {

    /**
     * Compact constructor validating required fields.
     */
    public KafkaResolvedConnection {
        Objects.requireNonNull(connectionName, "connectionName");
        Objects.requireNonNull(producerHandle, "producerHandle");
        if (connectionName.isBlank()) {
            throw new IllegalArgumentException("connectionName must not be blank");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionKind kind() {
        return ConnectionKind.KAFKA;
    }
}
