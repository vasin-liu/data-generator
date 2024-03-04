/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * 数据读取器工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@RequiredArgsConstructor
public class StageFactory {

    private final AutowireCapableBeanFactory beanFactory;

    public @NonNull Stage newInstance(final StageContext ctx) {
        var stage = switch (ctx.stage().getType()) {
            case READ -> new ReadStage(ctx);
            case SELECT -> new SelectStage(ctx);
            case SCRIPT -> new ScriptStage(ctx);
//            case CONVERT -> new ConvertStage(ctx);
            case WRITE -> new WriteStage(ctx);
            default -> null;
        };
        Assert.notNull(stage, "未找到类型为 " + ctx.stage().getType() + " 的数据处理器类");
        beanFactory.autowireBean(stage);
        beanFactory.initializeBean(stage, stage.getClass().getSimpleName());
        return stage;
    }
}
