/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.select.strategy;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.po.SelectStagePO;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * 选择策略工厂
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
@RequiredArgsConstructor
public class ValueSelectStrategyFactory {

    private final AutowireCapableBeanFactory beanFactory;

    public @NonNull ValueSelectStrategy newInstance(final SelectStagePO spo) {
        var strategy = switch (spo.getStrategyType()) {
            case REPEAT_RANDOM -> beanFactory.getBean(RepeatRandomValueSelectStrategy.class);
            case ONCE_RANDOM -> beanFactory.getBean(OnceRandomValueSelectStrategy.class);
            case ONCE_ORDER -> beanFactory.getBean(OnceOrderValueSelectStrategy.class);
            case REPEAT_ORDER -> beanFactory.getBean(RepeatOrderValueSelectStrategy.class);
            case MULTIPLE_ORDER -> beanFactory.getBean(MultipleOrderValueSelectStrategy.class);
        };
        Assert.notNull(strategy, "未找到类型为 " + spo.getStrategyType() + " 的数据选择策略类");
        beanFactory.autowireBean(strategy);
        beanFactory.initializeBean(strategy, strategy.getClass().getSimpleName());
        return strategy;
    }
}
