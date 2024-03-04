/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.script;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.po.ScriptStagePO;
import org.gensokyo.kit.character.StrKit;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.Nullable;

import java.util.Objects;

/**
 * 脚本工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ScriptFactory {

    private final AutowireCapableBeanFactory beanFactory;

    @SuppressWarnings("resource")
    public @Nullable Script newInstance(ScriptStagePO spo) {
        if (Objects.isNull(spo) || Objects.isNull(spo.getScriptType()) || StrKit.isBlank(spo.getContent())) {
            return null;
        }
        var script = switch (spo.getScriptType()) {
            case JAVASCRIPT -> new JsScript(spo);
            case SPEL -> new SpelScript(spo);
        };
        beanFactory.autowireBean(script);
        beanFactory.initializeBean(script, script.getClass().getSimpleName());
        return script;
    }
}
