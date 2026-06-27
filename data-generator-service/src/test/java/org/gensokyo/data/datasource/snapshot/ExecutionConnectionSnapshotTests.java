/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.snapshot;

import org.gensokyo.data.datasource.api.CatalogEntrySource;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.snapshot.ExecutionConnectionSnapshot;
import org.gensokyo.data.datasource.api.snapshot.SnapshottedConnectionRef;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verifies param-only execution snapshot JSON round-trip without secret values (D-01, D-04, T-07-01).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
class ExecutionConnectionSnapshotTests {

    @Test
    void roundTripsJdbcKafkaElasticsearchRefsWithoutPlaintextSecrets() {
        Instant capturedAt = Instant.parse("2026-06-27T10:15:30Z");
        Instant catalogUpdatedAt = Instant.parse("2026-06-27T09:00:00Z");

        SnapshottedConnectionRef jdbc = new SnapshottedConnectionRef(
                "orders-db",
                ConnectionKind.JDBC,
                CatalogEntrySource.MANAGED,
                catalogUpdatedAt.toEpochMilli(),
                catalogUpdatedAt,
                Map.of(
                        "url", "jdbc:postgresql://db.example:5432/orders",
                        "username", "orders_user",
                        "passwordSecretRef", "secrets/orders-db-password",
                        "driverClassName", "org.postgresql.Driver"));

        SnapshottedConnectionRef kafka = new SnapshottedConnectionRef(
                "events",
                ConnectionKind.KAFKA,
                CatalogEntrySource.BOOTSTRAP,
                1L,
                null,
                Map.of(
                        "bootstrapServers", List.of("kafka-1:9092", "kafka-2:9092"),
                        "cluster", "events"));

        SnapshottedConnectionRef elasticsearch = new SnapshottedConnectionRef(
                "search",
                ConnectionKind.ELASTICSEARCH,
                CatalogEntrySource.MANAGED,
                42L,
                catalogUpdatedAt,
                Map.of(
                        "hosts", List.of("https://es.example:9200"),
                        "apiKeySecretRef", "secrets/search-api-key"));

        ExecutionConnectionSnapshot snapshot = new ExecutionConnectionSnapshot(
                capturedAt,
                List.of(jdbc, kafka, elasticsearch));

        String json = TemplateJsonCodec.write(snapshot);
        Assertions.assertFalse(json.contains("super-secret"), "serialized snapshot must not contain secret values");
        Assertions.assertFalse(json.contains("\"password\""), "serialized snapshot must use secretRef fields only");
        Assertions.assertTrue(json.contains("passwordSecretRef"));
        Assertions.assertTrue(json.contains("apiKeySecretRef"));

        ExecutionConnectionSnapshot decoded = TemplateJsonCodec.read(json, ExecutionConnectionSnapshot.class);
        Assertions.assertEquals(capturedAt, decoded.capturedAt());
        Assertions.assertEquals(3, decoded.connections().size());

        SnapshottedConnectionRef decodedJdbc = decoded.connections().get(0);
        Assertions.assertEquals(ConnectionKind.JDBC, decodedJdbc.kind());
        Assertions.assertEquals("orders-db", decodedJdbc.name());
        Assertions.assertEquals(CatalogEntrySource.MANAGED, decodedJdbc.source());
        Assertions.assertEquals("secrets/orders-db-password", decodedJdbc.configParams().get("passwordSecretRef"));

        SnapshottedConnectionRef decodedKafka = decoded.connections().get(1);
        Assertions.assertEquals(ConnectionKind.KAFKA, decodedKafka.kind());
        Assertions.assertEquals(CatalogEntrySource.BOOTSTRAP, decodedKafka.source());
        Assertions.assertEquals(List.of("kafka-1:9092", "kafka-2:9092"), decodedKafka.configParams().get("bootstrapServers"));

        SnapshottedConnectionRef decodedEs = decoded.connections().get(2);
        Assertions.assertEquals(ConnectionKind.ELASTICSEARCH, decodedEs.kind());
        Assertions.assertEquals("secrets/search-api-key", decodedEs.configParams().get("apiKeySecretRef"));
    }

    @Test
    void configParamsMapIsDefensivelyCopied() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("url", "jdbc:h2:mem:test");
        SnapshottedConnectionRef ref = new SnapshottedConnectionRef(
                "inline-ds",
                ConnectionKind.JDBC,
                CatalogEntrySource.MANAGED,
                1L,
                Instant.EPOCH,
                params);
        params.put("password", "must-not-leak-into-snapshot");

        Assertions.assertFalse(ref.configParams().containsKey("password"));
        Assertions.assertEquals("jdbc:h2:mem:test", ref.configParams().get("url"));
    }
}
