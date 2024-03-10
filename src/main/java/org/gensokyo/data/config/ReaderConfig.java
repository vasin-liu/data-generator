/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.data.faker.DataFaker;
import org.gensokyo.data.read.ConstantReader;
import org.gensokyo.data.read.DirectSpelReader;
import org.gensokyo.data.read.JdbcReader;
import org.gensokyo.data.read.SpelReader;
import org.gensokyo.data.script.ScriptFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * 读取器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Configuration
public class ReaderConfig {

    @Bean
    @ConditionalOnMissingBean(ConstantReader.class)
    public ConstantReader constantReader() {
        return new ConstantReader();
    }

    @Bean
    @ConditionalOnMissingBean(JdbcReader.class)
    public JdbcReader jdbcReader(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                 ScriptFactory scriptFactory) {
        return new JdbcReader(namedParameterJdbcTemplate, scriptFactory);
    }

    @Bean
    @ConditionalOnMissingBean(SpelReader.class)
    public SpelReader spelReader(DataFaker dataFaker) {
        return new SpelReader(dataFaker);
    }

    @Bean
    @ConditionalOnMissingBean(DirectSpelReader.class)
    public DirectSpelReader directSpelReader(DataFaker dataFaker) {
        return new DirectSpelReader(dataFaker);
    }
}
