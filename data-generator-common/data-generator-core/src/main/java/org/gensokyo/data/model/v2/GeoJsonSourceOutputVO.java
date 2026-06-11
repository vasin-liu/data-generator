/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.geo.format.GeoOutputColumnNames;

/**
 * Output formatting for Template V2 {@code GEOJSON} file sources (mirrors GEO iterator output knobs).
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@Getter
@Setter
public class GeoJsonSourceOutputVO {

    private GeoOutputFormatKind format = GeoOutputFormatKind.columns;
    private GeoOutputColumnNames columnNames = new GeoOutputColumnNames();
    private boolean includeProperties;
}
