/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.iterator;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.iterator.IteratorVO;

/**
 * Geospatial synthetic data iterator configuration.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@Getter
@Setter
@AutoService(IteratorVO.class)
@JsonSubType(value = "GEO")
public class GeoIteratorVO extends IteratorVO {

    private String mode;
    private String boundaryPath;
    private String networkPath;
    private int featureIndex;
    private boolean randomFeature;
    private int count;
    private long seed;
    private double minDistanceMeters;
    private GeoSampleConfigVO sample;
    private GeoOutputConfigVO output = new GeoOutputConfigVO();
}
