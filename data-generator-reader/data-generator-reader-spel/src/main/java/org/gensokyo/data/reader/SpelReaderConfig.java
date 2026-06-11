/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import org.gensokyo.data.faker.DataFaker;
import org.gensokyo.data.faker.DataFakerConfig;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spel读取器配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/25 , Version 1.0.0
 */
@Configuration
@AutoConfigureAfter(DataFakerConfig.class)
public class SpelReaderConfig {

    @Bean
    @ConditionalOnMissingBean(SpelReader.class)
    public SpelReader<ReadStageVO, SpelReaderVO> spelReader(DataFaker dataFaker) {
        return new SpelReader<>(dataFaker);
    }
}
