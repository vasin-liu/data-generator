/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api.snapshot;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Param-only connection freeze captured when a worker enters {@code RUNNING} (D-01..D-08).
 * Serialized to {@code task_execution.connection_snapshot_json} and retained permanently for audit;
 * in-process execution caches are populated in Wave 2 and cleared at terminal states.
 *
 * @param capturedAt  instant the snapshot was taken at RUNNING
 * @param connections JDBC, Kafka, and Elasticsearch refs referenced by the template run
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
public record ExecutionConnectionSnapshot(
        Instant capturedAt,
        List<SnapshottedConnectionRef> connections) {

    /**
     * Compact constructor validating capture metadata.
     */
    public ExecutionConnectionSnapshot {
        Objects.requireNonNull(capturedAt, "capturedAt");
        Objects.requireNonNull(connections, "connections");
        connections = List.copyOf(connections);
    }
}
