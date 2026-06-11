/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.Row;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;

/**
 * Tests for {@link ChunkedQueryRowSource}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
class ChunkedQueryRowSourceTests {

    private static final int CHUNK_SIZE = 5_000;
    private static final int ROW_COUNT = 12_000;

    @Test
    void readsMoreRowsThanOneChunkWithoutLoadingAllAtOnce() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        jdbcTemplate.getJdbcTemplate().execute("create table chunked_t(id bigint, name varchar(20))");
        for (int batch = 0; batch < ROW_COUNT; batch += 1_000) {
            StringBuilder insert = new StringBuilder("insert into chunked_t(id, name) values ");
            for (int i = batch; i < Math.min(batch + 1_000, ROW_COUNT); i++) {
                if (i > batch) {
                    insert.append(',');
                }
                insert.append('(').append(i).append(", 'n").append(i).append("')");
            }
            jdbcTemplate.getJdbcTemplate().execute(insert.toString());
        }

        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql("select id, name from chunked_t order by id");

        ChunkedQueryRowSource rowSource = new ChunkedQueryRowSource("t", source, jdbcTemplate, CHUNK_SIZE);

        int total = 0;
        while (rowSource.hasNextChunk()) {
            List<Row> chunk = rowSource.nextChunk(CHUNK_SIZE);
            total += chunk.size();
            Assertions.assertTrue(chunk.size() <= CHUNK_SIZE);
        }
        Assertions.assertEquals(ROW_COUNT, total);
        Assertions.assertEquals(ROW_COUNT, rowSource.rowsReadSoFar());
        Assertions.assertTrue(rowSource.rows().isEmpty());
        Assertions.assertTrue(rowSource.schema().contains("id"));
        Assertions.assertTrue(rowSource.schema().contains("name"));
    }

    private DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:calcite_chunked_query_source;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
