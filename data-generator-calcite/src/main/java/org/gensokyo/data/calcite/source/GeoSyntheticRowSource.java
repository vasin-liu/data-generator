/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.geo.GeoGenerationMode;
import org.gensokyo.data.geo.GeoGenerationRequest;
import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.geo.GeoSyntheticGenerator;
import org.gensokyo.data.geo.format.GeoOutputColumnNames;
import org.gensokyo.data.model.v2.GeoSyntheticSourceOutputVO;
import org.gensokyo.data.model.v2.GeoSyntheticSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Materializes synthetic geographic points as finite Calcite rows (Phase 19 — D-12 eager constructor).
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
public class GeoSyntheticRowSource implements RowSource {

    private final String name;
    private final RowSchema schema;
    private final List<Row> rows;

    /**
     * Maps the VO to a generation request, generates rows, and builds schema in the constructor per D-10/D-12.
     *
     * @param name   logical source name
     * @param source geo synthetic source configuration
     */
    public GeoSyntheticRowSource(String name, GeoSyntheticSourceVO source) {
        this.name = name;
        GeoSyntheticSourceOutputVO output = source.getOutput() == null ? new GeoSyntheticSourceOutputVO() : source.getOutput();
        GeoOutputFormatKind format = output.getFormat() == null ? GeoOutputFormatKind.columns : output.getFormat();
        GeoOutputColumnNames columnNames = output.getColumnNames() == null ? new GeoOutputColumnNames() : output.getColumnNames();
        GeoGenerationRequest request = GeoSyntheticRequestMapper.toRequest(name, source);
        try {
            List<Map<String, Object>> generated = GeoSyntheticGenerator.generateRows(request);
            this.schema = source.getSchema() != null
                    ? source.getSchema()
                    : GeoRowSchemaSupport.schemaForGeoRows(format, columnNames, generated);
            this.rows = new ArrayList<>(generated.size());
            for (Map<String, Object> values : generated) {
                this.rows.add(new Row(new LinkedHashMap<>(values)));
            }
        } catch (IOException e) {
            throw readFailure(name, request, e);
        } catch (IllegalArgumentException e) {
            // GeoResourceResolver reports missing classpath resources as IAE rather than IOException.
            if (isAlreadySourceScoped(e, name)) {
                throw e;
            }
            String path = ioPathForRequest(request);
            if (!path.isBlank()) {
                throw readFailure(name, request, e);
            }
            throw e;
        }
    }

    private static boolean isAlreadySourceScoped(IllegalArgumentException error, String name) {
        String message = error.getMessage();
        return message != null && message.contains("GEO synthetic source [" + name + "]");
    }

    private static IllegalArgumentException readFailure(String name, GeoGenerationRequest request, Exception cause) {
        return new IllegalArgumentException(
                "Failed to read GEO synthetic source [" + name + "] at [" + ioPathForRequest(request) + "]", cause);
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

    private static String ioPathForRequest(GeoGenerationRequest request) {
        GeoGenerationMode mode = request.getMode();
        if (mode == GeoGenerationMode.BOUNDARY_POINTS) {
            return request.getBoundaryPath();
        }
        if (mode == GeoGenerationMode.LINE_SAMPLE) {
            return request.getNetworkPath();
        }
        return "";
    }
}
