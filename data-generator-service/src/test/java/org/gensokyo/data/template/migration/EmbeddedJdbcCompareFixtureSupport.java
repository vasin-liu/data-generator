/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * Shared embedded H2 setup for migration dual-run tests that exercise JDBC-shaped templates.
 *
 * @author Gensokyo
 * @since 2026-05-22
 */
public final class EmbeddedJdbcCompareFixtureSupport {

    /** In-memory H2 URL aligned with {@code application-phase7-test.yaml} {@code compare-inline-ds}. */
    public static final String H2_URL =
            "jdbc:h2:mem:compare_migration_embedded;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    /** Datasource id wired in phase-7 test configuration. */
    public static final String INLINE_DATA_SOURCE_ID = "compare-inline-ds";

    private static final Pattern DATABASE_ITERATOR_SQL = Pattern.compile(
            "  sql: >-.*?(?=\\n  pageIndex:)", Pattern.DOTALL);

    private EmbeddedJdbcCompareFixtureSupport() {
    }

    /**
     * Creates {@code t_compare} with columns required by {@code parking/11} SpEL compare CI.
     *
     * @throws Exception when DDL fails
     */
    public static void seedParkingCompareTable() throws Exception {
        try (Connection connection = DriverManager.getConnection(H2_URL, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("drop table if exists t_compare");
            statement.execute("""
                    create table t_compare (
                      id bigint primary key,
                      parking_lot_name varchar(200),
                      online_space_scale int,
                      regular_space_num int
                    )
                    """);
            statement.execute("""
                    insert into t_compare(id, parking_lot_name, online_space_scale, regular_space_num)
                    values (1, 'lot-a', 50, 10), (2, 'lot-b', 80, 20), (3, 'lot-c', 100, 5)
                    """);
        }
    }

    /**
     * Rewires a database-iterator V1 yaml to {@link #INLINE_DATA_SOURCE_ID} and parking/11 projection SQL.
     *
     * @param yaml original template yaml
     * @return adapted yaml for embedded compare
     */
    public static String adaptParking11ForEmbeddedH2(String yaml) {
        String adapted = rewireDataSourceToInline(yaml);
        return DATABASE_ITERATOR_SQL.matcher(adapted).replaceFirst("""
  sql: >-
    select id as ID, parking_lot_name as PARKING_LOT_NAME,
    online_space_scale as ONLINE_SPACE_SCALE, regular_space_num as REGULAR_SPACE_NUM
    from t_compare order by id
""");
    }

    /**
     * Replaces known production datasource ids with the inline H2 datasource id.
     *
     * @param yaml template yaml
     * @return yaml with {@link #INLINE_DATA_SOURCE_ID}
     */
    public static String rewireDataSourceToInline(String yaml) {
        String adapted = yaml.replace("dataSourceId: 'tocc_parking'", "dataSourceId: " + INLINE_DATA_SOURCE_ID);
        return adapted.replace("dataSourceId: tocc_parking", "dataSourceId: " + INLINE_DATA_SOURCE_ID);
    }
}
