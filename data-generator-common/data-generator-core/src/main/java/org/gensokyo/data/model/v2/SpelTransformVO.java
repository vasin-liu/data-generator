/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;

import java.util.ArrayList;
import java.util.List;

/**
 * Non-SQL Template V2 transform: evaluates SpEL expressions per row to add or replace columns.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@Getter
@Setter
@AutoService(TransformVO.class)
@JsonSubType("SPEL")
public class SpelTransformVO extends TransformVO {

    /**
     * Creates a transform with {@code type} set to {@code spel}.
     */
    public SpelTransformVO() {
        setType("spel");
    }

    /**
     * Output columns to add or replace; each mapping is evaluated once per input row.
     */
    private List<SpelColumnMapping> columns = new ArrayList<>();
}
