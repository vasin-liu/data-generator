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
 * <p>The {@code path} field accepts {@code classpath:}, filesystem, or {@code asset:{uuid}} locations. The dedicated
 * {@code assetId} field normalizes to {@code asset:{uuid}} at runtime (GEO-10/D-01). When both {@code path} and
 * {@code assetId} are non-blank, mappers fail fast per D-02.</p>
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
     * Classpath, filesystem, or {@code asset:{uuid}} location resolved via {@code GeoResourceResolver}.
     */
    private String path;

    /**
     * Metadata DB geo asset UUID; normalized to {@code asset:{uuid}} at runtime (D-01).
     * Mutually exclusive with non-blank {@code path} per D-02.
     */
    private String assetId;

    /**
     * Optional cap on emitted rows (applied after GeoJSON parse order).
     */
    private Long maxRows;

    private RowSchema schema;

    private GeoJsonSourceOutputVO output = new GeoJsonSourceOutputVO();
}
