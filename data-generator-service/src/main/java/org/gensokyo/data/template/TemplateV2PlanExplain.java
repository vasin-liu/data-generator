/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bounded Calcite plan summary and operator notes for a persisted Template V2 definition.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
public class TemplateV2PlanExplain implements Serializable {

    private String v2Sql;
    private String executionShape;
    private String effectiveExecutionMode;
    private String calciteValidation;
    private List<String> sourceSummaries = new ArrayList<>();
    private List<String> planFeatures = new ArrayList<>();
    private List<String> v1Hints = new ArrayList<>();
    private List<String> diffNotes = new ArrayList<>();
}
