/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Dual-run migration compare result for a template (JSON API and markdown report source).
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationComparisonReport implements Serializable {

    private Long templateId;
    private long v1RowCount;
    private long v2RowCount;
    private int sampleSize;
    private double sampleMatchRate;
    private MigrationClassification classification = MigrationClassification.UNCLASSIFIED;
    private List<String> warnings = new ArrayList<>();
    /**
     * Promote guidance: {@code accept}, {@code accept_with_review}, or {@code reject}.
     */
    private String recommendation;
    /** Relative path to written markdown report, when persisted. */
    private String reportPath;

    /**
     * Sets {@link #recommendation} from {@link #classification} using {@link MigrationClassificationRules}.
     */
    public void applyRecommendationFromClassification() {
        this.recommendation = MigrationClassificationRules.recommendationFor(classification);
    }
}
