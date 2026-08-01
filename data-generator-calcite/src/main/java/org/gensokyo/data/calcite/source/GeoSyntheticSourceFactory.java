/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.V2SourceFactory;
import org.gensokyo.data.geo.GeoAssetResolver;
import org.gensokyo.data.model.v2.GeoSyntheticSourceVO;
import org.gensokyo.data.model.v2.SourceVO;

/**
 * Factory for Template V2 {@code type: geo_synthetic} sources (Phase 19 — GEO-01).
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
public class GeoSyntheticSourceFactory implements V2SourceFactory {

    private final GeoAssetResolver geoAssetResolver;

    /**
     * Creates a factory without metadata DB asset resolution (classpath/filesystem only).
     */
    public GeoSyntheticSourceFactory() {
        this(null);
    }

    /**
     * Creates a factory that resolves {@code asset:{uuid}} locations via the given resolver.
     *
     * @param geoAssetResolver optional metadata DB asset resolver
     */
    public GeoSyntheticSourceFactory(GeoAssetResolver geoAssetResolver) {
        this.geoAssetResolver = geoAssetResolver;
    }

    /**
     * Returns {@code true} when the polymorphic source is a {@link GeoSyntheticSourceVO}.
     *
     * @param source candidate source configuration
     * @return whether this factory can build a row source for {@code source}
     */
    @Override
    public boolean supports(SourceVO source) {
        return source instanceof GeoSyntheticSourceVO;
    }

    /**
     * Creates a {@link RowSource} that materializes synthetic geographic points into rows.
     *
     * @param name   logical source name
     * @param source concrete {@link GeoSyntheticSourceVO} configuration
     * @return materialized row source
     */
    @Override
    public RowSource create(String name, SourceVO source) {
        return new GeoSyntheticRowSource(name, (GeoSyntheticSourceVO) source, geoAssetResolver);
    }
}
