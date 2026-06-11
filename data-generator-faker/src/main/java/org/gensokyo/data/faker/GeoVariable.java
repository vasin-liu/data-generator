/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.faker;

import org.gensokyo.data.constant.Const;
import org.gensokyo.data.faker.geo.GeoSpelSupport;
import org.gensokyo.data.script.vars.Variable;

import java.util.Objects;

/**
 * Exposes synthetic geospatial SpEL helpers as variable {@link Const#SCRIPT_VAR_GEO}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public class GeoVariable implements Variable {

    private final GeoSpelSupport geoSpelSupport;

    /**
     * Builds a GEO SpEL namespace wrapper.
     */
    public GeoVariable() {
        this.geoSpelSupport = new GeoSpelSupport();
    }

    private GeoVariable(GeoSpelSupport geoSpelSupport) {
        this.geoSpelSupport = Objects.requireNonNull(geoSpelSupport);
    }

    /**
     * Creates a GEO variable wrapping the delegate.
     *
     * @param geoSpelSupport delegate
     * @return variable bean
     */
    public static GeoVariable of(GeoSpelSupport geoSpelSupport) {
        return new GeoVariable(geoSpelSupport);
    }

    @Override
    public String name() {
        return Const.SCRIPT_VAR_GEO;
    }

    @Override
    public Object value() {
        return geoSpelSupport;
    }
}
