/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.config;

import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.pipeline.DefaultDataPipelineTaskFactory;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.migration.MigrationBatchCompareService;
import org.gensokyo.data.template.migration.MigrationCompareService;
import org.gensokyo.data.template.migration.MigrationCompareWorkflow;
import org.gensokyo.data.template.migration.MigrationDraftService;
import org.gensokyo.data.template.migration.MigrationInventoryService;
import org.gensokyo.data.template.migration.MigrationPromoteService;
import org.gensokyo.data.template.migration.MigrationReportWriter;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.data.template.migration.PipelineTemplateRunExecutor;
import org.gensokyo.data.template.migration.TemplateRunExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.nio.file.Path;

/**
 * Spring beans for V1 migration inventory, dual-run compare, and report writing.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@Configuration
public class MigrationConfig {

    /**
     * Committed scenario inventory path relative to the repository root.
     *
     * @return inventory YAML path
     */
    @Bean
    @ConditionalOnMissingBean(MigrationInventoryService.class)
    public MigrationInventoryService migrationInventoryService() {
        return new MigrationInventoryService(Path.of("docs/migration/scenario-inventory.yaml"));
    }

    /**
     * Markdown report output directory under {@code docs/migration/reports/}.
     *
     * @return report writer
     */
    @Bean
    @ConditionalOnMissingBean(MigrationReportWriter.class)
    public MigrationReportWriter migrationReportWriter() {
        return new MigrationReportWriter(Path.of("docs/migration/reports"));
    }

    /**
     * Production dual-run executor (V1 pipeline + V2 runner).
     *
     * @param v1TaskFactory        V1 task factory
     * @param templateV2Runner     V2 runner
     * @param jdbcTemplate         JDBC template
     * @param jdbcEndpointResolver JDBC endpoint resolver
     * @return pipeline executor
     */
    @Bean
    @ConditionalOnMissingBean(TemplateRunExecutor.class)
    public TemplateRunExecutor templateRunExecutor(
            DefaultDataPipelineTaskFactory v1TaskFactory,
            TemplateV2Runner templateV2Runner,
            NamedParameterJdbcTemplate jdbcTemplate,
            RuntimeJdbcEndpointResolver jdbcEndpointResolver) {
        return new PipelineTemplateRunExecutor(
                v1TaskFactory, templateV2Runner, jdbcTemplate, jdbcEndpointResolver);
    }

    /**
     * Dual-run compare orchestration service.
     *
     * @param executor template run executor
     * @return compare service
     */
    @Bean
    @ConditionalOnMissingBean(MigrationCompareService.class)
    public MigrationCompareService migrationCompareService(TemplateRunExecutor executor) {
        return new MigrationCompareService(executor);
    }

    /**
     * Single-template compare workflow (dual-run, report, inventory update).
     *
     * @param yamlParser                YAML parser
     * @param migrationDraftService     draft builder
     * @param migrationCompareService   compare service
     * @param migrationReportWriter     report writer
     * @param migrationInventoryService inventory
     * @return compare workflow
     */
    @Bean
    @ConditionalOnMissingBean(MigrationCompareWorkflow.class)
    public MigrationCompareWorkflow migrationCompareWorkflow(
            YamlParser yamlParser,
            MigrationDraftService migrationDraftService,
            MigrationCompareService migrationCompareService,
            MigrationReportWriter migrationReportWriter,
            MigrationInventoryService migrationInventoryService) {
        return new MigrationCompareWorkflow(
                yamlParser,
                migrationDraftService,
                migrationCompareService,
                migrationReportWriter,
                migrationInventoryService);
    }

    /**
     * Batch dual-run compare over database-backed inventory rows.
     *
     * @param repository              template persistence
     * @param migrationInventoryService inventory
     * @param migrationCompareWorkflow  single-template workflow
     * @return batch compare service
     */
    @Bean
    @ConditionalOnMissingBean(MigrationBatchCompareService.class)
    public MigrationBatchCompareService migrationBatchCompareService(
            TemplateRepository repository,
            MigrationInventoryService migrationInventoryService,
            MigrationCompareWorkflow migrationCompareWorkflow) {
        return new MigrationBatchCompareService(
                repository, migrationInventoryService, migrationCompareWorkflow);
    }

    /**
     * Unified V1 → V2 draft builder (query-source and iterator paths).
     *
     * @return draft service
     */
    @Bean
    @ConditionalOnMissingBean(MigrationDraftService.class)
    public MigrationDraftService migrationDraftService() {
        return new MigrationDraftService();
    }

    /**
     * Promote workflow after dual-run review.
     *
     * @param repository                 template persistence
     * @param migrationDraftService      draft builder
     * @param migrationInventoryService  inventory
     * @param yamlParser                 YAML serializer
     * @return promote service
     */
    @Bean
    @ConditionalOnMissingBean(MigrationPromoteService.class)
    public MigrationPromoteService migrationPromoteService(
            TemplateRepository repository,
            MigrationDraftService migrationDraftService,
            MigrationInventoryService migrationInventoryService,
            YamlParser yamlParser) {
        return new MigrationPromoteService(
                repository, migrationDraftService, migrationInventoryService, yamlParser);
    }
}
