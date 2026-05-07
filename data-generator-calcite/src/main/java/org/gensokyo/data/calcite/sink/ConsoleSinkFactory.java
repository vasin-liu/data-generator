package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.codec.*;

import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;

public class ConsoleSinkFactory implements V2SinkFactory {
    @Override
    public boolean supports(WriterVO writer) {
        return writer instanceof ConsoleWriterVO;
    }

    @Override
    public RowSink create(WriterVO writer) {
        return new ConsoleRowSinkAdapter();
    }
}
