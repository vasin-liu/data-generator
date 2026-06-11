package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

import lombok.Getter;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.List;

@Getter
public class TemplateV2RunResult {
    private final RowSchema schema;
    private final List<Row> rows;
    private final RunMetrics metrics;

    public TemplateV2RunResult(RowSchema schema, List<Row> rows) {
        this(schema, rows, null);
    }

    public TemplateV2RunResult(RowSchema schema, List<Row> rows, RunMetrics metrics) {
        this.schema = schema;
        this.rows = rows;
        this.metrics = metrics;
    }
}
