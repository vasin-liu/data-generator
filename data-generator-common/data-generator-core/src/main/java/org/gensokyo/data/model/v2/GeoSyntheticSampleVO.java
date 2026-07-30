/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.Setter;

/**
 * Line sampling options for Template V2 {@code geo_synthetic} sources (nested {@code sample} block per D-01).
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
@Getter
@Setter
public class GeoSyntheticSampleVO {

    private String strategy;
    private double spacingMeters;
}
