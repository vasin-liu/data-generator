/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.selector.reader.EqualReaderSelectStrategyVO;
import org.gensokyo.data.model.vo.selector.reader.WeightReaderSelectStrategyVO;
import org.gensokyo.data.model.vo.selector.value.*;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.model.vo.stage.SelectStageVO;
import org.gensokyo.data.reader.strategy.EqualReaderSelectStrategy;
import org.gensokyo.data.reader.strategy.WeightReaderSelectStrategy;
import org.gensokyo.data.selector.strategy.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Configuration
public class SelectStrategyConfig {

    @Bean
    @ConditionalOnMissingBean(RepeatRandomValueSelectStrategy.class)
    public <S extends SelectStageVO, T extends RepeatRandomValueSelectStrategyVO> RepeatRandomValueSelectStrategy<S, T> repeatRandomSelectStrategy() {
        return new RepeatRandomValueSelectStrategy<>();
    }

    @Bean
    @ConditionalOnMissingBean(OnceRandomValueSelectStrategy.class)
    public <S extends SelectStageVO, T extends OnceRandomValueSelectStrategyVO> OnceRandomValueSelectStrategy<S, T> onceRandomSelectStrategy() {
        return new OnceRandomValueSelectStrategy<>();
    }

    @Bean
    @ConditionalOnMissingBean(RepeatOrderValueSelectStrategy.class)
    public <S extends SelectStageVO, T extends RepeatOrderValueSelectStrategyVO> RepeatOrderValueSelectStrategy<S, T> repeatOrderSelectStrategy() {
        return new RepeatOrderValueSelectStrategy<>();
    }

    @Bean
    @ConditionalOnMissingBean(MultipleOrderValueSelectStrategy.class)
    public <S extends SelectStageVO, T extends MultipleOrderValueSelectStrategyVO> MultipleOrderValueSelectStrategy<S, T> multipleOrderValueSelectStrategy() {
        return new MultipleOrderValueSelectStrategy<>();
    }

    @Bean
    @ConditionalOnMissingBean(OnceOrderValueSelectStrategy.class)
    public <S extends SelectStageVO, T extends OnceOrderValueSelectStrategyVO> OnceOrderValueSelectStrategy<S, T> onceOrderSelectStrategy() {
        return new OnceOrderValueSelectStrategy<>();
    }

    @Bean
    @ConditionalOnMissingBean(EqualReaderSelectStrategy.class)
    public <S extends ReadStageVO, T extends ReaderVO, R extends EqualReaderSelectStrategyVO> EqualReaderSelectStrategy<S, T, R> equalSelectStrategy() {
        return new EqualReaderSelectStrategy<>();
    }

    @Bean
    @ConditionalOnMissingBean(WeightReaderSelectStrategy.class)
    public <S extends ReadStageVO, T extends ReaderVO, R extends WeightReaderSelectStrategyVO> WeightReaderSelectStrategy<S, T, R> weightSelectStrategy() {
        return new WeightReaderSelectStrategy<>();
    }

}
