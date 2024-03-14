/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.read;

import org.gensokyo.data.context.ReaderContext;
import org.gensokyo.data.po.reader.ConstantReaderPO;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.value.Value;

/**
 * 常量值数据读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/26 , Version 1.0.0
 */
public class ConstantReader<T extends ConstantReaderPO> implements Reader<T> {

    @Override
    public Value read(final ReaderContext<T> ctx, final Value input) {
        var rpo = ctx.reader();
        return DatasetKit.toValue(rpo.getData());
    }
}

