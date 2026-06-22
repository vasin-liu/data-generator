/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.testfixtures;

import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistry;
import org.gensokyo.data.calcite.sink.ConsoleSinkFactory;
import org.gensokyo.data.calcite.source.InlineRowsSourceFactory;
import org.gensokyo.data.calcite.transform.MaskTransformFactory;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.InlineRowsSourceVO;
import org.gensokyo.data.model.v2.MaskRuleVO;
import org.gensokyo.data.model.v2.MaskTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Embedded mask transform example aligned with matrix row {@code transform-mask} (D-03).
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
class FixtureTransformMaskExampleTests {

    @Test
    void masksColumnsPerStrategyAndDropsRawValues() {
        Assertions.assertTrue(FixtureTemplates.load("transform-mask").contains("transform-mask"));

        TemplateV2VO template = transformTemplate();
        TemplateV2RunResult result = new TemplateV2Runner(transformRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Map<String, Object> row = result.getRows().getFirst().values();

        // Each column is masked per its named strategy (D-03).
        Assertions.assertEquals("a****@example.com", row.get("email"));
        Assertions.assertEquals("*******8000", row.get("phone"));
        Assertions.assertEquals("************1111", row.get("card"));

        // PII-safe regression: the raw values must NOT survive into the output.
        Assertions.assertNotEquals("alice@example.com", row.get("email"));
        Assertions.assertNotEquals("13800138000", row.get("phone"));
        Assertions.assertNotEquals("4111111111111111", row.get("card"));
    }

    private static TemplateV2VO transformTemplate() {
        InlineRowsSourceVO source = new InlineRowsSourceVO();
        source.setRows(List.of(Map.of(
                "email", "alice@example.com",
                "phone", "13800138000",
                "card", "4111111111111111")));

        MaskTransformVO transform = new MaskTransformVO();
        transform.setRules(List.of(
                rule("email", "email"),
                rule("phone", "phone"),
                rule("card", "credit-card")));

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("IN_MEMORY");
        executionPolicy.setMaxRowsInMemory(100);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("transform-mask");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("input", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }

    private static MaskRuleVO rule(String column, String strategy) {
        MaskRuleVO rule = new MaskRuleVO();
        rule.setColumn(column);
        rule.setStrategy(strategy);
        return rule;
    }

    private static TemplateV2RuntimeRegistry transformRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new InlineRowsSourceFactory()),
                List.of(new MaskTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }
}
