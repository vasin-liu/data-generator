/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateV2PlanExplain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * Tests for {@link TemplateV2ControlPlaneService}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class TemplateV2ControlPlaneServiceTests {

    @Autowired
    private TemplateV2ControlPlaneService service;

    @Autowired
    private TemplateRepository templateRepository;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void validateRejectsBlankSqlTransform() {
        TemplateV2DraftVO draft = minimalDraft();
        ((SqlTransformVO) draft.getTransform()).setSql("");

        TemplateV2ValidationResult result = service.validate(draft);

        Assertions.assertFalse(result.isValid());
        Assertions.assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("SQL")));
    }

    @Test
    void previewSeededV2IteratorReturnsRowsBoundedByMaxRows() {
        TemplatePO entity = new TemplatePO();
        entity.setId(97002L);
        entity.setName("control-plane-preview-iterator");
        entity.setContentYaml("""
                name: control-plane-preview-iterator
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: number
                      from: 1
                      to: 5
                      step: 1
                transform:
                  type: sql
                  sql: SELECT value FROM input
                sink:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        TemplateV2PreviewDTO preview = service.preview(entity.getId(), 2);

        Assertions.assertNotNull(preview);
        Assertions.assertNotNull(preview.getSchema());
        Assertions.assertFalse(preview.getRows().isEmpty());
        Assertions.assertTrue(preview.getRows().size() <= 2);
    }

    @Test
    void explainSeededV2IteratorHasNonEmptySourceSummaries() {
        TemplatePO entity = new TemplatePO();
        entity.setId(97001L);
        entity.setName("control-plane-explain-iterator");
        entity.setContentYaml("""
                name: control-plane-explain-iterator
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: number
                      from: 1
                      to: 3
                transform:
                  type: sql
                  sql: SELECT value FROM input
                sink:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        TemplateV2PlanExplain explain = service.explain(entity.getId());

        Assertions.assertNotNull(explain);
        Assertions.assertFalse(explain.getSourceSummaries().isEmpty());
        Assertions.assertTrue(explain.getSourceSummaries().stream()
                .anyMatch(s -> s.contains("IteratorSourceVO")));
    }

    private static TemplateV2DraftVO minimalDraft() {
        var draft = new TemplateV2DraftVO();
        draft.setName("demo");
        draft.setSources(Map.of("input", new IteratorSourceVO()));
        draft.setTransform(sql("SELECT value FROM input"));
        draft.setSink(consoleSink());
        return draft;
    }

    private static SqlTransformVO sql(String content) {
        var transform = new SqlTransformVO();
        transform.setType("sql");
        transform.setSql(content);
        return transform;
    }

    private static WriteStageVO consoleSink() {
        var sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));
        return sink;
    }
}
