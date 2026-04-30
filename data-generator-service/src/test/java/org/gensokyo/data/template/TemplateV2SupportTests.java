package org.gensokyo.data.template;

import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.vo.FieldVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
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
}
