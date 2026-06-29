package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.List;

public interface RowSink {
    void write(RowSchema schema, List<Row> rows);

    /**
     * Writes rows in slices of at most {@code batchSize} rows per {@link #write} call.
     *
     * @param schema    output schema
     * @param rows      all rows to write
     * @param batchSize maximum rows per batch (must be positive when rows are non-empty)
     */
    default void writeBatch(RowSchema schema, List<Row> rows, int batchSize) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        for (int i = 0; i < rows.size(); i += batchSize) {
            write(schema, rows.subList(i, Math.min(i + batchSize, rows.size())));
        }
    }

    /**
     * Finalizes a streaming file sink after the last chunk (e.g. closes a JSON array bracket).
     * Default implementation is a no-op for one-shot and JDBC sinks.
     */
    default void finish() {
        // Non-streaming sinks require no finalize step.
    }
}
