/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.SelectStageVO;
import org.gensokyo.data.selector.strategy.ValueSelectStrategyFactory;
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
@Slf4j
public class SelectStage extends AbstractStage<SelectStageVO> {
    @Setter(onMethod_ = @Autowired)
    private ValueSelectStrategyFactory valueSelectStrategyFactory;

    public SelectStage(StageContext<SelectStageVO> ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        if (Objects.isNull(input) || input.isNullOrEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("当前输入值为 NULL 或者 空集合，忽略执行选择元素阶段，上下文信息为：{}", JsonKit.write(ctx));
            }
            return input;
        }
        var vpo = ctx.stage().getStrategy();
        try {
            var value = input;
            if (input instanceof ListValue lv) {
                var strategy = valueSelectStrategyFactory.newInstance(vpo);
                var sctx = new StageContext<>(ctx.template(), ctx.field(), ctx.stage());
                value = lv.select(strategy, sctx, vpo);
            }
            return value;
        } catch (Exception e) {
            var msg = String.format("执行选择元素阶段失败，输入值为：%s，上下文信息为：%s", input, JsonKit.write(ctx));
            throw new DataGeneratorException(msg, e);
        }
    }
}
