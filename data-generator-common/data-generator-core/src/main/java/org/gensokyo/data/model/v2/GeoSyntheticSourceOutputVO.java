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
 * Output formatting for Template V2 {@code geo_synthetic} sources (same knobs as {@link GeoJsonSourceOutputVO}, independent type per D-09).
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
@Getter
@Setter
public class GeoSyntheticSourceOutputVO {

    // Parity with GeoJsonSourceOutputVO output knobs — kept separate so synthetic and read-only geojson types do not couple.
    private GeoOutputFormatKind format = GeoOutputFormatKind.columns;
    private GeoOutputColumnNames columnNames = new GeoOutputColumnNames();
    private boolean includeProperties;
}
