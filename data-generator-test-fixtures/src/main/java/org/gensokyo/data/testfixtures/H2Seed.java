/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.testfixtures;

import org.gensokyo.data.exception.DataGeneratorException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/**
 * Applies DDL/DML seed scripts to an in-memory H2 database for fixture-backed tests.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
public final class H2Seed {

    private H2Seed() {
    }

    /**
     * Executes a semicolon-separated SQL script against the supplied datasource.
     *
     * @param dataSource in-memory H2 datasource
     * @param sql        one or more SQL statements separated by {@code ;}
     * @throws DataGeneratorException when execution fails
     */
    public static void apply(DataSource dataSource, String sql) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource must not be null");
        }
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql must not be blank");
        }
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            // Split on semicolons so multi-statement fixture scripts stay readable.
            Arrays.stream(sql.split(";"))
                    .map(String::trim)
                    .filter(chunk -> !chunk.isEmpty())
                    .forEach(chunk -> execute(statement, chunk));
        }
        catch (SQLException ex) {
            throw new DataGeneratorException("Failed to apply H2 seed script", ex);
        }
    }

    private static void execute(Statement statement, String sql) {
        try {
            statement.execute(sql);
        }
        catch (SQLException ex) {
            throw new DataGeneratorException("Failed to execute seed statement: " + sql, ex);
        }
    }
}
