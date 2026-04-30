package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.List;

public class ConsoleRowSinkAdapter implements RowSink {
    @Override
    public void write(RowSchema schema, List<Row> rows) {
        rows.forEach(row -> System.out.println(row.values()));
    }
}
