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
 * Reads geometries from a PostGIS table via generated {@code ST_*} SQL (Template V2 {@code POSTGIS} source).
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@Getter
@Setter
@AutoService(SourceVO.class)
@JsonSubType("POSTGIS")
public class PostGisQuerySourceVO extends SourceVO {

    public PostGisQuerySourceVO() {
        setType("postgis");
    }

    /**
     * Dynamic datasource id (same as {@link QuerySourceVO#getDataSourceId()}).
     */
    private String dataSourceId;

    private InlineDataSourceVO dataSource;

    /**
     * Target table name (simple identifier).
     */
    private String table;

    /**
     * Geometry column on the table (default {@code geom} when unset in SQL builder).
     */
    private String geometryColumn = "geom";

    /**
     * Non-geometry attribute columns to project (optional).
     */
    private List<String> attributes = new ArrayList<>();

    /**
     * Optional SQL predicate without the {@code WHERE} keyword (simple identifiers/literals only).
     */
    private String where;

    private Long maxRows;

    private RowSchema schema;

    private GeoJsonSourceOutputVO output = new GeoJsonSourceOutputVO();
}
