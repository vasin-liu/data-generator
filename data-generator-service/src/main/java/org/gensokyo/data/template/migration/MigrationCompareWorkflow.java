/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.template.TemplateV1Loader;
import org.gensokyo.data.yaml.YamlParser;

import java.util.Objects;

/**
 * Single-template dual-run compare: execute V1/V2, write markdown report, update inventory.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public class MigrationCompareWorkflow {

    private final MigrationV2CompareResolver v2Resolver;
    private final MigrationCompareService compareService;
    private final MigrationReportWriter reportWriter;
    private final MigrationInventoryService inventoryService;
    private final TemplateV1Loader v1Loader;

    /**
     * Creates the workflow.
     *
     * @param yamlParser         YAML parser
     * @param migrationDraftService draft builder for V2 resolution
     * @param compareService     dual-run compare
     * @param reportWriter       markdown report writer
     * @param inventoryService   scenario inventory
     */
    public MigrationCompareWorkflow(
            YamlParser yamlParser,
            MigrationDraftService migrationDraftService,
            MigrationCompareService compareService,
            MigrationReportWriter reportWriter,
            MigrationInventoryService inventoryService) {
        this.compareService = Objects.requireNonNull(compareService, "compareService");
        this.reportWriter = Objects.requireNonNull(reportWriter, "reportWriter");
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService");
        this.v2Resolver = new MigrationV2CompareResolver(yamlParser, migrationDraftService);
        this.v1Loader = new TemplateV1Loader(yamlParser);
    }

    /**
     * Runs dual-run compare for one persisted template and updates inventory.
     *
     * @param templateId persisted template id
     * @param entity     template row (must match {@code templateId})
     * @param opts       compare options; when {@code null}, defaults are used
     * @return report with {@link MigrationComparisonReport#getReportPath()} set
     */
    public MigrationComparisonReport compareAndPersist(
            Long templateId,
            TemplatePO entity,
            MigrationCompareOptions opts) {
        Objects.requireNonNull(entity, "entity");
        TemplateVO v1 = v1Loader.load(entity);
        TemplateV2VO v2 = v2Resolver.resolveForCompare(entity);
        MigrationCompareOptions options = opts != null ? opts : MigrationCompareOptions.defaults();
        MigrationComparisonReport report = compareService.compare(templateId, v1, v2, options);
        String reportPath = reportWriter.write(report);
        report.setReportPath(reportPath);
        inventoryService.updateCompareResult(templateId, report, reportPath);
        return report;
    }
}
