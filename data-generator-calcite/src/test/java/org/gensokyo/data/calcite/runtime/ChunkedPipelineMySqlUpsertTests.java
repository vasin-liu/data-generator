/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.support.UpsertParitySupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * CHUNKED JDBC upsert idempotency against MySQL {@code ON DUPLICATE KEY UPDATE} (D-15, D-25).
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
@EnabledIf("org.gensokyo.data.calcite.support.DockerTestSupport#dockerAvailable")
@Testcontainers
class ChunkedPipelineMySqlUpsertTests {

    @Container
    @SuppressWarnings("resource")
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("upsert_parity")
            .withUsername("test")
            .withPassword("test")
            .withUrlParam("useCursorFetch", "true")
            .withUrlParam("defaultFetchSize", String.valueOf(UpsertParitySupport.SOURCE_CHUNK_SIZE));

    @Test
    void chunkedUpsertReRunIsIdempotent() {
        UpsertParitySupport.assertUpsertIdempotent(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword(),
                "com.mysql.cj.jdbc.Driver",
                "mysql");
    }
}
