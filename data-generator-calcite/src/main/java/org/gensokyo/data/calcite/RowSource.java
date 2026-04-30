package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.List;

public interface RowSource {
    String name();

    RowSchema schema();

    List<Row> rows();
}
