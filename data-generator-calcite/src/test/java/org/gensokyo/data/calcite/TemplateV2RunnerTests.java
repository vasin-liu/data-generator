package org.gensokyo.data.calcite;

import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
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

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

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

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

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

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("2", result.getRows().get(0).getString("left_value"));
        Assertions.assertEquals("2", result.getRows().get(0).getString("right_value"));
        Assertions.assertEquals("3", result.getRows().get(1).getString("left_value"));
        Assertions.assertEquals("3", result.getRows().get(1).getString("right_value"));
    }

    @Test
    void resolvesTransformAndSinkThroughRuntimeRegistry() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("calcite_runner_registry"));
        jdbcTemplate.getJdbcTemplate().execute("create table sink_output_registry(source_value bigint)");

        RegistryOnlyWriterVO writer = new RegistryOnlyWriterVO();
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-registry");
        template.setSources(Map.of("seed", numberSource(1, 2, 1)));
        template.setTransformers(List.of(new RegistryOnlyTransformVO()));
        template.setSinks(List.of(sink));

        TemplateV2RuntimeRegistry registry = new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
                List.of(new RegistryOnlyTransformFactory()),
                List.of(new RegistryOnlySinkFactory(jdbcTemplate))
        );

        new TemplateV2Runner(registry).run(template);

        List<Map<String, Object>> rows = jdbcTemplate.getJdbcTemplate()
                .queryForList("select source_value from sink_output_registry order by source_value");
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals("1", rows.get(0).get("SOURCE_VALUE").toString());
        Assertions.assertEquals("2", rows.get(1).get("SOURCE_VALUE").toString());
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

        TemplateV2RunResult result = new TemplateV2Runner(jdbcRegistry(jdbcTemplate)).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        List<Map<String, Object>> rows = jdbcTemplate.getJdbcTemplate()
                .queryForList("select source_value, shifted_value from sink_output order by source_value");
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals("2", rows.get(0).get("SOURCE_VALUE").toString());
        Assertions.assertEquals("12", rows.get(0).get("SHIFTED_VALUE").toString());
        Assertions.assertEquals("3", rows.get(1).get("SOURCE_VALUE").toString());
        Assertions.assertEquals("13", rows.get(1).get("SHIFTED_VALUE").toString());
    }

    @Test
    void writesRowsIntoJdbcSinkUsingTemplateMapping() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("calcite_runner_sink_template"));
        jdbcTemplate.getJdbcTemplate().execute("create table sink_output_mapped(col_a bigint, col_b bigint)");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("sink_output_mapped");
        writer.setTemplate("col_b:shifted_value,col_a:source_value");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-jdbc-sink-template");
        template.setSources(Map.of("seed", numberSource(1, 3, 1)));
        template.setTransformers(List.of(sql("SELECT value AS source_value, value + 10 AS shifted_value FROM seed WHERE value >= 2")));
        template.setSinks(List.of(sink));

        new TemplateV2Runner(jdbcRegistry(jdbcTemplate)).run(template);

        List<Map<String, Object>> rows = jdbcTemplate.getJdbcTemplate()
                .queryForList("select col_a, col_b from sink_output_mapped order by col_a");
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals("2", rows.get(0).get("COL_A").toString());
        Assertions.assertEquals("12", rows.get(0).get("COL_B").toString());
        Assertions.assertEquals("3", rows.get(1).get("COL_A").toString());
        Assertions.assertEquals("13", rows.get(1).get("COL_B").toString());
    }

    @Test
    void writesRowsIntoJdbcSinkUsingResolvedEndpoint() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("calcite_runner_sink_resolved"));
        jdbcTemplate.getJdbcTemplate().execute("create table sink_output_resolved(source_value bigint)");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("resolved-sink");
        writer.setTarget("sink_output_resolved");
        writer.setTemplate("source_value:value");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-jdbc-sink-resolved");
        template.setSources(Map.of("seed", numberSource(1, 2, 1)));
        template.setTransformers(List.of(sql("SELECT value FROM seed")));
        template.setSinks(List.of(sink));

        RuntimeJdbcEndpointResolver resolver = new RuntimeJdbcEndpointResolver() {
            @Override
            public String resolveSourceDataSourceId(org.gensokyo.data.model.v2.QuerySourceVO source) {
                return source.getDataSourceId();
            }

            @Override
            public String resolveSinkDataSourceId(JdbcWriterVO ignored) {
                return "resolved-sink";
            }
        };

        TemplateV2RuntimeRegistry registry = new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory(), new JdbcSinkFactory(jdbcTemplate, resolver))
        );

        new TemplateV2Runner(registry).run(template);

        List<Map<String, Object>> rows = jdbcTemplate.getJdbcTemplate()
                .queryForList("select source_value from sink_output_resolved order by source_value");
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals("1", rows.get(0).get("SOURCE_VALUE").toString());
        Assertions.assertEquals("2", rows.get(1).get("SOURCE_VALUE").toString());
    }

    @Test
    void failsFastWhenSinkExecutionPolicyIsDefault() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("calcite_runner_sink_fail_fast"));
        jdbcTemplate.getJdbcTemplate().execute("create table sink_output_fail_fast(source_value bigint)");

        WriteStageVO failingSink = new WriteStageVO();
        failingSink.setWriters(List.of(new FailingWriterVO()));

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("sink_output_fail_fast");
        writer.setTemplate("source_value:value");

        WriteStageVO jdbcSink = new WriteStageVO();
        jdbcSink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-sink-fail-fast");
        template.setSources(Map.of("seed", numberSource(1, 2, 1)));
        template.setTransformers(List.of(sql("SELECT value FROM seed")));
        template.setSinks(List.of(failingSink, jdbcSink));

        Assertions.assertThrows(IllegalStateException.class,
                () -> new PolicyAwareTemplateV2Runner(jdbcRegistry(jdbcTemplate)).run(template));

        Integer count = jdbcTemplate.getJdbcTemplate().queryForObject("select count(*) from sink_output_fail_fast", Integer.class);
        Assertions.assertEquals(0, count);
    }

    @Test
    void continuesOnErrorWhenSinkExecutionPolicyAllowsIt() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("calcite_runner_sink_continue"));
        jdbcTemplate.getJdbcTemplate().execute("create table sink_output_continue(source_value bigint)");

        WriteStageVO failingSink = new WriteStageVO();
        failingSink.setWriters(List.of(new FailingWriterVO()));

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("sink_output_continue");
        writer.setTemplate("source_value:value");

        WriteStageVO jdbcSink = new WriteStageVO();
        jdbcSink.setWriters(List.of(writer));

        SinkExecutionPolicyVO policy = new SinkExecutionPolicyVO();
        policy.setMode("CONTINUE_ON_ERROR");

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-sink-continue");
        template.setSinkExecutionPolicy(policy);
        template.setSources(Map.of("seed", numberSource(1, 2, 1)));
        template.setTransformers(List.of(sql("SELECT value FROM seed")));
        template.setSinks(List.of(failingSink, jdbcSink));

        new PolicyAwareTemplateV2Runner(jdbcRegistry(jdbcTemplate)).run(template);

        Integer count = jdbcTemplate.getJdbcTemplate().queryForObject("select count(*) from sink_output_continue", Integer.class);
        Assertions.assertEquals(2, count);
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

    private TemplateV2RuntimeRegistry defaultRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory())
        );
    }

    private TemplateV2RuntimeRegistry jdbcRegistry(NamedParameterJdbcTemplate jdbcTemplate) {
        return new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory(),
                        new JdbcSinkFactory(jdbcTemplate, new NoopRuntimeJdbcEndpointResolver()))
        );
    }

    private static final class FailingWriterVO extends WriterVO {
        private FailingWriterVO() {
            setType("FAILING");
        }
    }

    private static final class RegistryOnlyTransformVO extends TransformVO {
        private RegistryOnlyTransformVO() {
            setType("REGISTRY_ONLY");
        }
    }

    private static final class RegistryOnlyWriterVO extends WriterVO {
        private RegistryOnlyWriterVO() {
            setType("REGISTRY_ONLY_SINK");
        }
    }

    private static final class FailingRowSinkAdapter implements RowSink {
        @Override
        public void write(RowSchema schema, List<Row> rows) {
            throw new IllegalStateException("Intentional sink failure");
        }
    }

    private static final class PolicyAwareTemplateV2Runner extends TemplateV2Runner {
        private PolicyAwareTemplateV2Runner(TemplateV2RuntimeRegistry runtimeRegistry) {
            super(runtimeRegistry);
        }

        @Override
        protected RowSink createSink(WriterVO writer) {
            if (writer instanceof FailingWriterVO) {
                return new FailingRowSinkAdapter();
            }
            return super.createSink(writer);
        }
    }

    private static final class RegistryOnlyTransformFactory implements V2TransformFactory {
        @Override
        public boolean supports(TransformVO transform) {
            return transform instanceof RegistryOnlyTransformVO;
        }

        @Override
        public CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context) {
            return new CalciteRowTransformer("SELECT value AS source_value FROM seed").transform(context);
        }
    }

    private static final class RegistryOnlySinkFactory implements V2SinkFactory {
        private final NamedParameterJdbcTemplate jdbcTemplate;

        private RegistryOnlySinkFactory(NamedParameterJdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public boolean supports(WriterVO writer) {
            return writer instanceof RegistryOnlyWriterVO;
        }

        @Override
        public RowSink create(WriterVO writer) {
            return (schema, rows) -> {
                Map<String, ?>[] batch = rows.stream()
                        .map(row -> Map.of("source_value", row.get("source_value")))
                        .toArray(Map[]::new);
                jdbcTemplate.batchUpdate("insert into sink_output_registry(source_value) values(:source_value)", batch);
            };
        }
    }
}
