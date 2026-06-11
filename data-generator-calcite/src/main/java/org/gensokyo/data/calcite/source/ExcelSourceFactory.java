package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.parser.*;

import org.gensokyo.data.model.v2.ExcelSourceVO;
import org.gensokyo.data.model.v2.SourceVO;

public class ExcelSourceFactory implements V2SourceFactory {
    @Override
    public boolean supports(SourceVO source) {
        return source instanceof ExcelSourceVO;
    }

    @Override
    public RowSource create(String name, SourceVO source) {
        return new ExcelRowSource(name, (ExcelSourceVO) source);
    }
}
