/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.support.ChunkedJdbcParitySupport;
import org.gensokyo.data.calcite.support.DockerTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * CHUNKED JDBC export against MySQL 8 with {@code useCursorFetch} (production cursor semantics).
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@EnabledIf("org.gensokyo.data.calcite.support.DockerTestSupport#dockerAvailable")
@Testcontainers
class ChunkedPipelineMySqlContainerTests {

    @Container
    @SuppressWarnings("resource")
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("chunked_parity")
            .withUsername("test")
            .withPassword("test")
            .withUrlParam("useCursorFetch", "true")
            .withUrlParam("defaultFetchSize", String.valueOf(ChunkedJdbcParitySupport.SOURCE_CHUNK_SIZE));

    @Test
    void chunkedModeWritesAllRowsWithCursorFetch() {
        ChunkedJdbcParitySupport.assertChunkedExportParity(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword(),
                "com.mysql.cj.jdbc.Driver");
    }
}
