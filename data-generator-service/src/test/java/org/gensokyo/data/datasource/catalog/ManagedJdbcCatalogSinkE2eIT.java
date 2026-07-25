/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.datasource.DataSourceConfigService;
import org.gensokyo.data.model.v2.InlineRowsSourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.repository.DataSourceConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Closes DS-02 managed-catalog → JDBC sink rows proof (audit flow #1 / ROADMAP SC1).
 *
 * <p>Creates a managed JDBC datasource via {@link DataSourceConfigService#save}, runs an
 * in-process V2 template whose sink references that managed {@code dataSourceId} only
 * (no inline {@code dataSource}, plain INSERT), and asserts sink table row counts via
 * {@code COUNT(*)} on the managed pool.
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-07-25
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class ManagedJdbcCatalogSinkE2eIT {

    private static final String DS_NAME = "managed-jdbc-catalog-sink-e2e-ds";

    private static final String TABLE = "managed_e2e_sink";

    private static final String H2_URL =
            "jdbc:h2:mem:managed-jdbc-catalog-sink-e2e;MODE=MySQL;DB_CLOSE_DELAY=-1";

    private static final long SEEDED_ROW_COUNT = 2L;

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @Autowired
    private DataSourceConfigRepository dataSourceConfigRepository;

    @Autowired
    private DynamicRoutingDataSource dynamicRoutingDataSource;

    @Autowired
    private TemplateV2Runner templateV2Runner;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @BeforeEach
    void resetDatasourceRow() {
        dataSourceConfigRepository.findById(DS_NAME).ifPresent(dataSourceConfigRepository::delete);
        if (dynamicRoutingDataSource.getDataSources().containsKey(DS_NAME)) {
            dynamicRoutingDataSource.removeDataSource(DS_NAME);
        }
    }

    /**
     * Proves managed catalog save → unbound {@link TemplateV2Runner#run} → sink INSERT
     * rows are countable on the managed pool (D-01–D-08).
     */
    @Test
    void managedCatalogSinkInsert_writesRowsCountableOnManagedPool() {
        dataSourceConfigService.save(DS_NAME, H2_URL, "sa", "", null, "org.h2.Driver", null, null);

        try {
            DynamicDataSourceContextHolder.push(DS_NAME);
            namedParameterJdbcTemplate.getJdbcTemplate().execute(
                    "create table " + TABLE + " (id int primary key, label varchar(64))");
        } finally {
            DynamicDataSourceContextHolder.clear();
        }

        TemplateV2VO template = buildManagedSinkTemplate();
        JdbcWriterVO writer = (JdbcWriterVO) template.getSinks().getFirst().getWriters().getFirst();
        // Sink must resolve via managed catalog id only — inline dataSource would bypass DS-02 proof.
        assertThat(writer.getDataSourceId()).isEqualTo(DS_NAME);
        assertThat(writer.getDataSource()).isNull();

        // Keep WorkflowRunContext unbound so resolve stays logical catalog name (not snap:...).
        TemplateV2RunResult result = templateV2Runner.run(template);

        assertThat(countRows(DS_NAME, TABLE)).isEqualTo(SEEDED_ROW_COUNT);
        if (result.getMetrics() != null) {
            assertThat(result.getMetrics().getRowsWritten()).isEqualTo(SEEDED_ROW_COUNT);
        }
    }

    private TemplateV2VO buildManagedSinkTemplate() {
        InlineRowsSourceVO source = new InlineRowsSourceVO();
        source.setRows(List.of(
                row("id", 1, "label", "a"),
                row("id", 2, "label", "b")));

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT id, label FROM seed");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId(DS_NAME);
        writer.setTarget(TABLE);
        // Plain INSERT only — no upsert / upsertKeys / merge options (D-08).

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("managed-jdbc-catalog-sink-e2e");
        template.setSources(Map.of("seed", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }

    private long countRows(String dataSourceId, String table) {
        try {
            DynamicDataSourceContextHolder.push(dataSourceId);
            Long count = namedParameterJdbcTemplate.getJdbcTemplate()
                    .queryForObject("select count(*) from " + table, Long.class);
            return count == null ? 0L : count;
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    private static Map<String, Object> row(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            map.put((String) keyValues[index], keyValues[index + 1]);
        }
        return map;
    }
}
