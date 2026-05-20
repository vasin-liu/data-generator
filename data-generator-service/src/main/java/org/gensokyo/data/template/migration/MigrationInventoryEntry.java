/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * One row in the migration scenario inventory ({@code scenario-inventory.yaml}).
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@Getter
@Setter
@NoArgsConstructor
public class MigrationInventoryEntry {

    private String id;
    private String name;
    /** {@code database} or {@code repository}. */
    private String origin;
    private String scenarioFamily;
    private MigrationClassification migrationClass = MigrationClassification.UNCLASSIFIED;
    private Integer wave;
    private List<String> blockers = new ArrayList<>();
    private Long dbTemplateId;
    private boolean v2DraftPresent;
    private String lastCompareReportPath;
    private String notes;
    /** Business approval recorded before production promote (P3 gate). */
    private boolean businessSignoffApproved;
    private String businessSignoffBy;
    /** ISO-8601 timestamp when sign-off was recorded. */
    private String businessSignoffAt;
}
