package org.gensokyo.data.template;

import org.gensokyo.data.iterator.DatabaseIteratorVO;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.json.JsonSubtypeRegistry;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.vo.FieldVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.scripter.PlainScriptVO;
import org.gensokyo.data.model.vo.stage.ParamVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.reader.JdbcReaderVO;
import org.gensokyo.data.template.querysource.V1DatabaseSourceAdapter;
import org.gensokyo.data.template.querysource.V1QuerySourceDraftConverter;
import org.gensokyo.data.template.querysource.V1QuerySourceExtractor;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class TemplateV2SupportTests {

    @Test
    void detectsV1Template() {
        var v1 = new TemplateVO();
        v1.setFields(List.of(new FieldVO()));

        Assertions.assertEquals(TemplateDefinitionKind.V1, TemplateDefinitionDetector.detect(v1, null));
    }

    @Test
    void detectsV2Template() {
        var draft = new TemplateV2DraftVO();
        draft.setSources(Map.of("input", new IteratorSourceVO()));
        draft.setTransform(sql("SELECT value FROM input"));

        Assertions.assertEquals(TemplateDefinitionKind.V2, TemplateDefinitionDetector.detect(new TemplateVO(), draft));
    }

    @Test
    void detectsPartialV2TemplateAsV2() {
        var draft = new TemplateV2DraftVO();
        draft.setSources(Map.of("input", new IteratorSourceVO()));

        Assertions.assertEquals(TemplateDefinitionKind.V2, TemplateDefinitionDetector.detect(new TemplateVO(), draft));
    }

    @Test
    void normalizesSingularTransformAndSink() {
        var draft = new TemplateV2DraftVO();
        draft.setName("demo");
        draft.setSources(Map.of("input", new IteratorSourceVO()));
        draft.setTransform(sql("SELECT value FROM input"));
        draft.setSink(consoleSink());

        TemplateV2VO normalized = TemplateV2Normalizer.normalize(draft);

        Assertions.assertEquals(1, normalized.getTransformers().size());
        Assertions.assertEquals(1, normalized.getSinks().size());
    }

    @Test
    void rejectsMixedSingularAndPluralForms() {
        var draft = new TemplateV2DraftVO();
        draft.setTransform(sql("SELECT value FROM input"));
        draft.setTransformers(List.of(sql("SELECT value FROM input")));

        Assertions.assertThrows(IllegalArgumentException.class, () -> TemplateV2Normalizer.normalize(draft));
    }

    @Test
    void validatesMinimumTemplate() {
        var template = new TemplateV2VO();
        template.setName("demo");
        template.setSources(Map.of("input", new IteratorSourceVO()));
        template.setTransformers(List.of(sql("SELECT value FROM input")));
        template.setSinks(List.of(consoleSink()));

        Assertions.assertDoesNotThrow(() -> TemplateV2Validator.validate(template));
    }

    @Test
    void validatesChunkedModeWithRowLocalSql() {
        var template = new TemplateV2VO();
        template.setName("demo");
        template.setSources(Map.of("input", new IteratorSourceVO()));
        template.setTransformers(List.of(sql("SELECT value FROM input")));
        template.setSinks(List.of(consoleSink()));
        var policy = new ExecutionPolicyVO();
        policy.setMode("chunked");
        template.setExecutionPolicy(policy);

        Assertions.assertDoesNotThrow(() -> TemplateV2Validator.validate(template));
    }

    @Test
    void validatesChunkedModeWithBroadcastJoinSql() {
        var fact = new QuerySourceVO();
        fact.setSql("SELECT id, dim_id FROM fact_table");
        var dim = new QuerySourceVO();
        dim.setSql("SELECT id, name FROM dim_table");
        dim.setMaxRows(100L);

        var template = new TemplateV2VO();
        template.setName("demo");
        template.setSources(Map.of("fact", fact, "dim", dim));
        template.setTransformers(List.of(sql(
                "SELECT f.id, d.name FROM fact f LEFT JOIN dim d ON f.dim_id = d.id")));
        template.setSinks(List.of(consoleSink()));
        var policy = new ExecutionPolicyVO();
        policy.setMode("CHUNKED");
        template.setExecutionPolicy(policy);

        Assertions.assertDoesNotThrow(() -> TemplateV2Validator.validate(template));
    }

    @Test
    void rejectsChunkedModeWithTwoLargeQuerySources() {
        var left = new QuerySourceVO();
        left.setSql("SELECT id, customer_id FROM orders");
        var right = new QuerySourceVO();
        right.setSql("SELECT id, name FROM customers");

        var template = new TemplateV2VO();
        template.setName("demo");
        template.setSources(Map.of("orders", left, "customers", right));
        template.setTransformers(List.of(sql(
                "SELECT o.id, c.name FROM orders o INNER JOIN customers c ON o.customer_id = c.id")));
        template.setSinks(List.of(consoleSink()));
        var policy = new ExecutionPolicyVO();
        policy.setMode("CHUNKED");
        template.setExecutionPolicy(policy);

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> TemplateV2Validator.validate(template));
        Assertions.assertTrue(ex.getMessage().contains("CHUNKED"));
        Assertions.assertTrue(ex.getMessage().contains("unbounded QuerySourceVO"));
        Assertions.assertTrue(ex.getMessage().contains("2"));
    }

    @Test
    void rejectsChunkedModeWithGroupBySql() {
        var template = new TemplateV2VO();
        template.setName("demo");
        template.setSources(Map.of("input", new IteratorSourceVO()));
        template.setTransformers(List.of(sql("SELECT status, COUNT(*) FROM input GROUP BY status")));
        template.setSinks(List.of(consoleSink()));
        var policy = new ExecutionPolicyVO();
        policy.setMode("CHUNKED");
        template.setExecutionPolicy(policy);

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> TemplateV2Validator.validate(template));
        Assertions.assertTrue(ex.getMessage().contains("CHUNKED"));
        Assertions.assertTrue(
                ex.getMessage().contains("GROUP BY") || ex.getMessage().contains("MATERIALIZATION"));
    }

    @Test
    void validatesTransformerNamesWhenMultipleTransformsExist() {
        var template = new TemplateV2VO();
        template.setName("demo");
        template.setSources(Map.of("input", new IteratorSourceVO()));
        template.setTransformers(List.of(sql("SELECT value FROM input"), sql("SELECT value FROM input")));
        template.setSinks(List.of(consoleSink()));

        Assertions.assertThrows(IllegalArgumentException.class, () -> TemplateV2Validator.validate(template));
    }

    @Test
    void rowSchemaHelpersWork() {
        var schema = new RowSchema();
        schema.setColumns(List.of(
                new org.gensokyo.data.model.v2.ColumnDef("value", "BIGINT", false)
        ));
        var row = new Row(Map.of("value", 1L));

        Assertions.assertTrue(schema.contains("value"));
        Assertions.assertEquals("1", row.getString("value"));
    }

    @Test
    void parsesV2DraftYamlWithSingularAliases() {
        var parser = new JacksonParser();
        var yaml = """
                name: demo
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: constant
                      count: 2
                transform:
                  type: sql
                  sql: SELECT 1 AS value FROM input
                sink:
                  writers:
                    - type: console
                """;

        TemplateV2DraftVO draft = parser.parse(yaml, TemplateV2DraftVO.class);

        Assertions.assertNotNull(draft);
        Assertions.assertInstanceOf(IteratorSourceVO.class, draft.getSources().get("input"));
        Assertions.assertInstanceOf(SqlTransformVO.class, draft.getTransform());
        Assertions.assertEquals(1, draft.getSink().getWriters().size());
    }

    @Test
    void parsesV2QuerySourceWithInlineDatasource() {
        var parser = new JacksonParser();
        var yaml = """
                name: inline-ds-demo
                sources:
                  orders:
                    type: query
                    dataSource:
                      name: seatunnel_orders
                      type: jdbc
                      url: jdbc:h2:mem:inline_orders
                      username: sa
                      password: ""
                      driverClassName: org.h2.Driver
                    sql: SELECT 1 AS id
                transform:
                  type: sql
                  sql: SELECT id FROM orders
                sink:
                  writers:
                    - type: console
                """;

        TemplateV2DraftVO draft = parser.parse(yaml, TemplateV2DraftVO.class);

        Assertions.assertNotNull(draft);
        Assertions.assertInstanceOf(QuerySourceVO.class, draft.getSources().get("orders"));
        QuerySourceVO source = (QuerySourceVO) draft.getSources().get("orders");
        Assertions.assertNotNull(source.getDataSource());
        Assertions.assertInstanceOf(InlineDataSourceVO.class, source.getDataSource());
        Assertions.assertEquals("seatunnel_orders", source.getDataSource().getName());
        Assertions.assertEquals("jdbc:h2:mem:inline_orders", source.getDataSource().getUrl());
    }

    @Test
    void roundTripsV2DraftJson() {
        var draft = new TemplateV2DraftVO();
        draft.setName("demo");
        draft.setSources(Map.of("input", new IteratorSourceVO()));
        draft.setTransform(sql("SELECT value FROM input"));
        draft.setSink(consoleSink());

        String json = TemplateJsonCodec.write(draft);
        TemplateV2DraftVO decoded = TemplateJsonCodec.read(json, TemplateV2DraftVO.class);

        Assertions.assertEquals("demo", decoded.getName());
        Assertions.assertInstanceOf(IteratorSourceVO.class, decoded.getSources().get("input"));
        Assertions.assertInstanceOf(SqlTransformVO.class, decoded.getTransform());
        Assertions.assertEquals(1, decoded.getSink().getWriters().size());
    }

    @Test
    void parserSupportsDynamicallyRegisteredSourceSubtype() {
        var parser = new JacksonParser();
        JsonSubtypeRegistry.registerSubtype(SourceVO.class, DynamicTestSourceVO.class);
        var yaml = """
                name: dynamic-source-demo
                sources:
                  input:
                    type: dynamic_test_source
                    message: hello
                transform:
                  type: sql
                  sql: SELECT 1 AS value FROM input
                sink:
                  writers:
                    - type: console
                """;

        TemplateV2DraftVO draft = parser.parse(yaml, TemplateV2DraftVO.class);

        Assertions.assertInstanceOf(DynamicTestSourceVO.class, draft.getSources().get("input"));
        Assertions.assertEquals("hello", ((DynamicTestSourceVO) draft.getSources().get("input")).getMessage());
    }

    @Test
    void jsonCodecSupportsDynamicallyRegisteredSourceSubtype() {
        JsonSubtypeRegistry.registerSubtype(SourceVO.class, DynamicTestSourceVO.class);
        var draft = new TemplateV2DraftVO();
        draft.setName("dynamic-json-demo");
        draft.setSources(Map.of("input", new DynamicTestSourceVO("hello-json")));
        draft.setTransform(sql("SELECT 1 AS value FROM input"));
        draft.setSink(consoleSink());

        String json = TemplateJsonCodec.write(draft);
        TemplateV2DraftVO decoded = TemplateJsonCodec.read(json, TemplateV2DraftVO.class);

        Assertions.assertInstanceOf(DynamicTestSourceVO.class, decoded.getSources().get("input"));
        Assertions.assertEquals("hello-json", ((DynamicTestSourceVO) decoded.getSources().get("input")).getMessage());
    }

    @Test
    void adaptsDatabaseIteratorIntoQuerySource() {
        PlainScriptVO script = new PlainScriptVO();
        script.setContent("42");
        ParamVO param = new ParamVO();
        param.setName("tenantId");
        param.setLanguage(script);

        DatabaseIteratorVO iterator = new DatabaseIteratorVO();
        iterator.setDataSourceId("main-db");
        iterator.setSql("select * from demo where tenant_id = :tenantId");
        iterator.setParams(List.of(param));
        iterator.setPageIndex(2);
        iterator.setPageSize(50);
        iterator.setMaxRows(1000);

        QuerySourceVO source = V1DatabaseSourceAdapter.fromDatabaseIterator(iterator);

        Assertions.assertNotNull(source);
        Assertions.assertEquals("main-db", source.getDataSourceId());
        Assertions.assertEquals("select * from demo where tenant_id = :tenantId", source.getSql());
        Assertions.assertEquals(1, source.getParams().size());
        Assertions.assertEquals(2, source.getPageIndex());
        Assertions.assertEquals(50, source.getPageSize());
        Assertions.assertEquals(1000L, source.getMaxRows());
    }

    @Test
    void adaptsJdbcReaderIntoQuerySource() {
        PlainScriptVO script = new PlainScriptVO();
        script.setContent("100");
        ParamVO param = new ParamVO();
        param.setName("tenantId");
        param.setLanguage(script);

        ReadStageVO stage = new ReadStageVO();
        stage.setParams(List.of(param));

        JdbcReaderVO reader = new JdbcReaderVO();
        reader.setDataSourceId("main-db");
        reader.setContent("select id, name from demo");

        QuerySourceVO source = V1DatabaseSourceAdapter.fromJdbcReader(stage, reader);

        Assertions.assertNotNull(source);
        Assertions.assertEquals("main-db", source.getDataSourceId());
        Assertions.assertEquals("select id, name from demo", source.getSql());
        Assertions.assertEquals(1, source.getParams().size());
        Assertions.assertEquals("tenantId", source.getParams().get(0).getName());
        Assertions.assertNull(source.getPageIndex());
        Assertions.assertNull(source.getPageSize());
        Assertions.assertNull(source.getMaxRows());
    }

    @Test
    void extractsOnlyQueryBackedSourcesFromV1Template() {
        DatabaseIteratorVO iterator = new DatabaseIteratorVO();
        iterator.setDataSourceId("iterator-db");
        iterator.setSql("select * from t_order");

        PlainScriptVO script = new PlainScriptVO();
        script.setContent("tenant-a");
        ParamVO param = new ParamVO();
        param.setName("tenantId");
        param.setLanguage(script);

        JdbcReaderVO reader = new JdbcReaderVO();
        reader.setDataSourceId("reader-db");
        reader.setContent("select id from t_customer where tenant_id = :tenantId");

        ReadStageVO readStage = new ReadStageVO();
        readStage.setReaders(List.of(reader));
        readStage.setParams(List.of(param));

        FieldVO field = new FieldVO();
        field.setName("customer_lookup");
        field.setStages(List.of(readStage));

        TemplateVO template = new TemplateVO();
        template.setIterator(iterator);
        template.setFields(List.of(field));

        Map<String, QuerySourceVO> sources = V1QuerySourceExtractor.extract(template);

        Assertions.assertEquals(2, sources.size());
        Assertions.assertEquals("iterator-db", sources.get("iterator").getDataSourceId());
        Assertions.assertEquals("select * from t_order", sources.get("iterator").getSql());
        Assertions.assertEquals("reader-db", sources.get("customer_lookup").getDataSourceId());
        Assertions.assertEquals("select id from t_customer where tenant_id = :tenantId", sources.get("customer_lookup").getSql());
        Assertions.assertEquals(1, sources.get("customer_lookup").getParams().size());
    }

    @Test
    void convertsV1TemplateIntoQuerySourceDraft() {
        DatabaseIteratorVO iterator = new DatabaseIteratorVO();
        iterator.setDataSourceId("iterator-db");
        iterator.setSql("select * from t_order");

        JdbcReaderVO reader = new JdbcReaderVO();
        reader.setDataSourceId("reader-db");
        reader.setContent("select id from t_customer");

        ReadStageVO readStage = new ReadStageVO();
        readStage.setReaders(List.of(reader));

        FieldVO field = new FieldVO();
        field.setName("customer_lookup");
        field.setStages(List.of(readStage));

        WriteStageVO output = consoleSink();

        TemplateVO template = new TemplateVO();
        template.setId(1001L);
        template.setName("v1-query-source-convert");
        template.setIterator(iterator);
        template.setFields(List.of(field));
        template.setOutput(output);

        TemplateV2DraftVO draft = V1QuerySourceDraftConverter.convert(template);

        Assertions.assertNotNull(draft);
        Assertions.assertEquals(1001L, draft.getId());
        Assertions.assertEquals("v1-query-source-convert", draft.getName());
        Assertions.assertEquals(2, draft.getSources().size());
        Assertions.assertTrue(draft.getSources().get("iterator") instanceof QuerySourceVO);
        Assertions.assertTrue(draft.getSources().get("customer_lookup") instanceof QuerySourceVO);
        Assertions.assertNull(draft.getTransform());
        Assertions.assertNotNull(draft.getSinkExecutionPolicy());
        Assertions.assertEquals("FAIL_FAST", draft.getSinkExecutionPolicy().getMode());
        Assertions.assertNotNull(draft.getSink());
        Assertions.assertEquals(1, draft.getSink().getWriters().size());
    }

    @Test
    void extractsMultipleJdbcReadersFromOneFieldWithoutOverwriting() {
        JdbcReaderVO readerA = new JdbcReaderVO();
        readerA.setDataSourceId("reader-db-a");
        readerA.setContent("select id from t_customer_a");

        JdbcReaderVO readerB = new JdbcReaderVO();
        readerB.setDataSourceId("reader-db-b");
        readerB.setContent("select id from t_customer_b");

        ReadStageVO readStage = new ReadStageVO();
        readStage.setReaders(List.of(readerA, readerB));

        FieldVO field = new FieldVO();
        field.setName("customer_lookup");
        field.setStages(List.of(readStage));

        TemplateVO template = new TemplateVO();
        template.setFields(List.of(field));

        Map<String, QuerySourceVO> sources = V1QuerySourceExtractor.extract(template);

        Assertions.assertEquals(2, sources.size());
        Assertions.assertEquals("reader-db-a", sources.get("customer_lookup").getDataSourceId());
        Assertions.assertEquals("reader-db-b", sources.get("customer_lookup_2").getDataSourceId());
    }

    @Test
    void createsMinimalExecutableTransformForSingleQuerySourceDraft() {
        DatabaseIteratorVO iterator = new DatabaseIteratorVO();
        iterator.setDataSourceId("iterator-db");
        iterator.setSql("select * from t_order");

        TemplateVO template = new TemplateVO();
        template.setId(1002L);
        template.setName("single-source");
        template.setIterator(iterator);
        template.setOutput(consoleSink());

        TemplateV2DraftVO draft = V1QuerySourceDraftConverter.convert(template);

        Assertions.assertNotNull(draft);
        Assertions.assertNotNull(draft.getTransform());
        Assertions.assertInstanceOf(SqlTransformVO.class, draft.getTransform());
        Assertions.assertEquals("SELECT * FROM iterator", ((SqlTransformVO) draft.getTransform()).getSql());
        Assertions.assertNotNull(draft.getSinkExecutionPolicy());
        Assertions.assertEquals("FAIL_FAST", draft.getSinkExecutionPolicy().getMode());
    }

    private SqlTransformVO sql(String content) {
        var transform = new SqlTransformVO();
        transform.setType("sql");
        transform.setSql(content);
        return transform;
    }

    private WriteStageVO consoleSink() {
        var sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));
        return sink;
    }

    @JsonSubType("DYNAMIC_TEST_SOURCE")
    public static class DynamicTestSourceVO extends SourceVO {
        public DynamicTestSourceVO() {
            setType("dynamic_test_source");
        }

        public DynamicTestSourceVO(String message) {
            this();
            this.message = message;
        }

        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
