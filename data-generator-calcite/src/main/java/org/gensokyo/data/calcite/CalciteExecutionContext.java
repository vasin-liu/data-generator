package org.gensokyo.data.calcite;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class CalciteExecutionContext {
    private final Map<String, RowSchema> schemas = new LinkedHashMap<>();
    private final Map<String, List<Row>> data = new LinkedHashMap<>();

    public CalciteExecutionContext addSchema(String tableName, RowSchema schema) {
        schemas.put(tableName, schema);
        data.putIfAbsent(tableName, new ArrayList<>());
        return this;
    }

    public CalciteExecutionContext addTable(String tableName, RowSchema schema, List<Row> rows) {
        schemas.put(tableName, schema);
        data.put(tableName, new ArrayList<>(rows));
        return this;
    }

    public CalciteExecutionContext addSource(RowSource source) {
        schemas.put(source.name(), source.schema());
        data.put(source.name(), new ArrayList<>(source.rows()));
        return this;
    }
}
