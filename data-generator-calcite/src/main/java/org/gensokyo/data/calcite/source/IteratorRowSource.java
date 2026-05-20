/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.parser.*;

import org.gensokyo.data.geo.GeoGenerationRequest;
import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.geo.format.GeoValueFormatter;
import org.gensokyo.data.geo.GeoSyntheticGenerator;
import org.gensokyo.data.iterator.ConstantIteratorVO;
import org.gensokyo.data.iterator.DateTimeIteratorVO;
import org.gensokyo.data.iterator.GeoIteratorRequestMapper;
import org.gensokyo.data.iterator.GeoIteratorVO;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class IteratorRowSource implements RowSource {
    private static final RowSchema NUMBER_SCHEMA = numberSchema();
    private static final RowSchema VALUE_SCHEMA = valueSchema("VARCHAR");
    private static final RowSchema DATETIME_SCHEMA = valueSchema("TIMESTAMP");

    private final String name;
    private final RowSchema schema;
    private final List<Row> rows;

    public IteratorRowSource(String name, IteratorSourceVO source) {
        this.name = name;
        switch (source.getIterator()) {
            case NumberIteratorVO iterator -> {
                this.schema = NUMBER_SCHEMA;
                this.rows = materialize(iterator);
            }
            case ConstantIteratorVO iterator -> {
                this.schema = VALUE_SCHEMA;
                this.rows = materialize(iterator);
            }
            case DateTimeIteratorVO iterator -> {
                this.schema = DATETIME_SCHEMA;
                this.rows = materialize(iterator);
            }
            case GeoIteratorVO iterator -> {
                GeoGenerationRequest request = GeoIteratorRequestMapper.toRequest(iterator);
                try {
                    List<Map<String, Object>> generated = GeoSyntheticGenerator.generateRows(request);
                    this.schema = geoSchema(request, generated);
                    this.rows = materializeGeo(generated);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Failed to materialize GEO iterator for source [" + name + "]", e);
                }
            }
            case null -> throw new IllegalArgumentException("Iterator source [" + name + "] iterator must not be null");
            default -> throw new IllegalArgumentException("Unsupported Template V2 iterator type ["
                    + source.getIterator().getType() + "] for source [" + name + "]");
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

    private static List<Row> materialize(NumberIteratorVO iterator) {
        List<Row> rows = new ArrayList<>();
        long current = iterator.getFrom();
        long to = iterator.getTo();
        int step = iterator.getStep();
        if (step <= 0) {
            throw new IllegalArgumentException("Iterator step must be positive");
        }
        while (current <= to) {
            rows.add(new Row(new LinkedHashMap<>(java.util.Map.of("value", current))));
            current += step;
        }
        return rows;
    }

    private static List<Row> materialize(ConstantIteratorVO iterator) {
        if (iterator.getDataset() == null || iterator.getDataset().isEmpty()) {
            throw new IllegalArgumentException("CONSTANT iterator dataset must not be empty");
        }
        int repeat = iterator.getRepeat();
        if (repeat == -1) {
            throw new IllegalArgumentException("CONSTANT iterator repeat [-1] is not supported by finite Template V2 materialization");
        }
        if (repeat <= 0) {
            throw new IllegalArgumentException("CONSTANT iterator repeat must be positive");
        }
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < repeat; i++) {
            for (Object value : iterator.getDataset()) {
                rows.add(valueRow(value));
            }
        }
        return rows;
    }

    private static List<Row> materialize(DateTimeIteratorVO iterator) {
        LocalDateTime current = Objects.requireNonNull(iterator.getFrom(), "DATETIME iterator from must not be null");
        LocalDateTime to = iterator.getTo() == null ? LocalDateTime.now() : iterator.getTo();
        int step = iterator.getStep();
        if (step <= 0) {
            throw new IllegalArgumentException("DATETIME iterator step must be positive");
        }
        ChronoUnit unit = iterator.getUnit() == null ? ChronoUnit.DAYS : iterator.getUnit();
        if (current.isAfter(to)) {
            throw new IllegalArgumentException("DATETIME iterator from must not be after to");
        }
        List<Row> rows = new ArrayList<>();
        while (!current.isAfter(to)) {
            rows.add(valueRow(current));
            current = current.plus(step, unit);
        }
        return rows;
    }

    private static Row valueRow(Object value) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("value", value);
        return new Row(values);
    }

    private static RowSchema numberSchema() {
        return valueSchema("BIGINT");
    }

    private static RowSchema valueSchema(String type) {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(new ColumnDef("value", type, false)));
        return schema;
    }

    private static List<Row> materializeGeo(List<Map<String, Object>> generated) {
        List<Row> rows = new ArrayList<>(generated.size());
        for (Map<String, Object> values : generated) {
            rows.add(new Row(new LinkedHashMap<>(values)));
        }
        return rows;
    }

    private static RowSchema geoSchema(GeoGenerationRequest request, List<Map<String, Object>> generated) {
        List<ColumnDef> columns = new ArrayList<>();
        if (generated.isEmpty()) {
            for (String column : GeoValueFormatter.columnNames(request.getOutputFormat(), request.getColumnNames())) {
                String sqlType = request.getOutputFormat() == GeoOutputFormatKind.columns ? "DOUBLE" : "VARCHAR";
                columns.add(new ColumnDef(column, sqlType, false));
            }
        } else {
            for (String key : generated.get(0).keySet()) {
                Object value = generated.get(0).get(key);
                boolean nullable = key.startsWith("prop.");
                String sqlType;
                if (value instanceof Number) {
                    sqlType = "DOUBLE";
                } else {
                    sqlType = "VARCHAR";
                }
                columns.add(new ColumnDef(key, sqlType, nullable));
            }
        }
        RowSchema schema = new RowSchema();
        schema.setColumns(columns);
        return schema;
    }
}
