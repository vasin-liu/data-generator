/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.migration.MigrationClassification;
import org.gensokyo.data.template.migration.MigrationCompareOptions;
import org.gensokyo.data.template.migration.MigrationComparisonReport;
import org.gensokyo.data.template.migration.MigrationInventoryEntry;
import org.gensokyo.data.template.migration.MigrationInventoryService;
import org.gensokyo.data.template.migration.MigrationReportWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Integration tests for {@code POST /template/migration/compare/{templateId}} using the real
 * {@link org.gensokyo.data.template.migration.PipelineTemplateRunExecutor} and embedded H2 JDBC.
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

    private static final String H2_URL =
            "jdbc:h2:mem:compare_migration_embedded;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    @Autowired
    private TemplateController templateController;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private MigrationInventoryService migrationInventoryService;

    @BeforeEach
    void seedEmbeddedJdbcData() throws Exception {
        try (Connection connection = DriverManager.getConnection(H2_URL, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("drop table if exists t_compare");
            statement.execute("create table t_compare(id bigint primary key)");
            statement.execute("insert into t_compare(id) values (1), (2), (3)");
        }
    }

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void compareMigrationUsesEmbeddedJdbcRunsAndWritesReport() {
        TemplatePO entity = new TemplatePO();
        entity.setId(93001L);
        entity.setName("compare-h2-embedded");
        entity.setContentYaml("""
                name: compare-h2-embedded
                iterator:
                  type: database
                  dataSourceId: compare-inline-ds
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
        MigrationComparisonReport report = result.getData();
        Assertions.assertNotNull(report);
        Assertions.assertEquals(
                MigrationClassification.EXACT,
                report.getClassification(),
                () -> "v1Rows=" + report.getV1RowCount()
                        + " v2Rows=" + report.getV2RowCount()
                        + " sampleRate=" + report.getSampleMatchRate()
                        + " warnings=" + report.getWarnings());
        Assertions.assertEquals(3, report.getV1RowCount());
        Assertions.assertEquals(3, report.getV2RowCount());
        Assertions.assertTrue(report.getReportPath().contains("db-" + entity.getId()));

        MigrationInventoryEntry entry = migrationInventoryService.findById("db-" + entity.getId()).orElseThrow();
        Assertions.assertEquals(MigrationClassification.EXACT, entry.getMigrationClass());
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
    }
}
