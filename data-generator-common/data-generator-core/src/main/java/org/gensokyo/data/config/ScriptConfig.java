/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.data.model.vo.scripter.PlainScriptVO;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.script.PlainScript;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 脚本处理器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Configuration
public class ScriptConfig {

    @Bean
    @ConditionalOnMissingBean(PlainScript.class)
    public <S extends ScriptStageVO, T extends PlainScriptVO> PlainScript<S, T> plainScript() {
        return new PlainScript<>();
    }
}
