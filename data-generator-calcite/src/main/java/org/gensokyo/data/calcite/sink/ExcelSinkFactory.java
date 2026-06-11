package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.codec.*;

import org.gensokyo.data.constant.Const;
import org.gensokyo.data.model.vo.writer.WriterVO;

public class ExcelSinkFactory implements V2SinkFactory {
    @Override
    public boolean supports(WriterVO writer) {
        return writer != null && Const.WriterType.EXCEL.equalsIgnoreCase(writer.getType());
    }

    @Override
    public RowSink create(WriterVO writer) {
        return new ExcelRowSinkAdapter(writer);
    }
}
