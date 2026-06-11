/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.config;

import org.gensokyo.data.json.TemplateObjectMapperFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registers template polymorphic subtypes on Spring Boot's auto-configured {@link JsonMapper}.
 *
 * @author Gensokyo
 * @since 2026-06-04
 */
@Configuration
public class TemplateJacksonHttpConfig {

    /**
     * @return primary HTTP JSON mapper with template subtype registration (Spring Boot 4 uses {@link JsonMapper}).
     */
    @Bean
    @Primary
    public JsonMapper jsonMapper() {
        return TemplateObjectMapperFactory.buildJsonMapper();
    }
}
