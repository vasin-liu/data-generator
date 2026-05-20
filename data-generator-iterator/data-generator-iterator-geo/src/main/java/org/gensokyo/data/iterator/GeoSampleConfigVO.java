/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.iterator;

import lombok.Getter;
import lombok.Setter;

/**
 * Line sampling options for {@link GeoIteratorVO}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@Getter
@Setter
public class GeoSampleConfigVO {

    private String strategy;
    private double spacingMeters;
}
