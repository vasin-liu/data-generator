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
import org.gensokyo.data.calcite.transform.LookupTransformFactory;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.InlineRowsSourceVO;
import org.gensokyo.data.model.v2.LookupTransformVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedded lookup transform example aligned with matrix row {@code transform-lookup} (D-04).
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
class FixtureTransformLookupExampleTests {

    @Test
    void enrichesRowsWithProjectedLookupColumns() {
        Assertions.assertTrue(FixtureTemplates.load("transform-lookup").contains("transform-lookup"));

        TemplateV2VO template = transformTemplate();
        TemplateV2RunResult result = new TemplateV2Runner(transformRegistry()).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        // Each input row gains the projected dept_name by joining input.dept_id -> ref.dept_id (D-04).
        Map<Object, Object> byId = new LinkedHashMap<>();
        for (Row row : result.getRows()) {
            byId.put(row.get("id"), row.get("dept_name"));
        }
        Assertions.assertEquals("Eng", byId.get("1"));
        Assertions.assertEquals("Sales", byId.get("2"));
    }

    private static TemplateV2VO transformTemplate() {
        InlineRowsSourceVO input = new InlineRowsSourceVO();
        input.setRows(List.of(
                orderedRow("id", "1", "dept_id", "d1"),
                orderedRow("id", "2", "dept_id", "d2")));

        InlineRowsSourceVO ref = new InlineRowsSourceVO();
        ref.setRows(List.of(
                orderedRow("dept_id", "d1", "dept_name", "Eng"),
                orderedRow("dept_id", "d2", "dept_name", "Sales")));

        LookupTransformVO transform = new LookupTransformVO();
        transform.setSource("ref");
        transform.setLeftKey("dept_id");
        transform.setRightKey("dept_id");
        transform.setColumns(List.of("dept_name"));

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("IN_MEMORY");
        executionPolicy.setMaxRowsInMemory(100);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("transform-lookup");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("input", input, "ref", ref));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }

    private static Map<String, Object> orderedRow(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(k1, v1);
        row.put(k2, v2);
        return row;
    }

    private static TemplateV2RuntimeRegistry transformRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new InlineRowsSourceFactory()),
                List.of(new LookupTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }
}
