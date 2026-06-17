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
import org.gensokyo.data.calcite.sink.JdbcSinkFactory;
import org.gensokyo.data.calcite.source.InlineRowsSourceFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.InlineRowsSourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedded JDBC writer example aligned with matrix row {@code writer-jdbc-basic}.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
class FixtureWriterJdbcExampleTests {

    private static final int EXPECTED_ROWS = 3;

    @Test
    void writesInlineRowsToJdbcSinkTable() {
        Assertions.assertTrue(FixtureTemplates.load("writer-jdbc-basic").contains("writer-jdbc-basic"));

        DataSource dataSource = FixtureTestSupport.h2DataSource("fixture_writer_example");
        H2Seed.apply(dataSource, FixtureTestSupport.loadSql("writer-jdbc-basic"));

        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        TemplateV2VO template = writerTemplate();
        TemplateV2RunResult result = new TemplateV2Runner(writerRegistry(jdbcTemplate)).run(template);

        Assertions.assertEquals(EXPECTED_ROWS, result.getRows().size());
        Long count = jdbcTemplate.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM fixture_export", Long.class);
        Assertions.assertEquals(EXPECTED_ROWS, count);
    }

    private static TemplateV2VO writerTemplate() {
        InlineRowsSourceVO source = new InlineRowsSourceVO();
        source.setRows(List.of(
                row("id", 1, "label", "alpha"),
                row("id", 2, "label", "beta"),
                row("id", 3, "label", "gamma")));

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT id, label FROM lookup");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("fixture_export");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("IN_MEMORY");
        executionPolicy.setMaxRowsInMemory(100);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("writer-jdbc-basic");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("lookup", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }

    private static Map<String, Object> row(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            map.put((String) keyValues[index], keyValues[index + 1]);
        }
        return map;
    }

    private static TemplateV2RuntimeRegistry writerRegistry(NamedParameterJdbcTemplate jdbcTemplate) {
        return new TemplateV2RuntimeRegistry(
                List.of(new InlineRowsSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new JdbcSinkFactory(jdbcTemplate, new NoopRuntimeJdbcEndpointResolver())));
    }
}
