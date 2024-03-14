/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.read.strategy;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.po.stage.ReadStagePO;
import org.gensokyo.data.po.reader.ReaderPO;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * 读取器选择策略工厂
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
@RequiredArgsConstructor
public class ReaderSelectStrategyFactory {

    private final AutowireCapableBeanFactory beanFactory;

    public @NonNull ReaderSelectStrategy<? extends ReaderPO> newInstance(final ReadStagePO rpo) {
        ReaderSelectStrategy<? extends ReaderPO> strategy = switch (rpo.getStrategyType()) {
            case EQUAL -> beanFactory.getBean(EqualReaderSelectStrategy.class);
            case WEIGHT -> beanFactory.getBean(WeightReaderSelectStrategy.class);
        };
        Assert.notNull(strategy, "未找到类型为 " + rpo.getStrategyType() + " 的数据选择策略类");
        beanFactory.autowireBean(strategy);
        beanFactory.initializeBean(strategy, strategy.getClass().getSimpleName());
        return strategy;
    }
}
