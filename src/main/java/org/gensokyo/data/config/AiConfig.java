/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.data.ai.parser.ListOutputParser;
import org.gensokyo.data.ai.parser.OutputParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * AI配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/1 , Version 1.0.0
 */
@Configuration
public class AiConfig {

    @Bean
    @ConditionalOnMissingBean(OutputParser.class)
    public OutputParser<?> outputParser(DefaultConversionService conversionService) {
        return new ListOutputParser(conversionService);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultConversionService.class)
    public DefaultConversionService conversionService() {
        return new DefaultConversionService();
    }
}
