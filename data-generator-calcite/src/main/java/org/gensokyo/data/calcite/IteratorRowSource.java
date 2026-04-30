package org.gensokyo.data.calcite;

import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class IteratorRowSource implements RowSource {
    private static final RowSchema NUMBER_SCHEMA = numberSchema();

    private final String name;
    private final RowSchema schema;
    private final List<Row> rows;

    public IteratorRowSource(String name, IteratorSourceVO source) {
        this.name = name;
        if (!(source.getIterator() instanceof NumberIteratorVO iterator)) {
            throw new IllegalArgumentException("Only NUMBER iterator is supported in the current V2 skeleton");
        }
        this.schema = NUMBER_SCHEMA;
        this.rows = materialize(iterator);
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

    private static RowSchema numberSchema() {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(new ColumnDef("value", "BIGINT", false)));
        return schema;
    }
}
