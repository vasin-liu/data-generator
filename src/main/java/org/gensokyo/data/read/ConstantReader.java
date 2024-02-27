/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.read;

import org.gensokyo.data.Context;
import org.gensokyo.data.po.ReaderPO;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;

import java.util.Objects;

/**
 * 常量值数据读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/26 , Version 1.0.0
 */
public class ConstantReader extends AbstractReader {

    public ConstantReader(final ReaderPO rpo) {
        super(Objects.requireNonNull(rpo));
    }

    @Override
    public Value read(final Context ctx) {
        return ListValue.fromObjectList(DatasetKit.toList(rpo.getDataSet()));
    }
}

