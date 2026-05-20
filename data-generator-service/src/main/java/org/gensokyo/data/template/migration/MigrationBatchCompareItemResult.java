/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Per-template outcome within a batch dual-run compare.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
public class MigrationBatchCompareItemResult implements Serializable {

    /** Outcome status for one inventory row. */
    public enum Status {
        /** Compare completed and report written. */
        SUCCESS,
        /** Row intentionally not compared (e.g. compatibility-only). */
        SKIPPED,
        /** Compare failed (missing template, parse error, pipeline error). */
        FAILED
    }

    private String inventoryId;
    private Long templateId;
    private Status status;
    private MigrationClassification classification;
    private String reportPath;
    private String message;

    /**
     * Builds a successful item result.
     *
     * @param inventoryId inventory row id
     * @param templateId  database template id
     * @param report      compare report
     * @return item result
     */
    public static MigrationBatchCompareItemResult success(
            String inventoryId, Long templateId, MigrationComparisonReport report) {
        MigrationBatchCompareItemResult item = new MigrationBatchCompareItemResult();
        item.setInventoryId(inventoryId);
        item.setTemplateId(templateId);
        item.setStatus(Status.SUCCESS);
        item.setClassification(report.getClassification());
        item.setReportPath(report.getReportPath());
        return item;
    }

    /**
     * Builds a skipped item result.
     *
     * @param inventoryId inventory row id
     * @param templateId  database template id, may be null
     * @param message     skip reason
     * @return item result
     */
    public static MigrationBatchCompareItemResult skipped(String inventoryId, Long templateId, String message) {
        MigrationBatchCompareItemResult item = new MigrationBatchCompareItemResult();
        item.setInventoryId(inventoryId);
        item.setTemplateId(templateId);
        item.setStatus(Status.SKIPPED);
        item.setMessage(message);
        return item;
    }

    /**
     * Builds a failed item result.
     *
     * @param inventoryId inventory row id
     * @param templateId  database template id, may be null
     * @param message     failure message
     * @return item result
     */
    public static MigrationBatchCompareItemResult failed(String inventoryId, Long templateId, String message) {
        MigrationBatchCompareItemResult item = new MigrationBatchCompareItemResult();
        item.setInventoryId(inventoryId);
        item.setTemplateId(templateId);
        item.setStatus(Status.FAILED);
        item.setMessage(message);
        return item;
    }
}
