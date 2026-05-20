/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TemplateRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Batch dual-run compare for database-backed templates listed in the migration inventory.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public class MigrationBatchCompareService {

    private final TemplateRepository repository;
    private final MigrationInventoryService inventoryService;
    private final MigrationCompareWorkflow compareWorkflow;

    /**
     * Creates the batch compare service.
     *
     * @param repository        template persistence
     * @param inventoryService  scenario inventory
     * @param compareWorkflow   single-template compare workflow
     */
    public MigrationBatchCompareService(
            TemplateRepository repository,
            MigrationInventoryService inventoryService,
            MigrationCompareWorkflow compareWorkflow) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService");
        this.compareWorkflow = Objects.requireNonNull(compareWorkflow, "compareWorkflow");
    }

    /**
     * Runs dual-run compare for inventory rows with {@code dbTemplateId} (up to {@code maxTemplates}).
     *
     * @param options batch options; when {@code null}, defaults are used
     * @return per-template outcomes and aggregate counts
     */
    public MigrationBatchCompareResult runBatch(MigrationBatchCompareOptions options) {
        MigrationBatchCompareOptions batchOptions =
                options != null ? options : MigrationBatchCompareOptions.defaults();
        MigrationBatchCompareResult result = new MigrationBatchCompareResult();

        if (batchOptions.isRefreshInventoryFirst()) {
            result.setInventoryRefresh(inventoryService.refreshFromRepository(repository));
        }

        List<MigrationInventoryEntry> candidates = selectDbCandidates();
        int processed = 0;
        for (MigrationInventoryEntry entry : candidates) {
            if (processed >= batchOptions.getMaxTemplates()) {
                break;
            }
            processed++;
            MigrationBatchCompareItemResult item = compareOne(entry, batchOptions);
            result.getItems().add(item);
            switch (item.getStatus()) {
                case SUCCESS -> result.recordSuccess();
                case SKIPPED -> result.recordSkipped();
                case FAILED -> result.recordFailed();
            }
        }
        return result;
    }

    private MigrationBatchCompareItemResult compareOne(
            MigrationInventoryEntry entry,
            MigrationBatchCompareOptions batchOptions) {
        String inventoryId = entry.getId();
        Long templateId = entry.getDbTemplateId();
        if (templateId == null) {
            return MigrationBatchCompareItemResult.skipped(inventoryId, null, "Not a database-backed inventory row");
        }
        if (batchOptions.isSkipCompatibilityOnly()
                && entry.getMigrationClass() == MigrationClassification.COMPATIBILITY_ONLY) {
            return MigrationBatchCompareItemResult.skipped(
                    inventoryId, templateId, "COMPATIBILITY_ONLY — see compatibility-only-templates.md");
        }

        Optional<TemplatePO> entityOpt = repository.findById(templateId);
        if (entityOpt.isEmpty()) {
            return MigrationBatchCompareItemResult.failed(
                    inventoryId, templateId, "Template not found in database: " + templateId);
        }

        try {
            MigrationComparisonReport report = compareWorkflow.compareAndPersist(
                    templateId,
                    entityOpt.get(),
                    batchOptions.getCompareOptions());
            return MigrationBatchCompareItemResult.success(inventoryId, templateId, report);
        }
        catch (RuntimeException e) {
            return MigrationBatchCompareItemResult.failed(inventoryId, templateId, e.getMessage());
        }
    }

    private List<MigrationInventoryEntry> selectDbCandidates() {
        List<MigrationInventoryEntry> candidates = new ArrayList<>();
        for (MigrationInventoryEntry entry : inventoryService.listAll()) {
            if (entry.getDbTemplateId() == null) {
                continue;
            }
            if (!"database".equalsIgnoreCase(entry.getOrigin())) {
                continue;
            }
            candidates.add(entry);
        }
        return candidates;
    }
}
