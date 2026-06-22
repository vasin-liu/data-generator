/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.transform;

import org.gensokyo.data.calcite.V2TransformFactory;
import org.gensokyo.data.calcite.sql.CalciteExecutionContext;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.LookupTransformVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.TransformVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Row-local transform that enriches input rows by joining, on a key, against another source already declared
 * in the same template (D-04). The lookup source is materialized once into a single-pass {@code HashMap}
 * index keyed by {@code rightKey}; each input row is then enriched with the projected lookup columns.
 *
 * <p>Failures are fail-fast (D-10): a missing lookup source, a duplicate {@code rightKey} value, and a lookup
 * miss each throw an exception naming the source and key so the run report can locate the problem (D-08).</p>
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
public class LookupTransformFactory implements V2TransformFactory {

    private static final String INPUT_TABLE = "input";

    /**
     * Returns whether this factory handles {@link LookupTransformVO}.
     *
     * @param transform transform configuration
     * @return {@code true} for lookup transforms
     */
    @Override
    public boolean supports(TransformVO transform) {
        return transform instanceof LookupTransformVO;
    }

    /**
     * Joins table {@code input} against the named lookup source and projects the configured columns.
     *
     * @param transform lookup transform definition
     * @param context   execution context containing table {@code input} and the named lookup source
     * @return schema with input columns plus projected lookup columns, and the enriched rows
     * @throws IllegalArgumentException if a required table/field is missing
     * @throws IllegalStateException    on a duplicate lookup key or a lookup miss (fail-fast, D-10)
     */
    @Override
    public CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context) {
        LookupTransformVO lookup = (LookupTransformVO) transform;
        RowSchema inputSchema = context.getSchemas().get(INPUT_TABLE);
        List<Row> inputRows = context.getData().get(INPUT_TABLE);
        if (inputSchema == null || inputRows == null) {
            throw new IllegalArgumentException("Lookup transform requires table '" + INPUT_TABLE + "' in execution context");
        }

        String source = lookup.getSource();
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Lookup transform requires a source");
        }
        if (lookup.getLeftKey() == null || lookup.getLeftKey().isBlank()) {
            throw new IllegalArgumentException("Lookup transform requires a leftKey");
        }
        if (lookup.getRightKey() == null || lookup.getRightKey().isBlank()) {
            throw new IllegalArgumentException("Lookup transform requires a rightKey");
        }
        List<Row> lookupRows = context.getData().get(source);
        if (lookupRows == null) {
            throw new IllegalArgumentException("Lookup source not found in template: " + source);
        }

        String leftKey = lookup.getLeftKey().toLowerCase(Locale.ROOT);
        String rightKey = lookup.getRightKey().toLowerCase(Locale.ROOT);
        List<String> projected = lookup.getColumns();

        // Build the key -> row index once; a duplicate rightKey is a fail-fast configuration error.
        Map<Object, Row> index = new HashMap<>();
        for (Row lookupRow : lookupRows) {
            Object key = lookupRow.values().get(rightKey);
            if (index.put(key, lookupRow) != null) {
                throw new IllegalStateException("Duplicate lookup key in source '" + source + "': " + key);
            }
        }

        RowSchema outputSchema = mergeSchema(inputSchema, projected);
        List<Row> outputRows = new ArrayList<>(inputRows.size());
        for (Row inputRow : inputRows) {
            Object key = inputRow.values().get(leftKey);
            Row match = index.get(key);
            if (match == null) {
                throw new IllegalStateException("Lookup miss for key '" + key + "' in source '" + source + "'");
            }
            Map<String, Object> values = new LinkedHashMap<>(inputRow.values());
            for (String column : projected) {
                String columnKey = column.toLowerCase(Locale.ROOT);
                values.put(columnKey, match.values().get(columnKey));
            }
            outputRows.add(new Row(values));
        }
        return new CalciteRowTransformer.TransformResult(outputSchema, outputRows);
    }

    private static RowSchema mergeSchema(RowSchema inputSchema, List<String> projected) {
        Map<String, ColumnDef> columns = new LinkedHashMap<>();
        if (inputSchema.getColumns() != null) {
            for (ColumnDef column : inputSchema.getColumns()) {
                columns.put(column.getName().toLowerCase(Locale.ROOT), column);
            }
        }
        for (String column : projected) {
            columns.put(column.toLowerCase(Locale.ROOT), new ColumnDef(column, "ANY", true));
        }
        RowSchema schema = new RowSchema();
        schema.setColumns(List.copyOf(columns.values()));
        return schema;
    }
}
