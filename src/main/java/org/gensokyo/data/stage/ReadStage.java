/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.ReadStagePO;
import org.gensokyo.data.read.ReaderFactory;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.Value;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 数据读取阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public class ReadStage extends AbstractStage {
    private ReaderFactory readerFactory;

    @Autowired
    public void setReaderFactory(ReaderFactory readerFactory) {
        this.readerFactory = readerFactory;
    }

    public ReadStage(StageContext ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        if (ctx.stage() instanceof ReadStagePO rpo) {
            var result = new MapValue();
            for (ReadStagePO.ReaderPO po : rpo.getReaders()) {
                var ds = readerFactory.newInstance(po).read(input);
                result.put(po.getDataSetId(), ds);
            }
            //只有一个数据源的情况，返回数据集
            if (result.size() == 1) {
                return result.values().stream().findFirst().orElse(Value.EMPTY);
            }
            return result;
        }

        throw new DataGeneratorException(String.format("当前阶段要求的配置值类型为：[%s] ，实际的配置值类型为：[%s]",
                ReadStagePO.class.getName(), ctx.stage().getClass().getName()));
    }
}
