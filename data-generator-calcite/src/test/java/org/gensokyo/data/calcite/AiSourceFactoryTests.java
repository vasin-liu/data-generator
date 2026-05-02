package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.AiSourceVO;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class AiSourceFactoryTests {

    @Test
    void materializesInlineRowsWithDeclaredSchema() {
        AiSourceVO source = aiSource("INLINE", Map.of("rows", List.of(
                Map.of("name", "alpha", "score", 10),
                Map.of("name", "beta", "score", 20)
        )));
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(
                new ColumnDef("name", "VARCHAR", false),
                new ColumnDef("score", "BIGINT", false)
        ));
        source.setSchema(schema);

        RowSource rowSource = new AiSourceFactory().create("ai_seed", source);

        Assertions.assertEquals("ai_seed", rowSource.name());
        Assertions.assertEquals(schema, rowSource.schema());
        Assertions.assertEquals(2, rowSource.rows().size());
        Assertions.assertEquals("alpha", rowSource.rows().getFirst().getString("name"));
        Assertions.assertEquals("20", rowSource.rows().get(1).getString("score"));
    }

    @Test
    void materializesEchoPromptAsDefaultContentColumn() {
        AiSourceVO source = aiSource("ECHO", Map.of());
        source.setPrompt("generate row");

        RowSource rowSource = new AiSourceFactory().create("ai_echo", source);

        Assertions.assertEquals(1, rowSource.rows().size());
        Assertions.assertEquals("generate row", rowSource.rows().getFirst().getString("content"));
        Assertions.assertTrue(rowSource.schema().contains("content"));
    }

    @Test
    void runnerCanQueryAiSourceRows() {
        AiSourceVO source = aiSource("INLINE", Map.of("rows", List.of(
                Map.of("name", "alpha", "score", 10),
                Map.of("name", "beta", "score", 20)
        )));
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(
                new ColumnDef("name", "VARCHAR", false),
                new ColumnDef("score", "BIGINT", false)
        ));
        source.setSchema(schema);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("ai-v2-source");
        template.setSources(Map.of("ai_seed", source));
        template.setTransformers(List.of(sql("SELECT name FROM ai_seed WHERE score >= 20")));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals("beta", result.getRows().getFirst().getString("name"));
    }

    @Test
    void rejectsMissingProviderType() {
        AiSourceVO source = new AiSourceVO();
        source.setProvider(new AiProviderVO());

        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new AiSourceFactory().create("ai_seed", source));

        Assertions.assertTrue(failure.getMessage().contains("provider type"));
    }

    @Test
    void reportsExternalProviderBridgeAsPending() {
        AiSourceVO source = aiSource("OLLAMA", Map.of());

        UnsupportedOperationException failure = Assertions.assertThrows(UnsupportedOperationException.class,
                () -> new AiSourceFactory().create("ai_seed", source));

        Assertions.assertTrue(failure.getMessage().contains("external AI runtime bridge"));
    }

    private AiSourceVO aiSource(String providerType, Map<String, Object> options) {
        AiProviderVO provider = new AiProviderVO();
        provider.setType(providerType);
        provider.setOptions(options);
        AiSourceVO source = new AiSourceVO();
        source.setProvider(provider);
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

    private TemplateV2RuntimeRegistry defaultRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new AiSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory())
        );
    }
}
