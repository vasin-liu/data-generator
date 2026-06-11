/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.join;

import org.apache.calcite.sql.JoinType;
import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.sink.JdbcSinkFactory;
import org.gensokyo.data.calcite.source.QuerySourceFactory;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistry;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for broadcast join snapshot, executor, and chunked pipeline integration.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
class BroadcastJoinExecutorTests {

    private static final int DIM_ROWS = 100;
    private static final int FACT_ROWS = 8_000;

    @Test
    void leftJoinProjectsFactAndDimColumns() {
        RowSchema dimSchema = schema("id", "name");
        List<Row> dimRows = List.of(
                row("id", 1L, "name", "alpha"),
                row("id", 2L, "name", "beta"));
        BroadcastJoinSnapshot snapshot = BroadcastJoinSnapshot.materialize(
                new ListRowSource("dim", dimSchema, dimRows),
                "id",
                100,
                "dim");

        BroadcastJoinSpec spec = new BroadcastJoinSpec(
                JoinType.LEFT,
                "fact",
                "dim",
                "dim_id",
                "id",
                List.of(
                        new BroadcastJoinSpec.OutputColumn("id", BroadcastJoinSpec.ProjectionSide.FACT, "id"),
                        new BroadcastJoinSpec.OutputColumn("name", BroadcastJoinSpec.ProjectionSide.DIM, "name")),
                schema("id", "name"));

        List<Row> factChunk = List.of(
                row("id", 10L, "dim_id", 1L),
                row("id", 11L, "dim_id", 99L));

        CalciteRowTransformer.TransformResult result = BroadcastJoinExecutor.join(factChunk, snapshot, spec);

        Assertions.assertEquals(2, result.rows().size());
        Assertions.assertEquals(10L, result.rows().get(0).get("id"));
        Assertions.assertEquals("alpha", result.rows().get(0).get("name"));
        Assertions.assertEquals(11L, result.rows().get(1).get("id"));
        Assertions.assertNull(result.rows().get(1).get("name"));
    }

    @Test
    void innerJoinSkipsUnmatchedFactRows() {
        BroadcastJoinSnapshot snapshot = BroadcastJoinSnapshot.materialize(
                new ListRowSource("dim", schema("id"), List.of(row("id", 1L))),
                "id",
                10,
                "dim");

        BroadcastJoinSpec spec = new BroadcastJoinSpec(
                JoinType.INNER,
                "fact",
                "dim",
                "dim_id",
                "id",
                List.of(new BroadcastJoinSpec.OutputColumn("id", BroadcastJoinSpec.ProjectionSide.FACT, "id")),
                schema("id"));

        CalciteRowTransformer.TransformResult result = BroadcastJoinExecutor.join(
                List.of(row("id", 1L, "dim_id", 1L), row("id", 2L, "dim_id", 9L)),
                snapshot,
                spec);

        Assertions.assertEquals(1, result.rows().size());
        Assertions.assertEquals(1L, result.rows().getFirst().get("id"));
    }

    @Test
    void chunkedBroadcastJoinWritesAllFactRows() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        jdbcTemplate.getJdbcTemplate().execute("create table dim_t(id bigint primary key, name varchar(32))");
        jdbcTemplate.getJdbcTemplate().execute("create table fact_t(id bigint primary key, dim_id bigint)");
        jdbcTemplate.getJdbcTemplate().execute("create table target_t(id bigint, name varchar(32))");

        for (int i = 0; i < DIM_ROWS; i++) {
            jdbcTemplate.getJdbcTemplate().update(
                    "insert into dim_t(id, name) values (?, ?)", i + 1, "dim-" + i);
        }
        for (int batch = 0; batch < FACT_ROWS; batch += 1_000) {
            StringBuilder insert = new StringBuilder("insert into fact_t(id, dim_id) values ");
            for (int i = batch; i < Math.min(batch + 1_000, FACT_ROWS); i++) {
                if (i > batch) {
                    insert.append(',');
                }
                long dimId = (i % DIM_ROWS) + 1;
                insert.append('(').append(i + 1).append(", ").append(dimId).append(')');
            }
            jdbcTemplate.getJdbcTemplate().execute(insert.toString());
        }

        QuerySourceVO fact = new QuerySourceVO();
        fact.setDataSourceId("ignored");
        fact.setSql("select id, dim_id from fact_t order by id");

        QuerySourceVO dim = new QuerySourceVO();
        dim.setDataSourceId("ignored");
        dim.setSql("select id, name from dim_t order by id");
        dim.setMaxRows((long) DIM_ROWS);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT f.id, d.name FROM fact f LEFT JOIN dim d ON f.dim_id = d.id");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("target_t");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("CHUNKED");
        executionPolicy.setSourceChunkSize(2_000);
        executionPolicy.setSinkBatchSize(500);
        executionPolicy.setMaxRowsInMemory(FACT_ROWS + DIM_ROWS);
        executionPolicy.setBroadcastMaxRows(DIM_ROWS);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("broadcast-join-demo");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("fact", fact, "dim", dim));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));

        TemplateV2RuntimeRegistry registry = new TemplateV2RuntimeRegistry(
                List.of(new QuerySourceFactory(jdbcTemplate)),
                List.of(new SqlTransformFactory()),
                List.of(new JdbcSinkFactory(jdbcTemplate, new NoopRuntimeJdbcEndpointResolver())));

        var result = new TemplateV2Runner(registry).run(template);

        Assertions.assertTrue(result.getRows().isEmpty());
        Assertions.assertEquals(FACT_ROWS + DIM_ROWS, result.getMetrics().getTotalRowsRead());
        Assertions.assertEquals(FACT_ROWS, countRows(jdbcTemplate, "target_t"));
    }

    private static long countRows(NamedParameterJdbcTemplate jdbcTemplate, String table) {
        Long count = jdbcTemplate.getJdbcTemplate().queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0L : count;
    }

    private static RowSchema schema(String... names) {
        RowSchema schema = new RowSchema();
        List<ColumnDef> columns = new java.util.ArrayList<>();
        for (String name : names) {
            columns.add(new ColumnDef(name, "ANY", true));
        }
        schema.setColumns(columns);
        return schema;
    }

    private static Row row(Object... keyValuePairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            values.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return new Row(values);
    }

    private DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:calcite_broadcast_join;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private static final class ListRowSource implements org.gensokyo.data.calcite.RowSource {
        private final String name;
        private final RowSchema schema;
        private final List<Row> rows;

        private ListRowSource(String name, RowSchema schema, List<Row> rows) {
            this.name = name;
            this.schema = schema;
            this.rows = rows;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public RowSchema schema() {
            return schema;
        }

        @Override
        public List<Row> rows() {
            return rows;
        }
    }
}
