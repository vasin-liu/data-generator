/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.BuiltinClasspathTemplateCatalog;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.template.TemplateV2Validator;
import org.gensokyo.data.template.migration.MigrationClassification;
import org.gensokyo.data.template.migration.MigrationCompareOptions;
import org.gensokyo.data.template.migration.MigrationComparisonReport;
import org.gensokyo.data.template.migration.MigrationInventoryService;
import org.gensokyo.data.template.migration.MigrationReportWriter;
import org.gensokyo.data.template.migration.TemplateMigrationAnalysisDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * REST migration workflow over built-in classpath templates (R0-style evidence without a live staging server).
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
@Import(BuiltinClasspathTemplateMigrationWorkflowTests.WorkflowTestConfig.class)
class BuiltinClasspathTemplateMigrationWorkflowTests {

    private static final String H2_URL =
            "jdbc:h2:mem:compare_migration_embedded;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    private static final String SYNTHETIC_TEMPLATE = "demo/28_常量迭代器重复多次样例.yaml";
    private static final String JDBC_TEMPLATE = "tocc/parking/11_parking_online_space_record.yaml";

    @Autowired
    private TemplateController templateController;

    @Autowired
    private TemplateRepository templateRepository;

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
    void everyBuiltinTemplatePassesMigrationAnalyzeApi() {
        List<String> failures = new ArrayList<>();

        for (BuiltinClasspathTemplateCatalog.Fixture fixture : BuiltinClasspathTemplateCatalog.loadAll()) {
            try {
                TemplatePO entity = toEntity(fixture);
                templateRepository.saveAndFlush(entity);

                R<TemplateMigrationAnalysisDTO> result = templateController.analyzeMigration(entity.getId());
                if (!result.isSuccess()) {
                    failures.add(fixture.displayName() + ": " + result.getMessage());
                    continue;
                }
                TemplateMigrationAnalysisDTO analysis = result.getData();
                Assertions.assertNotNull(analysis.getRecommendedPath(), fixture.displayName());
            }
            catch (Exception e) {
                failures.add(fixture.displayName() + ": " + e.getMessage());
            }
            finally {
                templateRepository.deleteAll();
            }
        }
        Assertions.assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    @Test
    void stagingWorkflowSyntheticFromDemo28() {
        BuiltinClasspathTemplateCatalog.Fixture fixture =
                BuiltinClasspathTemplateCatalog.require(SYNTHETIC_TEMPLATE);
        TemplatePO entity = toEntity(fixture);
        templateRepository.saveAndFlush(entity);

        assertAnalyzeDraftCompare(
                entity,
                MigrationClassification.ADAPTED,
                MigrationClassification.EXACT,
                MigrationClassification.APPROXIMATE,
                MigrationClassification.BLOCKED,
                MigrationClassification.UNCLASSIFIED);
    }

    @Test
    void stagingWorkflowJdbcShapedFromParking11WithEmbeddedH2() {
        BuiltinClasspathTemplateCatalog.Fixture fixture =
                BuiltinClasspathTemplateCatalog.require(JDBC_TEMPLATE);
        String adaptedYaml = adaptParking11ForEmbeddedH2(fixture.yaml());
        TemplatePO entity = toEntity(fixture, adaptedYaml);
        templateRepository.saveAndFlush(entity);

        assertAnalyzeDraftCompare(
                entity,
                MigrationClassification.ADAPTED,
                MigrationClassification.EXACT,
                MigrationClassification.APPROXIMATE,
                MigrationClassification.UNCLASSIFIED);
    }

    private void assertAnalyzeDraftCompare(
            TemplatePO entity,
            MigrationClassification minAnalyzeClass,
            MigrationClassification... acceptableCompareClasses) {
        R<TemplateMigrationAnalysisDTO> analyze = templateController.analyzeMigration(entity.getId());
        Assertions.assertTrue(analyze.isSuccess(), analyze.getMessage());
        MigrationClassification suggested = analyze.getData().getSuggestedClass();
        Assertions.assertNotEquals(MigrationClassification.COMPATIBILITY_ONLY, suggested);
        if (minAnalyzeClass == MigrationClassification.ADAPTED) {
            Assertions.assertTrue(
                    suggested == MigrationClassification.ADAPTED
                            || suggested == MigrationClassification.EXACT
                            || suggested == MigrationClassification.APPROXIMATE
                            || suggested == MigrationClassification.UNCLASSIFIED,
                    () -> "unexpected analyze class " + suggested);
        }

        R<TemplateV2DraftVO> draft = templateController.buildMigrationDraft(entity.getId());
        Assertions.assertTrue(draft.isSuccess(), draft.getMessage());
        TemplateV2VO normalized = TemplateV2Normalizer.normalize(draft.getData());
        TemplateV2Validator.validate(normalized);

        R<MigrationComparisonReport> compare = templateController.compareMigration(
                entity.getId(),
                MigrationCompareOptions.defaults());
        Assertions.assertTrue(compare.isSuccess(), compare.getMessage());
        MigrationClassification compareClass = compare.getData().getClassification();
        boolean ok = false;
        for (MigrationClassification acceptable : acceptableCompareClasses) {
            if (compareClass == acceptable) {
                ok = true;
                break;
            }
        }
        Assertions.assertTrue(ok, () -> "compare class " + compareClass + " warnings=" + compare.getData().getWarnings());
        Assertions.assertNotNull(compare.getData().getReportPath());
    }

    private static TemplatePO toEntity(BuiltinClasspathTemplateCatalog.Fixture fixture) {
        return toEntity(fixture, fixture.yaml());
    }

    private static TemplatePO toEntity(BuiltinClasspathTemplateCatalog.Fixture fixture, String yaml) {
        TemplatePO entity = new TemplatePO();
        entity.setId(fixture.stableTemplateId());
        entity.setName(fixture.inventoryId());
        entity.setContentYaml(yaml);
        return entity;
    }

    /**
     * Rewires parking/11 database iterator to the embedded H2 table used by migration compare tests.
     */
    private static String adaptParking11ForEmbeddedH2(String yaml) {
        String adapted = yaml.replace("dataSourceId: 'tocc_parking'", "dataSourceId: compare-inline-ds");
        adapted = adapted.replace("dataSourceId: tocc_parking", "dataSourceId: compare-inline-ds");
        adapted = Pattern.compile("sql: >-.*?(?=\\n  pageIndex:)", Pattern.DOTALL)
                .matcher(adapted)
                .replaceFirst("sql: select id from t_compare order by id\n");
        return adapted;
    }

    @TestConfiguration
    static class WorkflowTestConfig {

        @Bean
        @Primary
        MigrationInventoryService testMigrationInventoryService() throws Exception {
            Path inventoryPath = Files.createTempFile("inventory-builtin", ".yaml");
            Files.writeString(inventoryPath, "version: 1\ntemplates: []\n");
            return new MigrationInventoryService(inventoryPath);
        }

        @Bean
        @Primary
        MigrationReportWriter testMigrationReportWriter() throws Exception {
            Path reportsDir = Files.createTempDirectory("migration-reports-builtin");
            return new MigrationReportWriter(reportsDir);
        }
    }
}
