/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.V2SourceFactory;
import org.gensokyo.data.model.v2.InlineRowsSourceVO;
import org.gensokyo.data.model.v2.SourceVO;

/**
 * Factory for template-embedded static row sources.
 *
 * @author Gensokyo
 * @since 2026-06-07
 */
public class InlineRowsSourceFactory implements V2SourceFactory {

    @Override
    public boolean supports(SourceVO source) {
        return source instanceof InlineRowsSourceVO;
    }

    @Override
    public RowSource create(String name, SourceVO source) {
        return new InlineRowsRowSource(name, (InlineRowsSourceVO) source);
    }
}
