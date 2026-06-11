package org.gensokyo.data.calcite;

import org.gensokyo.data.model.vo.writer.WriterVO;

public interface V2SinkFactory {
    boolean supports(WriterVO writer);

    RowSink create(WriterVO writer);
}
