/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.testfixtures;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Shared helpers for fixture-backed embedded integration tests.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
final class FixtureTestSupport {

    private FixtureTestSupport() {
    }

    /**
     * Builds an in-memory H2 datasource using PostgreSQL compatibility mode.
     *
     * @param databaseName unique mem database name
     * @return H2 datasource with no production credentials
     */
    static DataSource h2DataSource(String databaseName) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    /**
     * Loads a classpath SQL seed script for the given scenario.
     *
     * @param scenario scenario basename (for example {@code reader-jdbc-basic})
     * @return SQL script content
     */
    static String loadSql(String scenario) {
        String resource = "fixtures/sql/" + scenario + ".sql";
        try (InputStream input = FixtureTestSupport.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing fixture SQL scenario: " + scenario);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to read fixture SQL [" + scenario + "]", ex);
        }
    }
}
