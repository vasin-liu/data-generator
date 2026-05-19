/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.migration.MigrationClassification;
import org.gensokyo.data.template.migration.MigrationCompareOptions;
import org.gensokyo.data.template.migration.MigrationCompareService;
import org.gensokyo.data.template.migration.MigrationComparisonReport;
import org.gensokyo.data.template.migration.MigrationInventoryEntry;
import org.gensokyo.data.template.migration.MigrationInventoryService;
import org.gensokyo.data.template.migration.MigrationReportWriter;
import org.gensokyo.data.template.migration.RunOutcome;
import org.gensokyo.data.template.migration.TemplateRunExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Integration tests for {@code POST /template/migration/compare/{templateId}}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
@Import(TemplateControllerMigrationCompareTests.CompareTestConfig.class)
class TemplateControllerMigrationCompareTests {

    @Autowired
    private TemplateController templateController;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private MigrationInventoryService migrationInventoryService;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void compareMigrationWritesReportAndUpdatesInventory() throws Exception {
        String dataSourceId = "compare-inline-ds";
        try {
            DynamicDataSourceContextHolder.push(dataSourceId);
            namedParameterJdbcTemplate.getJdbcTemplate().execute("drop table if exists t_compare");
            namedParameterJdbcTemplate.getJdbcTemplate().execute(
                    "create table t_compare(id bigint primary key)");
            namedParameterJdbcTemplate.getJdbcTemplate().execute(
                    "insert into t_compare(id) values (1), (2), (3)");
        }
        finally {
            DynamicDataSourceContextHolder.clear();
        }

        TemplatePO entity = new TemplatePO();
        entity.setId(93001L);
        entity.setName("compare-h2-fixture");
        entity.setContentYaml("""
                name: compare-h2-fixture
                iterator:
                  type: database
                  dataSource:
                    name: compare-inline-ds
                    type: jdbc
                    url: jdbc:h2:mem:compare-inline-ds;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
                    username: sa
                    password:
                    driverClassName: org.h2.Driver
                  sql: select id from t_compare order by id
                  maxRows: 100
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<MigrationComparisonReport> result = templateController.compareMigration(
                entity.getId(),
                MigrationCompareOptions.defaults());

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("Compare completed", result.getMessage());
        Assertions.assertNotNull(result.getData());
        Assertions.assertEquals(MigrationClassification.EXACT, result.getData().getClassification());
        Assertions.assertNotNull(result.getData().getReportPath());
        Assertions.assertTrue(result.getData().getReportPath().contains("db-" + entity.getId()));

        MigrationInventoryEntry entry = migrationInventoryService.findById("db-" + entity.getId()).orElseThrow();
        Assertions.assertEquals(MigrationClassification.EXACT, entry.getMigrationClass());
        Assertions.assertEquals(result.getData().getReportPath(), entry.getLastCompareReportPath());
    }

    @TestConfiguration
    static class CompareTestConfig {

        @Bean
        @Primary
        MigrationInventoryService testMigrationInventoryService() throws Exception {
            Path inventoryPath = Files.createTempFile("inventory-compare", ".yaml");
            Files.writeString(inventoryPath, "version: 1\ntemplates: []\n");
            return new MigrationInventoryService(inventoryPath);
        }

        @Bean
        @Primary
        MigrationReportWriter testMigrationReportWriter() throws Exception {
            Path reportsDir = Files.createTempDirectory("migration-reports");
            return new MigrationReportWriter(reportsDir);
        }

        @Bean
        @Primary
        TemplateRunExecutor matchingCompareExecutor() {
            return new MatchingCompareExecutor();
        }

        @Bean
        @Primary
        MigrationCompareService migrationCompareService(TemplateRunExecutor executor) {
            return new MigrationCompareService(executor);
        }
    }

    /**
     * Returns identical JDBC-shaped rows for V1 and V2 compare (deterministic EXACT).
     */
    static final class MatchingCompareExecutor implements TemplateRunExecutor {

        @Override
        public RunOutcome runV1(TemplateVO v1, Map<String, Object> params, MigrationCompareOptions options) {
            return matchingOutcome();
        }

        @Override
        public RunOutcome runV2(TemplateV2VO v2, Map<String, Object> params, MigrationCompareOptions options) {
            return matchingOutcome();
        }

        private static RunOutcome matchingOutcome() {
            List<Map<String, Object>> rows = List.of(
                    row(1L),
                    row(2L),
                    row(3L));
            return new RunOutcome(rows.size(), rows);
        }

        private static Map<String, Object> row(Long id) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            return map;
        }
    }
}
