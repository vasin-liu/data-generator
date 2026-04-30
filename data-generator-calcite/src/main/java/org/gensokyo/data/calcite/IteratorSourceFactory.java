package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.SourceVO;

public class IteratorSourceFactory implements V2SourceFactory {
    @Override
    public boolean supports(SourceVO source) {
        return source instanceof IteratorSourceVO;
    }

    @Override
    public RowSource create(String name, SourceVO source) {
        return new IteratorRowSource(name, (IteratorSourceVO) source);
    }
}
