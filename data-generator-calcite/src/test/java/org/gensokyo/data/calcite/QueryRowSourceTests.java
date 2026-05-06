package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.vo.scripter.PlainScriptVO;
import org.gensokyo.data.model.vo.stage.ParamVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;

class QueryRowSourceTests {

    @Test
    void readsRowsFromJdbcQuery() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        jdbcTemplate.getJdbcTemplate().execute("create table sample(num_value bigint, name varchar(20))");
        jdbcTemplate.getJdbcTemplate().execute("insert into sample(num_value, name) values (1, 'a'), (2, 'b')");

        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql("select num_value, name from sample order by num_value");

        QueryRowSource rowSource = new QueryRowSource("input", source, jdbcTemplate);

        Assertions.assertEquals(2, rowSource.rows().size());
        Assertions.assertEquals("1", rowSource.rows().get(0).getString("num_value"));
        Assertions.assertEquals("a", rowSource.rows().get(0).getString("name"));
        Assertions.assertTrue(rowSource.schema().contains("num_value"));
        Assertions.assertTrue(rowSource.schema().contains("name"));
    }

    @Test
    void bindsPlainScriptParamsIntoJdbcQuery() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        jdbcTemplate.getJdbcTemplate().execute("create table sample_param(num_value bigint, name varchar(20))");
        jdbcTemplate.getJdbcTemplate().execute("insert into sample_param(num_value, name) values (1, 'a'), (2, 'b'), (3, 'c')");

        PlainScriptVO script = new PlainScriptVO();
        script.setContent("2");
        ParamVO param = new ParamVO();
        param.setName("minValue");
        param.setLanguage(script);

        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql("select num_value, name from sample_param where num_value >= :minValue order by num_value");
        source.setParams(List.of(param));

        QueryRowSource rowSource = new QueryRowSource("input", source, jdbcTemplate);

        Assertions.assertEquals(2, rowSource.rows().size());
        Assertions.assertEquals("b", rowSource.rows().get(0).getString("name"));
        Assertions.assertEquals("c", rowSource.rows().get(1).getString("name"));
    }

    @Test
    void infersSchemaEvenWhenParameterizedQueryReturnsNoRows() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        jdbcTemplate.getJdbcTemplate().execute("create table sample_empty_param(num_value bigint, name varchar(20))");
        jdbcTemplate.getJdbcTemplate().execute("insert into sample_empty_param(num_value, name) values (1, 'a'), (2, 'b')");

        PlainScriptVO script = new PlainScriptVO();
        script.setContent("99");
        ParamVO param = new ParamVO();
        param.setName("minValue");
        param.setLanguage(script);

        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql("select num_value, name from sample_empty_param where num_value >= :minValue order by num_value");
        source.setParams(List.of(param));

        QueryRowSource rowSource = new QueryRowSource("input", source, jdbcTemplate);

        Assertions.assertTrue(rowSource.rows().isEmpty());
        Assertions.assertTrue(rowSource.schema().contains("num_value"));
        Assertions.assertTrue(rowSource.schema().contains("name"));
        Assertions.assertEquals("BIGINT", rowSource.schema().column("num_value").getLogicalType());
        Assertions.assertEquals("VARCHAR", rowSource.schema().column("name").getLogicalType());
    }

    @Test
    void appliesPageWindowAndMaxRowsForQuerySource() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        jdbcTemplate.getJdbcTemplate().execute("create table sample_window(num_value bigint, name varchar(20))");
        jdbcTemplate.getJdbcTemplate().execute("""
                insert into sample_window(num_value, name)
                values (1, 'a'), (2, 'b'), (3, 'c'), (4, 'd'), (5, 'e')
                """);

        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql("select num_value, name from sample_window order by num_value");
        source.setPageIndex(2);
        source.setPageSize(2);
        source.setMaxRows(1L);

        QueryRowSource rowSource = new QueryRowSource("input", source, jdbcTemplate);

        Assertions.assertEquals(1, rowSource.rows().size());
        Assertions.assertEquals("3", rowSource.rows().get(0).getString("num_value"));
        Assertions.assertEquals("c", rowSource.rows().get(0).getString("name"));
    }

    private DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:calcite_query_source;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
