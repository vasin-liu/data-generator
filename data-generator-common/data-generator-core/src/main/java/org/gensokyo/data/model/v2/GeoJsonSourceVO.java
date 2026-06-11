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

/**
 * Reads GeoJSON {@code Feature} / {@code FeatureCollection} roots into Calcite rows (Phase B — real file source).
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@Getter
@Setter
@AutoService(SourceVO.class)
@JsonSubType("GEOJSON")
public class GeoJsonSourceVO extends SourceVO {

    public GeoJsonSourceVO() {
        setType("geojson");
    }

    /**
     * Classpath or filesystem location resolved via {@code org.gensokyo.data.geo.io.GeoResourceResolver}.
     */
    private String path;

    /**
     * Optional cap on emitted rows (applied after GeoJSON parse order).
     */
    private Long maxRows;

    private RowSchema schema;

    private GeoJsonSourceOutputVO output = new GeoJsonSourceOutputVO();
}
