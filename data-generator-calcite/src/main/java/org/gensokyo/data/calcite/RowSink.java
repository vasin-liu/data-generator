package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.List;

public interface RowSink {
    void write(RowSchema schema, List<Row> rows);
}
