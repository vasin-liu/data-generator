/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.V2SourceFactory;
import org.gensokyo.data.model.v2.GeoJsonSourceVO;
import org.gensokyo.data.model.v2.SourceVO;

/**
 * Factory for Template V2 {@code type: geojson} sources.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public class GeoJsonSourceFactory implements V2SourceFactory {

    /**
     * Returns {@code true} when the polymorphic source is a {@link GeoJsonSourceVO}.
     *
     * @param source candidate source configuration
     * @return whether this factory can build a row source for {@code source}
     */
    @Override
    public boolean supports(SourceVO source) {
        return source instanceof GeoJsonSourceVO;
    }

    /**
     * Creates a {@link RowSource} that materializes GeoJSON features into rows.
     *
     * @param name   logical source name
     * @param source concrete {@link GeoJsonSourceVO} configuration
     * @return materialized row source
     */
    @Override
    public RowSource create(String name, SourceVO source) {
        return new GeoJsonRowSource(name, (GeoJsonSourceVO) source);
    }
}
