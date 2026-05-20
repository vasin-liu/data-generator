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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregated migration inventory statistics for operator dashboards and staging checks.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
public class MigrationInventorySummary implements Serializable {

    private int totalTemplates;
    private int databaseTemplates;
    private int repositoryTemplates;
    private int withV2Draft;
    private int withCompareReport;
    private int compatibilityOnly;
    private int blocked;
    private int readyToPromote;
    private String inventoryPath;
    private Map<String, Integer> byClassification = new LinkedHashMap<>();
    private Map<String, Integer> byScenarioFamily = new LinkedHashMap<>();
    private Map<Integer, Integer> byWave = new LinkedHashMap<>();
}
