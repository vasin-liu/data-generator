/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.data.cache.Templates;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.yaml.JacksonParser;
import org.gensokyo.data.yaml.YamlParser;
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
public class CoreConfig {

    @Bean
    @ConditionalOnMissingBean(JacksonParser.class)
    public JacksonParser jacksonParser() {
        return new JacksonParser();
    }

    @Bean
    @ConditionalOnMissingBean(Templates.class)
    public Templates templates(DataGeneratorProperties props,
                               YamlParser yamlParser,
                               TemplateRepository repository) {
        return new Templates(props, yamlParser, repository);
    }
}
