/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.geo.GeoAssetResolver;
import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.geo.format.GeoFeatureRowFormatter;
import org.gensokyo.data.geo.format.GeoOutputColumnNames;
import org.gensokyo.data.geo.io.GeoFeature;
import org.gensokyo.data.geo.io.GeoJsonLoader;
import org.gensokyo.data.model.v2.GeoJsonSourceOutputVO;
import org.gensokyo.data.model.v2.GeoJsonSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Materializes GeoJSON {@code Feature} / {@code FeatureCollection} files as finite Calcite rows (Phase B).
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public class GeoJsonRowSource implements RowSource {

    private final String name;
    private final RowSchema schema;
    private final List<Row> rows;

    /**
     * Reads the configured GeoJSON path and builds rows plus an inferred schema when none is declared.
     *
     * @param name   logical source name
     * @param source GeoJSON source configuration
     */
    public GeoJsonRowSource(String name, GeoJsonSourceVO source) {
        this(name, source, null);
    }

    /**
     * Reads the configured GeoJSON location and builds rows plus an inferred schema when none is declared.
     *
     * @param name   logical source name
     * @param source GeoJSON source configuration
     * @param assets optional resolver for {@code asset:{uuid}} locations
     */
    public GeoJsonRowSource(String name, GeoJsonSourceVO source, GeoAssetResolver assets) {
        this.name = name;
        String location = GeoJsonLocationMapper.resolveLocation(name, source);
        GeoJsonSourceOutputVO output = source.getOutput() == null ? new GeoJsonSourceOutputVO() : source.getOutput();
        GeoOutputFormatKind format = output.getFormat() == null ? GeoOutputFormatKind.columns : output.getFormat();
        GeoOutputColumnNames columnNames = output.getColumnNames() == null ? new GeoOutputColumnNames() : output.getColumnNames();
        boolean includeProperties = output.isIncludeProperties();
        try {
            List<GeoFeature> features = GeoJsonLoader.loadFeatureCollection(location, assets);
            List<Map<String, Object>> generated = new ArrayList<>(features.size());
            for (GeoFeature feature : features) {
                generated.add(GeoFeatureRowFormatter.format(feature, format, columnNames, includeProperties));
            }
            long limit = source.getMaxRows() == null ? Long.MAX_VALUE : source.getMaxRows();
            if (limit < 0) {
                throw new IllegalArgumentException("GEOJSON source maxRows must be >= 0 for source [" + name + "]");
            }
            List<Map<String, Object>> limited = generated;
            if (limit != Long.MAX_VALUE) {
                int end = (int) Math.min((long) generated.size(), limit);
                limited = new ArrayList<>(generated.subList(0, end));
            }
            this.schema = source.getSchema() != null
                    ? source.getSchema()
                    : GeoRowSchemaSupport.schemaForGeoRows(format, columnNames, limited);
            this.rows = new ArrayList<>(limited.size());
            for (Map<String, Object> values : limited) {
                this.rows.add(new Row(new LinkedHashMap<>(values)));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read GEOJSON source [" + name + "] at [" + location + "]", e);
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public RowSchema schema() {
        return schema;
    }

    @Override
    public List<Row> rows() {
        return rows;
    }
}
