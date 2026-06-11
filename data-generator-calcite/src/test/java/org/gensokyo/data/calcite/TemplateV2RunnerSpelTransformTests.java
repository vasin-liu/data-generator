/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistryFactory;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.SpelColumnMapping;
import org.gensokyo.data.model.v2.SpelTransformVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * End-to-end Template V2 runner tests for iterator → SQL → SpEL transform chains.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
class TemplateV2RunnerSpelTransformTests {

    @Test
    void runsIteratorSqlThenSpelTransformChain() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("spel-chain");
        template.setSources(Map.of("seed", numberSource(1, 3, 1)));
        template.setTransformers(List.of(
                sql("SELECT value AS id FROM seed"),
                spel(mapping("label", "#row['id'] + '-1'"))
        ));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(3, result.getRows().size());
        Assertions.assertEquals("1-1", result.getRows().get(0).getString("label"));
        Assertions.assertEquals("2-1", result.getRows().get(1).getString("label"));
        Assertions.assertEquals("3-1", result.getRows().get(2).getString("label"));
    }

    private static org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistry defaultRegistry() {
        return new TemplateV2RuntimeRegistryFactory().createDefault();
    }

    private static IteratorSourceVO numberSource(long from, long to, int step) {
        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(from);
        iterator.setTo(to);
        iterator.setStep(step);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);
        return source;
    }

    private static SqlTransformVO sql(String sql) {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql(sql);
        return transform;
    }

    private static SpelTransformVO spel(SpelColumnMapping... columns) {
        SpelTransformVO transform = new SpelTransformVO();
        transform.setColumns(List.of(columns));
        return transform;
    }

    private static SpelColumnMapping mapping(String name, String expression) {
        SpelColumnMapping mapping = new SpelColumnMapping();
        mapping.setName(name);
        mapping.setExpression(expression);
        return mapping;
    }

    private static WriteStageVO consoleSink() {
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));
        return sink;
    }
}
