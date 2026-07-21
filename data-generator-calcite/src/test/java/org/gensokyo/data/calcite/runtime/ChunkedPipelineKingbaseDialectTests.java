/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.support.UpsertParitySupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * CHUNKED JDBC upsert idempotency for {@code kingbase} and {@code highgo} dialect keys via
 * PostgreSQL Testcontainers proxy (D-01, D-13, D-15).
 *
 * <p>Kingbase and HighGo reuse the PostgreSQL {@code ON CONFLICT} upsert SQL path; a PG container
 * with {@code options.dialect} set to {@code kingbase} or {@code highgo} fulfills the Phase 9
 * per-dialect read/write harness requirement without licensed DM/KB/HG images in default CI.
 *
 * @author Gensokyo
 * @since 2026-07-21
 */
@EnabledIf("org.gensokyo.data.calcite.support.DockerTestSupport#dockerAvailable")
@Testcontainers
class ChunkedPipelineKingbaseDialectTests {

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kingbase_dialect_proxy")
            .withUsername("test")
            .withPassword("test");

    /**
     * Proves {@code dialect=kingbase} upsert idempotency and {@code rowsUpserted &gt; 0} via PG proxy.
     */
    @Test
    void chunkedUpsertKingbaseDialectIsIdempotent() {
        // PG container executes ON CONFLICT SQL generated for kingbase dialect key (D-15).
        UpsertParitySupport.assertUpsertIdempotent(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword(),
                "org.postgresql.Driver",
                "kingbase");
    }

    /**
     * Proves {@code dialect=highgo} upsert idempotency and {@code rowsUpserted &gt; 0} via PG proxy.
     */
    @Test
    void chunkedUpsertHighgoDialectIsIdempotent() {
        // PG container executes ON CONFLICT SQL generated for highgo dialect key (D-15).
        UpsertParitySupport.assertUpsertIdempotent(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword(),
                "org.postgresql.Driver",
                "highgo");
    }
}
