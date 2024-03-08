/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.SelectStagePO;
import org.gensokyo.data.select.strategy.SelectStrategyFactory;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * 数据选取阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public class SelectStage extends AbstractStage {
    private SelectStrategyFactory selectStrategyFactory;

    @Autowired
    public void setSelectStrategyFactory(SelectStrategyFactory selectStrategyFactory) {
        this.selectStrategyFactory = selectStrategyFactory;
    }

    public SelectStage(StageContext ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        if (Objects.isNull(input) || input.isNullOrEmpty()) {
            return Value.EMPTY;
        }
        if (ctx.stage() instanceof SelectStagePO spo) {
            var value = input;
            if (input instanceof ListValue lv) {
                var strategy = selectStrategyFactory.newInstance(spo);
                value = lv.select(strategy, spo.getNum());
            }
            return value;
        }
        throw new DataGeneratorException(String.format("当前阶段要求的配置值类型为：[%s] ，实际的配置值类型为：[%s]",
                SelectStagePO.class.getName(), ctx.stage().getClass().getName()));
    }
}
