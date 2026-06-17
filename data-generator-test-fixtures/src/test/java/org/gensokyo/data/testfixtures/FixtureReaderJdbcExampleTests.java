/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.testfixtures;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
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
 * Embedded JDBC reader example aligned with matrix row {@code reader-jdbc-basic}.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
class FixtureReaderJdbcExampleTests {

    private static final int EXPECTED_ROWS = 3;

    @Test
    void readsSeededJdbcRowsThroughSqlTransform() {
        // Prove the YAML fixture exists for Playwright/API reuse (D-19).
        Assertions.assertTrue(FixtureTemplates.load("reader-jdbc-basic").contains("reader-jdbc-basic"));

        DataSource dataSource = FixtureTestSupport.h2DataSource("fixture_reader_example");
        H2Seed.apply(dataSource, FixtureTestSupport.loadSql("reader-jdbc-basic"));

        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        TemplateV2VO template = readerTemplate();
        TemplateV2RunResult result = new TemplateV2Runner(readerRegistry(jdbcTemplate)).run(template);

        Assertions.assertEquals(EXPECTED_ROWS, result.getRows().size());
        Assertions.assertEquals(EXPECTED_ROWS, result.getMetrics().getTotalRowsRead());
    }

    private static TemplateV2VO readerTemplate() {
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql("SELECT id, name FROM fixture_customers ORDER BY id");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT id, name FROM customers");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("IN_MEMORY");
        executionPolicy.setMaxRowsInMemory(100);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("reader-jdbc-basic");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("customers", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }

    private static TemplateV2RuntimeRegistry readerRegistry(NamedParameterJdbcTemplate jdbcTemplate) {
        return new TemplateV2RuntimeRegistry(
                List.of(new QuerySourceFactory(jdbcTemplate)),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }
}
