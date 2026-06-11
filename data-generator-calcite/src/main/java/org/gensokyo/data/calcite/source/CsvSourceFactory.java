package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.parser.*;

import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.SourceVO;

public class CsvSourceFactory implements V2SourceFactory {
    private final CsvParser csvParser;

    public CsvSourceFactory() {
        this(new DefaultCsvParser());
    }

    public CsvSourceFactory(CsvParser csvParser) {
        this.csvParser = csvParser;
    }

    @Override
    public boolean supports(SourceVO source) {
        return source instanceof CsvSourceVO;
    }

    @Override
    public RowSource create(String name, SourceVO source) {
        return new CsvRowSource(name, (CsvSourceVO) source, csvParser);
    }
}
