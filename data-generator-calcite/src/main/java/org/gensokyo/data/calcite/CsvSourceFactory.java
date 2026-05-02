package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.SourceVO;

public class CsvSourceFactory implements V2SourceFactory {
    @Override
    public boolean supports(SourceVO source) {
        return source instanceof CsvSourceVO;
    }

    @Override
    public RowSource create(String name, SourceVO source) {
        return new CsvRowSource(name, (CsvSourceVO) source);
    }
}
