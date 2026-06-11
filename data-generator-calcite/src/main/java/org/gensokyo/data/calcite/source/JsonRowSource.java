package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.parser.*;

import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.JsonSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonRowSource implements RowSource {
    private final String name;
    private final JsonSourceVO source;
    private final JsonParser jsonParser;

    public JsonRowSource(String name, JsonSourceVO source) {
        this(name, source, new DefaultJsonParser());
    }

    public JsonRowSource(String name, JsonSourceVO source, JsonParser jsonParser) {
        this.name = name;
        this.source = source;
        this.jsonParser = jsonParser;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public RowSchema schema() {
        if (source.getSchema() != null) {
            return source.getSchema();
        }
        RowSchema schema = new RowSchema();
        schema.setColumns(records().stream()
                .findFirst()
                .map(row -> row.keySet().stream()
                        .map(column -> new ColumnDef(column, "VARCHAR", true))
                        .toList())
                .orElseGet(List::of));
        return schema;
    }

    @Override
    public List<Row> rows() {
        long limit = source.getMaxRows() == null ? Long.MAX_VALUE : source.getMaxRows();
        return records().stream()
                .limit(limit)
                .map(values -> new Row(new LinkedHashMap<>(values)))
                .toList();
    }

    private List<Map<String, Object>> records() {
        return jsonParser.parse(source, readContent());
    }

    private String readContent() {
        if (source.getPath() == null || source.getPath().isBlank()) {
            throw new IllegalArgumentException("JSON source path must not be blank");
        }
        try {
            return Files.readString(Path.of(source.getPath()), Charset.forName(source.getCharset()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read JSON source: " + source.getPath(), e);
        }
    }
}
