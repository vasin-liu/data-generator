/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.support.DockerTestSupport;
import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.model.v2.GeoJsonSourceOutputVO;
import org.gensokyo.data.model.v2.PostGisQuerySourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for {@link PostGisQueryRowSource} against a PostGIS-enabled PostgreSQL container.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@EnabledIf("org.gensokyo.data.calcite.support.DockerTestSupport#dockerAvailable")
@Testcontainers
class PostGisQueryRowSourceContainerTests {

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>("postgis/postgis:16-3.4")
            .withDatabaseName("postgis_test")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void initSchema() {
        NamedParameterJdbcTemplate jdbc = jdbcTemplate();
        jdbc.getJdbcTemplate().execute("CREATE EXTENSION IF NOT EXISTS postgis");
        jdbc.getJdbcTemplate().execute("""
                CREATE TABLE sites (
                  id integer PRIMARY KEY,
                  geom geometry(Point, 4326)
                )
                """);
        jdbc.getJdbcTemplate().execute("""
                INSERT INTO sites (id, geom) VALUES
                (1, ST_SetSRID(ST_MakePoint(113.1, 22.1), 4326)),
                (2, ST_SetSRID(ST_MakePoint(113.2, 22.2), 4326))
                """);
    }

    @Test
    void readsLatLonFromPostGisTable() {
        PostGisQuerySourceVO source = new PostGisQuerySourceVO();
        source.setDataSourceId("ignored");
        source.setTable("sites");
        source.setGeometryColumn("geom");
        GeoJsonSourceOutputVO output = new GeoJsonSourceOutputVO();
        output.setFormat(GeoOutputFormatKind.columns);
        source.setOutput(output);

        PostGisQueryRowSource rowSource = new PostGisQueryRowSource("sites_in", source, jdbcTemplate());
        Assertions.assertEquals(2, rowSource.rows().size());
        Assertions.assertEquals(22.1, ((Number) rowSource.rows().get(0).values().get("lat")).doubleValue(), 0.001);
        Assertions.assertEquals(113.1, ((Number) rowSource.rows().get(0).values().get("lon")).doubleValue(), 0.001);
    }

    private static NamedParameterJdbcTemplate jdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(POSTGIS.getJdbcUrl());
        dataSource.setUsername(POSTGIS.getUsername());
        dataSource.setPassword(POSTGIS.getPassword());
        dataSource.setDriverClassName("org.postgresql.Driver");
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
