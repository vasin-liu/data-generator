package org.example.datagenerator.plugin.sample;

import org.gensokyo.data.calcite.RowSink;
import org.gensokyo.data.calcite.V2SinkFactory;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.WriterVO;

import java.util.List;

public final class SampleLoggingSinkFactory implements V2SinkFactory {
    private static final String TYPE = "sample_logging";

    @Override
    public boolean supports(WriterVO writer) {
        return writer != null && TYPE.equalsIgnoreCase(writer.getType());
    }

    @Override
    public RowSink create(WriterVO writer) {
        return new SampleLoggingRowSink();
    }

    private static final class SampleLoggingRowSink implements RowSink {
        @Override
        public void write(RowSchema schema, List<Row> rows) {
            System.out.println("[sample-logging-sink] schema=" + schema.getColumns());
            for (Row row : rows) {
                System.out.println("[sample-logging-sink] row=" + row.values());
            }
        }
    }
}
