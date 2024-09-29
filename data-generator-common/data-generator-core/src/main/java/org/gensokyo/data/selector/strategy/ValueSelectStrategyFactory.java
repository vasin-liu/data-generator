/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.selector.strategy;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.vo.selector.value.ValueSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.SelectStageVO;
import org.gensokyo.data.util.TypeKit;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * 选择策略工厂
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
@RequiredArgsConstructor
public class ValueSelectStrategyFactory {

    private final ApplicationContext ctx;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public @NonNull <S extends SelectStageVO, T extends ValueSelectStrategyVO> ValueSelectStrategy<S, T> newInstance(final T vssvo) {
        ValueSelectStrategy<S, T> selectStrategy = null;
        Map<String, ValueSelectStrategy> services = ctx.getBeansOfType(ValueSelectStrategy.class);

        for (ValueSelectStrategy<?, ?> service : services.values()) {
            if (TypeKit.isMatchingType(ValueSelectStrategy.class, service, SelectStageVO.class, vssvo.getClass())) {
                selectStrategy = (ValueSelectStrategy<S, T>) service;
            }
        }

        Assert.notNull(selectStrategy, "未找到类型为 " + vssvo.getType() + " 的数据选择策略类");
        var beanFactory = this.ctx.getAutowireCapableBeanFactory();
        beanFactory.autowireBean(selectStrategy);
        beanFactory.initializeBean(selectStrategy, selectStrategy.getClass().getSimpleName());
        return selectStrategy;
    }
}
