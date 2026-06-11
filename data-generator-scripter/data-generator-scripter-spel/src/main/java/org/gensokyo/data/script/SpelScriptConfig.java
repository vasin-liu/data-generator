/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.script;

import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SPEL脚本配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/25 , Version 1.0.0
 */
@Configuration
public class SpelScriptConfig {

    @Bean
    @ConditionalOnMissingBean(ScriptFactory.class)
    public ScriptFactory scriptFactory(ApplicationContext ctx) {
        return new ScriptFactory(ctx);
    }

    @Bean
    @ConditionalOnMissingBean(SpelScript.class)
    public <S extends ScriptStageVO, T extends SpelScriptVO> SpelScript<S, T> spelScript(ScriptFactory scriptFactory) {
        return new SpelScript<>(scriptFactory);
    }
}
