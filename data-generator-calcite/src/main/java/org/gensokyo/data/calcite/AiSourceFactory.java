package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.AiSourceVO;
import org.gensokyo.data.model.v2.SourceVO;

public class AiSourceFactory implements V2SourceFactory {
    @Override
    public boolean supports(SourceVO source) {
        return source instanceof AiSourceVO;
    }

    @Override
    public RowSource create(String name, SourceVO source) {
        return new AiRowSource(name, (AiSourceVO) source);
    }
}
