package org.gensokyo.data.calcite;

import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Map;

class TemplateV2RunnerTests {

    @Test
    void runsSingleSourceSingleTransformSingleSinkTemplate() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2");
        template.setSources(Map.of("seed", numberSource(1, 5, 1)));
        template.setTransformers(List.of(sql("SELECT value, value + 10 AS shifted FROM seed WHERE value >= 4")));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner().run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("4", result.getRows().get(0).getString("value"));
        Assertions.assertEquals("14", result.getRows().get(0).getString("shifted"));
        Assertions.assertEquals("15", result.getRows().get(1).getString("shifted"));
    }

    @Test
    void supportsMultipleSourcesInSingleSqlContext() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2");
        template.setSources(Map.of(
                "left_input", numberSource(1, 2, 1),
                "right_input", numberSource(3, 4, 1)
        ));
        template.setTransformers(List.of(sql("SELECT value FROM right_input WHERE value >= 4")));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner().run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals("4", result.getRows().get(0).getString("value"));
    }

    @Test
    void supportsInnerJoinAcrossMultipleSources() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-join");
        template.setSources(Map.of(
                "left_input", numberSource(1, 3, 1),
                "right_input", numberSource(2, 4, 1)
        ));
        template.setTransformers(List.of(sql("""
                SELECT l.value AS left_value, r.value AS right_value
                FROM left_input AS l
                INNER JOIN right_input AS r ON l.value = r.value
                WHERE r.value >= 2
                """)));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner().run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("2", result.getRows().get(0).getString("left_value"));
        Assertions.assertEquals("2", result.getRows().get(0).getString("right_value"));
        Assertions.assertEquals("3", result.getRows().get(1).getString("left_value"));
        Assertions.assertEquals("3", result.getRows().get(1).getString("right_value"));
    }

    @Test
    void writesRowsIntoJdbcSink() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("calcite_runner_sink"));
        jdbcTemplate.getJdbcTemplate().execute("create table sink_output(source_value bigint, shifted_value bigint)");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("sink_output");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-jdbc-sink");
        template.setSources(Map.of("seed", numberSource(1, 3, 1)));
        template.setTransformers(List.of(sql("SELECT value AS source_value, value + 10 AS shifted_value FROM seed WHERE value >= 2")));
        template.setSinks(List.of(sink));

        TemplateV2RunResult result = new TemplateV2Runner(List.of(new IteratorSourceFactory()), jdbcTemplate).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        List<Map<String, Object>> rows = jdbcTemplate.getJdbcTemplate()
                .queryForList("select source_value, shifted_value from sink_output order by source_value");
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals("2", rows.get(0).get("SOURCE_VALUE").toString());
        Assertions.assertEquals("12", rows.get(0).get("SHIFTED_VALUE").toString());
        Assertions.assertEquals("3", rows.get(1).get("SOURCE_VALUE").toString());
        Assertions.assertEquals("13", rows.get(1).get("SHIFTED_VALUE").toString());
    }

    private IteratorSourceVO numberSource(long from, long to, int step) {
        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(from);
        iterator.setTo(to);
        iterator.setStep(step);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);
        return source;
    }

    private SqlTransformVO sql(String sql) {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql(sql);
        return transform;
    }

    private WriteStageVO consoleSink() {
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));
        return sink;
    }

    private DriverManagerDataSource dataSource(String name) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + name + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
