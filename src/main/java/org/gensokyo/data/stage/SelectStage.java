/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.stage.SelectStagePO;
import org.gensokyo.data.select.strategy.ValueSelectStrategyFactory;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * 数据选取阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public class SelectStage extends AbstractStage<SelectStagePO> {
    private ValueSelectStrategyFactory valueSelectStrategyFactory;

    @Autowired
    public void setSelectStrategyFactory(ValueSelectStrategyFactory valueSelectStrategyFactory) {
        this.valueSelectStrategyFactory = valueSelectStrategyFactory;
    }

    public SelectStage(StageContext<SelectStagePO> ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        if (Objects.isNull(input) || input.isNullOrEmpty()) {
            throw new DataGeneratorException(String.format("字段 %s 的输入值为 NULL 或者 空集合，无法执行选择元素阶段。",
                    ctx.field().getName()));
        }
        var spo = ctx.stage();
        try {
            var value = input;
            if (input instanceof ListValue lv) {
                var strategy = valueSelectStrategyFactory.newInstance(spo);
                value = lv.select(strategy, spo);
            }
            return value;
        } catch (Exception e) {
            throw new DataGeneratorException(String.format("字段 %s 的执行选择元素阶段失败，选择策略为：%s ，选择数量为：%s ，输入值为：%s。",
                    ctx.field().getName(), spo.getStrategyType(), spo.getSelectNum(), JsonKit.write(input.get())), e);
        }
    }
}
