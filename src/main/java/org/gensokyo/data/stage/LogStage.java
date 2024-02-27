/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.stage.LogStagePO;
import org.gensokyo.data.po.stage.SelectStagePO;
import org.gensokyo.data.select.strategy.ValueSelectStrategyFactory;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * 日志打印阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
public class LogStage extends AbstractStage<LogStagePO> {

    public LogStage(StageContext<LogStagePO> ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        log.info("当前字段 {} 的配置信息为：{} ，上一阶段输入值为： {}",
                ctx.field().getName(), JsonKit.write(ctx.field()), JsonKit.write(input));
        return input;
    }
}
