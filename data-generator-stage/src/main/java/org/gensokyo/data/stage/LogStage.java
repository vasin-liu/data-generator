/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.json.JsonKit;

/**
 * 日志打印阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
public class LogStage extends AbstractStage<LogStageVO> {

    public LogStage(StageContext<LogStageVO> ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        if (log.isInfoEnabled()) {
            log.info("输入值为：{}，上下文信息为：{}", input, JsonKit.write(ctx));
        }
        return input;
    }
}
