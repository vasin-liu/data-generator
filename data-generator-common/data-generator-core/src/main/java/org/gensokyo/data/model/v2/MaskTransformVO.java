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
 * Built-in Template V2 transform that masks column values in place using predefined named strategies.
 * It is the no-config complement to the Phase 3 {@code mask-email} UDF sample; custom masking still
 * goes through UDFs.
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
@Getter
@Setter
@AutoService(TransformVO.class)
@JsonSubType("MASK")
public class MaskTransformVO extends TransformVO {

    /**
     * Creates a transform with {@code type} set to {@code mask}.
     */
    public MaskTransformVO() {
        setType("mask");
    }

    /**
     * Masking rules applied per input row; each rule redacts one column with a named strategy.
     */
    private List<MaskRuleVO> rules = new ArrayList<>();
}
