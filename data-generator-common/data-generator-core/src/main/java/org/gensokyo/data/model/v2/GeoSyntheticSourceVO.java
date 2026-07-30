/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import com.google.auto.service.AutoService;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;

/**
 * Template V2 source that synthesizes geographic point rows via {@code GeoSyntheticGenerator} (Phase 19 — configuration model only).
 *
 * <p>Path fields ({@code boundaryPath}, {@code networkPath}) resolve through
 * {@code org.gensokyo.data.geo.io.GeoResourceResolver} in downstream {@code GeoSyntheticRowSource} (GEO-03).</p>
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
@Getter
@Setter
@AutoService(SourceVO.class)
@JsonSubType("GEO_SYNTHETIC")
public class GeoSyntheticSourceVO extends SourceVO {

    /**
     * Registers the runtime Template V2 type string {@code geo_synthetic} per D-02.
     */
    public GeoSyntheticSourceVO() {
        setType("geo_synthetic");
    }

    private String mode;
    private int count;
    // D-03: omitted YAML seed deserializes as deterministic zero.
    private long seed = 0L;
    private double minDistanceMeters;
    /**
     * Classpath or filesystem location for boundary GeoJSON; resolved via {@code GeoResourceResolver}.
     */
    private String boundaryPath;
    /**
     * Classpath or filesystem location for line/network GeoJSON; resolved via {@code GeoResourceResolver}.
     */
    private String networkPath;
    private int featureIndex;
    private boolean randomFeature;
    private GeoSyntheticSampleVO sample;
    private GeoSyntheticSourceOutputVO output = new GeoSyntheticSourceOutputVO();
    /**
     * Bounding box as {@code [minLon, minLat, maxLon, maxLat]} per D-01.
     */
    private List<Double> bbox;
    /**
     * Circle center as {@code [lon, lat]} per D-01.
     */
    private List<Double> center;
    private double radiusMeters;
    private RowSchema schema;
}
