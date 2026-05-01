package org.gensokyo.data.generator;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.calcite.QuerySourceFactory;
import org.gensokyo.data.calcite.QueryRowSource;
import org.gensokyo.data.calcite.JdbcRowSinkAdapter;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;

@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class RuntimeJdbcEndpointResolverTests {

    @Autowired
    private RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Test
    void registersInlineDatasourceAndReadsRowsThroughQuerySourceFactory() {
        QuerySourceVO source = new QuerySourceVO();
        source.setSql("select id, name from inline_orders order by id");

        InlineDataSourceVO inline = new InlineDataSourceVO();
        inline.setName("inline-orders-test");
        inline.setType("jdbc");
        inline.setUrl("jdbc:h2:mem:inline-orders-test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        inline.setUsername("sa");
        inline.setPassword("");
        inline.setDriverClassName("org.h2.Driver");
        source.setDataSource(inline);

        String dataSourceId = runtimeJdbcEndpointResolver.resolveSourceDataSourceId(source);
        Assertions.assertEquals("inline-orders-test", dataSourceId);
        source.setDataSourceId(dataSourceId);

        try {
            DynamicDataSourceContextHolder.push(dataSourceId);
            namedParameterJdbcTemplate.getJdbcTemplate().execute("create table inline_orders(id bigint, name varchar(64))");
            namedParameterJdbcTemplate.getJdbcTemplate().execute("insert into inline_orders(id, name) values (1, 'alpha'), (2, 'beta')");
        } finally {
            DynamicDataSourceContextHolder.clear();
        }

        QueryRowSource rowSource = (QueryRowSource) new QuerySourceFactory(namedParameterJdbcTemplate, runtimeJdbcEndpointResolver)
                .create("orders", source);

        Assertions.assertEquals(2, rowSource.rows().size());
        Assertions.assertEquals("1", rowSource.rows().get(0).getString("id"));
        Assertions.assertEquals("alpha", rowSource.rows().get(0).getString("name"));
        Assertions.assertTrue(rowSource.schema().contains("id"));
        Assertions.assertTrue(rowSource.schema().contains("name"));
    }

    @Test
    void registersInlineDatasourceAndWritesRowsThroughJdbcSinkAdapter() {
        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setTarget("inline_sink_output");

        InlineDataSourceVO inline = new InlineDataSourceVO();
        inline.setName("inline-sink-test");
        inline.setType("jdbc");
        inline.setUrl("jdbc:h2:mem:inline-sink-test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        inline.setUsername("sa");
        inline.setPassword("");
        inline.setDriverClassName("org.h2.Driver");
        writer.setDataSource(inline);

        String dataSourceId = runtimeJdbcEndpointResolver.resolveSinkDataSourceId(writer);
        Assertions.assertEquals("inline-sink-test", dataSourceId);
        writer.setDataSourceId(dataSourceId);

        try {
            DynamicDataSourceContextHolder.push(dataSourceId);
            namedParameterJdbcTemplate.getJdbcTemplate().execute("create table inline_sink_output(id bigint, name varchar(64))");
        } finally {
            DynamicDataSourceContextHolder.clear();
        }

        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(
                new ColumnDef("id", "BIGINT", false),
                new ColumnDef("name", "VARCHAR", true)
        ));
        Row row = new Row(new LinkedHashMap<>(java.util.Map.of(
                "id", 1L,
                "name", "sink-a"
        )));

        new JdbcRowSinkAdapter(namedParameterJdbcTemplate, writer, runtimeJdbcEndpointResolver)
                .write(schema, List.of(row));

        try {
            DynamicDataSourceContextHolder.push(dataSourceId);
            List<LinkedHashMap<String, Object>> rows = namedParameterJdbcTemplate.getJdbcTemplate()
                    .query("select id, name from inline_sink_output", (rs, rowNum) -> {
                        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
                        item.put("id", rs.getLong("id"));
                        item.put("name", rs.getString("name"));
                        return item;
                    });
            Assertions.assertEquals(1, rows.size());
            Assertions.assertEquals("1", rows.get(0).get("id").toString());
            Assertions.assertEquals("sink-a", rows.get(0).get("name"));
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }
}
