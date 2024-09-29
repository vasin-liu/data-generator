/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader.strategy;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.selector.reader.ReaderSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.util.TypeKit;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * 读取器选择策略工厂
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
@RequiredArgsConstructor
public class ReaderSelectStrategyFactory {

    private final ApplicationContext ctx;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public @NonNull <S extends ReadStageVO, T extends ReaderVO, R extends ReaderSelectStrategyVO> ReaderSelectStrategy<S, T, R> newInstance(final S rsvo) {
        ReaderSelectStrategy<S, T, R> strategy = null;
        Map<String, ReaderSelectStrategy> services = ctx.getBeansOfType(ReaderSelectStrategy.class);

        for (ReaderSelectStrategy<?, ?, ?> service : services.values()) {
            if (TypeKit.isMatchingType(ReaderSelectStrategy.class, service,
                    rsvo.getClass(), ReaderVO.class, rsvo.getStrategy().getClass())) {
                strategy = (ReaderSelectStrategy<S, T, R>) service;
            }
        }

        Assert.notNull(strategy, "未找到类型为 " + rsvo.getStrategy().getType() + " 的数据选择策略类");
        var beanFactory = this.ctx.getAutowireCapableBeanFactory();
        beanFactory.autowireBean(strategy);
        beanFactory.initializeBean(strategy, strategy.getClass().getSimpleName());
        return strategy;
    }
}
