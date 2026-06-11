package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.SourceVO;

public interface V2SourceFactory {
    boolean supports(SourceVO source);

    RowSource create(String name, SourceVO source);
}
