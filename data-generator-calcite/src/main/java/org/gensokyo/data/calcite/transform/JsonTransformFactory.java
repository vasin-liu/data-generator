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
import org.gensokyo.data.model.v2.JsonTransformVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.TransformVO;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Row-local transform that parses a JSON string column into an object and, when {@code flatten} is enabled,
 * expands nested fields into separate columns named with a separator convention (e.g. {@code addr.city}).
 *
 * <p>Parsing is pure Java (Jackson 3) inside the factory. On a parse failure the thrown
 * {@link IllegalArgumentException} names the offending column and a bounded value snippet so the run report
 * can build a row/field locator (D-08) without echoing the full payload.</p>
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
public class JsonTransformFactory implements V2TransformFactory {

    private static final String INPUT_TABLE = "input";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int SNIPPET_MAX = 50;

    /**
     * Returns whether this factory handles {@link JsonTransformVO}.
     *
     * @param transform transform configuration
     * @return {@code true} for JSON transforms
     */
    @Override
    public boolean supports(TransformVO transform) {
        return transform instanceof JsonTransformVO;
    }

    /**
     * Parses the configured source column for every row in table {@code input}, optionally flattening.
     *
     * @param transform JSON transform definition
     * @param context   execution context containing table {@code input}
     * @return merged schema and rows carrying the parsed/flattened columns
     * @throws IllegalArgumentException if table {@code input} is missing, {@code sourceColumn} is blank,
     *                                  or a row value cannot be parsed as JSON
     */
    @Override
    public CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context) {
        JsonTransformVO jsonTransform = (JsonTransformVO) transform;
        RowSchema inputSchema = context.getSchemas().get(INPUT_TABLE);
        List<Row> inputRows = context.getData().get(INPUT_TABLE);
        if (inputSchema == null || inputRows == null) {
            throw new IllegalArgumentException("JSON transform requires table '" + INPUT_TABLE + "' in execution context");
        }

        String sourceColumn = jsonTransform.getSourceColumn();
        if (sourceColumn == null || sourceColumn.isBlank()) {
            throw new IllegalArgumentException("JSON transform requires a sourceColumn");
        }
        boolean flatten = jsonTransform.isFlatten();
        String separator = (jsonTransform.getSeparator() == null || jsonTransform.getSeparator().isEmpty())
                ? "." : jsonTransform.getSeparator();
        String targetColumn = (jsonTransform.getTargetColumn() == null || jsonTransform.getTargetColumn().isBlank())
                ? sourceColumn : jsonTransform.getTargetColumn();
        String sourceKey = sourceColumn.toLowerCase(Locale.ROOT);

        // Parse every row once, collecting the union of flattened column names so the output schema is stable.
        List<Map<String, Object>> perRowExtra = new ArrayList<>(inputRows.size());
        Set<String> flattenColumns = new LinkedHashSet<>();
        for (Row inputRow : inputRows) {
            Map<String, Object> extra = new LinkedHashMap<>();
            Object raw = inputRow.values().get(sourceKey);
            if (raw != null && !raw.toString().isBlank()) {
                Object parsed = parse(sourceColumn, raw.toString());
                if (flatten && parsed instanceof Map<?, ?> parsedMap) {
                    flattenValue("", parsedMap, separator, extra);
                    flattenColumns.addAll(extra.keySet());
                } else {
                    extra.put(targetColumn, parsed);
                }
            }
            perRowExtra.add(extra);
        }

        RowSchema outputSchema = mergeSchema(inputSchema, flatten, flattenColumns, targetColumn);
        List<Row> outputRows = new ArrayList<>(inputRows.size());
        for (int i = 0; i < inputRows.size(); i++) {
            Map<String, Object> values = new LinkedHashMap<>(inputRows.get(i).values());
            if (flatten) {
                // Ensure every union column is present (null when this row lacked the nested key).
                for (String column : flattenColumns) {
                    values.put(column.toLowerCase(Locale.ROOT), null);
                }
            }
            for (Map.Entry<String, Object> entry : perRowExtra.get(i).entrySet()) {
                values.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
            }
            outputRows.add(new Row(values));
        }
        return new CalciteRowTransformer.TransformResult(outputSchema, outputRows);
    }

    private static Object parse(String column, String text) {
        try {
            return MAPPER.readValue(text, Object.class);
        } catch (JacksonException e) {
            // Located, bounded message lets 04-04 build a row/field locator without echoing the full payload.
            throw new IllegalArgumentException(
                    "JSON transform failed to parse column '" + column + "': " + snippet(text), e);
        }
    }

    private static void flattenValue(String prefix, Object value, String separator, Map<String, Object> out) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String name = prefix.isEmpty() ? key : prefix + separator + key;
                flattenValue(name, entry.getValue(), separator, out);
            }
        } else {
            out.put(prefix, value);
        }
    }

    private static RowSchema mergeSchema(RowSchema inputSchema, boolean flatten, Set<String> flattenColumns,
                                         String targetColumn) {
        Map<String, ColumnDef> columns = new LinkedHashMap<>();
        if (inputSchema.getColumns() != null) {
            for (ColumnDef column : inputSchema.getColumns()) {
                columns.put(column.getName().toLowerCase(Locale.ROOT), column);
            }
        }
        if (flatten) {
            for (String name : flattenColumns) {
                columns.put(name.toLowerCase(Locale.ROOT), new ColumnDef(name, "ANY", true));
            }
        } else {
            columns.put(targetColumn.toLowerCase(Locale.ROOT), new ColumnDef(targetColumn, "ANY", true));
        }
        RowSchema schema = new RowSchema();
        schema.setColumns(List.copyOf(columns.values()));
        return schema;
    }

    private static String snippet(String text) {
        // Bound the echoed payload so a hostile/large value cannot bloat the error/report.
        String trimmed = text.strip();
        return trimmed.length() <= SNIPPET_MAX ? trimmed : trimmed.substring(0, SNIPPET_MAX) + "…";
    }
}
