/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.reader;

import org.gensokyo.data.generator.dataset.Dataset;
import org.gensokyo.data.generator.dataset.ReadableDataset;
import org.gensokyo.data.generator.domain.Context;
import org.gensokyo.data.generator.domain.ReaderPO;
import org.gensokyo.data.generator.factory.ScriptFactory;
import org.gensokyo.data.generator.util.DatasetKit;

import java.util.Objects;

/**
 * 常量值数据读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
public class ConstantReader extends AbstractReader {

    public ConstantReader(final ReaderPO rpo, final ScriptFactory scriptFactory) {
        super(Objects.requireNonNull(rpo), Objects.requireNonNull(scriptFactory));
    }

    @Override
    public Dataset read(final Context ctx) {
        var data = evalScript(ctx, DatasetKit.toList(rpo.getDataSet()));
        return ReadableDataset.of(data);
    }
}
