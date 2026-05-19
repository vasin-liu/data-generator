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
 * Full-template V1 migration analysis returned by {@link V1TemplateMigrationAnalyzer}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateMigrationAnalysisDTO implements Serializable {

    private MigrationClassification suggestedClass = MigrationClassification.UNCLASSIFIED;
    private String scenarioFamily;
    private Integer wave;
    private List<String> blockers = new ArrayList<>();
    /**
     * Recommended migration path: {@code sql}, {@code sql_udf}, {@code non_sql}, {@code custom},
     * or {@code compatibility_only}.
     */
    private String recommendedPath;
    private List<String> warnings = new ArrayList<>();
}
