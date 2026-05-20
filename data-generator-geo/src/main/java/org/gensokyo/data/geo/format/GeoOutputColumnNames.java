/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.format;

/**
 * Optional column name overrides for {@link GeoValueFormatter}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public class GeoOutputColumnNames {

    private String lat = "lat";
    private String lon = "lon";
    private String alt = "alt";
    private String geometry = "geometry";

    /**
     * Returns the latitude column name.
     *
     * @return column name
     */
    public String getLat() {
        return lat;
    }

    /**
     * Sets the latitude column name.
     *
     * @param lat column name
     */
    public void setLat(String lat) {
        if (lat != null && !lat.isBlank()) {
            this.lat = lat.strip();
        }
    }

    /**
     * Returns the longitude column name.
     *
     * @return column name
     */
    public String getLon() {
        return lon;
    }

    /**
     * Sets the longitude column name.
     *
     * @param lon column name
     */
    public void setLon(String lon) {
        if (lon != null && !lon.isBlank()) {
            this.lon = lon.strip();
        }
    }

    /**
     * Returns the altitude column name.
     *
     * @return column name
     */
    public String getAlt() {
        return alt;
    }

    /**
     * Sets the altitude column name.
     *
     * @param alt column name
     */
    public void setAlt(String alt) {
        if (alt != null && !alt.isBlank()) {
            this.alt = alt.strip();
        }
    }

    /**
     * Returns the geometry column name.
     *
     * @return column name
     */
    public String getGeometry() {
        return geometry;
    }

    /**
     * Sets the geometry column name.
     *
     * @param geometry column name
     */
    public void setGeometry(String geometry) {
        if (geometry != null && !geometry.isBlank()) {
            this.geometry = geometry.strip();
        }
    }
}
