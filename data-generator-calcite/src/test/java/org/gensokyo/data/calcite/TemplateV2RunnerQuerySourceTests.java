package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.scripter.PlainScriptVO;
import org.gensokyo.data.model.vo.stage.ParamVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Map;

class TemplateV2RunnerQuerySourceTests {

    @Test
    void runsTemplateWithQuerySource() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        jdbcTemplate.getJdbcTemplate().execute("create table sample(num_value bigint, name varchar(20))");
        jdbcTemplate.getJdbcTemplate().execute("insert into sample(num_value, name) values (1, 'a'), (2, 'b'), (3, 'c')");

        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql("select num_value, name from sample");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select num_value, name from input where num_value >= 2");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("query-source-demo");
        template.setSources(Map.of("input", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));

        TemplateV2RunResult result = new TemplateV2Runner(List.of(new QuerySourceFactory(jdbcTemplate))).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("b", result.getRows().get(0).getString("name"));
        Assertions.assertEquals("c", result.getRows().get(1).getString("name"));
    }

    @Test
    void runsTemplateWithParameterizedQuerySource() {
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
        source.setSql("select num_value, name from sample_param where num_value >= :minValue");
        source.setParams(List.of(param));

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select num_value, name from input where num_value < 3");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("query-source-param-demo");
        template.setSources(Map.of("input", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));

        TemplateV2RunResult result = new TemplateV2Runner(List.of(new QuerySourceFactory(jdbcTemplate))).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals("b", result.getRows().get(0).getString("name"));
        Assertions.assertEquals("2", result.getRows().get(0).getString("num_value"));
    }

    private DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:calcite_runner_query_source;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
