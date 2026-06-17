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
import org.gensokyo.data.calcite.source.QuerySourceFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Embedded SQL transform example aligned with matrix row {@code transform-sql-basic}.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
class FixtureTransformSqlExampleTests {

    @Test
    void aggregatesSeededOrdersThroughSqlTransform() {
        Assertions.assertTrue(FixtureTemplates.load("transform-sql-basic").contains("transform-sql-basic"));

        DataSource dataSource = FixtureTestSupport.h2DataSource("fixture_transform_example");
        H2Seed.apply(dataSource, FixtureTestSupport.loadSql("transform-sql-basic"));

        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        TemplateV2VO template = transformTemplate();
        TemplateV2RunResult result = new TemplateV2Runner(transformRegistry(jdbcTemplate)).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Object orderCount = result.getRows().getFirst().get("order_count");
        Object totalAmount = result.getRows().getFirst().get("total_amount");
        Assertions.assertEquals(3L, ((Number) orderCount).longValue());
        Assertions.assertEquals(40.0d, ((Number) totalAmount).doubleValue(), 0.001d);
    }

    private static TemplateV2VO transformTemplate() {
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql("SELECT id, amount FROM fixture_orders ORDER BY id");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT COUNT(*) AS order_count, SUM(amount) AS total_amount FROM orders");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("IN_MEMORY");
        executionPolicy.setMaxRowsInMemory(100);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("transform-sql-basic");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("orders", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }

    private static TemplateV2RuntimeRegistry transformRegistry(NamedParameterJdbcTemplate jdbcTemplate) {
        return new TemplateV2RuntimeRegistry(
                List.of(new QuerySourceFactory(jdbcTemplate)),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }
}
