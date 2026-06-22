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
import org.gensokyo.data.calcite.transform.JsonTransformFactory;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.InlineRowsSourceVO;
import org.gensokyo.data.model.v2.JsonTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Embedded JSON transform example aligned with matrix row {@code transform-json} (D-02).
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
class FixtureTransformJsonExampleTests {

    @Test
    void flattensNestedJsonColumnThroughJsonTransform() {
        Assertions.assertTrue(FixtureTemplates.load("transform-json").contains("transform-json"));

        TemplateV2VO template = transformTemplate();
        TemplateV2RunResult result = new TemplateV2Runner(transformRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Map<String, Object> row = result.getRows().getFirst().values();
        // Flatten expands addr.city / addr.zip into separate columns (lowercased) per D-02.
        Assertions.assertEquals("sh", row.get("addr.city"));
        Assertions.assertEquals("200000", row.get("addr.zip"));
        Assertions.assertEquals("alice", row.get("name"));
    }

    private static TemplateV2VO transformTemplate() {
        InlineRowsSourceVO source = new InlineRowsSourceVO();
        source.setRows(List.of(Map.of("payload", "{\"name\":\"alice\",\"addr\":{\"city\":\"sh\",\"zip\":\"200000\"}}")));

        JsonTransformVO transform = new JsonTransformVO();
        transform.setSourceColumn("payload");
        transform.setFlatten(true);
        transform.setSeparator(".");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("IN_MEMORY");
        executionPolicy.setMaxRowsInMemory(100);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("transform-json");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("input", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }

    private static TemplateV2RuntimeRegistry transformRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new InlineRowsSourceFactory()),
                List.of(new JsonTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }
}
