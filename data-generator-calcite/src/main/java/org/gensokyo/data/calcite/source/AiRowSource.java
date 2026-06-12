package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.AiRuntimeBridge;
import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.runtime.AiGenerateResult;
import org.gensokyo.data.calcite.runtime.AiRunMetricsScope;

import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.AiSourceVO;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AiRowSource implements RowSource {
    private static final RowSchema DEFAULT_SCHEMA = defaultSchema();

    private final String name;
    private final RowSchema schema;
    private final List<Row> rows;
    private final AiRuntimeBridge aiRuntimeBridge;

    public AiRowSource(String name, AiSourceVO source) {
        this(name, source, null);
    }

    public AiRowSource(String name, AiSourceVO source, AiRuntimeBridge aiRuntimeBridge) {
        this.name = name;
        this.aiRuntimeBridge = aiRuntimeBridge;
        this.rows = materialize(source);
        this.schema = resolveSchema(source.getSchema(), rows);
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

    private List<Row> materialize(AiSourceVO source) {
        AiProviderVO provider = source.getProvider();
        if (provider == null || provider.getType() == null || provider.getType().isBlank()) {
            throw new IllegalArgumentException("AI source provider type must not be blank");
        }
        Map<String, Object> options = provider.getOptions() == null ? Map.of() : provider.getOptions();
        return switch (provider.getType().trim().toUpperCase(Locale.ROOT)) {
            case "INLINE", "STATIC" -> inlineRows(options);
            case "ECHO" -> List.of(new Row(Map.of("content", source.getPrompt() == null ? "" : source.getPrompt())));
            default -> bridgeRows(source, provider);
        };
    }

    private List<Row> bridgeRows(AiSourceVO source, AiProviderVO provider) {
        if (aiRuntimeBridge == null || !aiRuntimeBridge.supports(provider)) {
            throw new UnsupportedOperationException("AI provider type [" + provider.getType()
                    + "] requires an external AI runtime bridge");
        }
        AiGenerateResult result = aiRuntimeBridge.generateTraced(source);
        var metrics = AiRunMetricsScope.current();
        if (metrics != null && result.metric() != null) {
            metrics.recordAiCall(name, result.metric());
        }
        return toRows(result.payload());
    }

    private List<Row> inlineRows(Map<String, Object> options) {
        Object rowsOption = options.get("rows");
        if (rowsOption instanceof List<?> rowItems) {
            List<Row> materialized = new ArrayList<>(rowItems.size());
            for (Object item : rowItems) {
                materialized.add(new Row(toRowMap(item)));
            }
            return materialized;
        }
        Object rowOption = options.get("row");
        if (rowOption != null) {
            return List.of(new Row(toRowMap(rowOption)));
        }
        Object content = options.get("content");
        return List.of(new Row(Map.of("content", content == null ? "" : content)));
    }

    private List<Row> toRows(Object output) {
        if (output instanceof List<?> items) {
            List<Row> materialized = new ArrayList<>(items.size());
            for (Object item : items) {
                materialized.add(new Row(toRowMap(item)));
            }
            return materialized;
        }
        return List.of(new Row(toRowMap(output)));
    }

    private Map<String, Object> toRowMap(Object item) {
        if (item instanceof Map<?, ?> map) {
            Map<String, Object> row = new LinkedHashMap<>();
            map.forEach((key, value) -> row.put(String.valueOf(key), value));
            return row;
        }
        if (item instanceof Row row) {
            return row.values();
        }
        return Map.of("content", item == null ? "" : item);
    }

    private static RowSchema resolveSchema(RowSchema declared, List<Row> materializedRows) {
        if (declared != null && declared.getColumns() != null && !declared.getColumns().isEmpty()) {
            return declared;
        }
        if (!materializedRows.isEmpty()) {
            Map<String, Object> first = materializedRows.getFirst().values();
            if (!first.isEmpty()) {
                RowSchema inferred = new RowSchema();
                List<ColumnDef> columns = new ArrayList<>();
                for (String key : first.keySet()) {
                    columns.add(new ColumnDef(key, "VARCHAR", true));
                }
                inferred.setColumns(columns);
                return inferred;
            }
        }
        return defaultSchema();
    }

    private static RowSchema defaultSchema() {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(new ColumnDef("content", "VARCHAR", true)));
        return schema;
    }
}
