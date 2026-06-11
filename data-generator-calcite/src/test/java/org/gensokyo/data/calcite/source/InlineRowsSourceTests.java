/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistry;
import org.gensokyo.data.calcite.sink.ConsoleSinkFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.model.v2.InlineRowsSourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for template-embedded static row sources.
 *
 * @author Gensokyo
 * @since 2026-06-07
 */
class InlineRowsSourceTests {

    @Test
    void readsInlineRowsThroughSqlTransform() {
        InlineRowsSourceVO source = new InlineRowsSourceVO();
        source.setRows(List.of(
                row("id", 1, "code", "US", "label", "United States"),
                row("id", 2, "code", "UK", "label", "United Kingdom")));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("demo-v2-inline-rows");
        template.setSources(Map.of("lookup", source));
        template.setTransformers(List.of(sql("SELECT id, code, label FROM lookup WHERE code = 'US'")));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(runtimeRegistry()).run(template);

        assertThat(result.getRows()).hasSize(1);
        assertThat(result.getRows().getFirst().getString("label")).isEqualTo("United States");
    }

    @Test
    void rejectsEmptyInlineRows() {
        InlineRowsSourceVO source = new InlineRowsSourceVO();
        assertThatThrownBy(() -> new InlineRowsRowSource("lookup", source))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one row");
    }

    private static Map<String, Object> row(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            map.put((String) keyValues[index], keyValues[index + 1]);
        }
        return map;
    }

    private static SqlTransformVO sql(String sql) {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql(sql);
        return transform;
    }

    private static WriteStageVO consoleSink() {
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));
        return sink;
    }

    private static TemplateV2RuntimeRegistry runtimeRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new InlineRowsSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }
}
