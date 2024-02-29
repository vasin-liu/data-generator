/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.po.ScriptPO;
import org.gensokyo.data.script.JsScript;
import org.gensokyo.data.script.Script;
import org.gensokyo.data.script.SpelScript;
import org.gensokyo.kit.character.StrKit;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.Nullable;

import java.util.Objects;

/**
 * 步骤工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class StageFactory {

    private final AutowireCapableBeanFactory beanFactory;

    /**
     * 根据脚本类型创建脚本实例
     *
     * @param spo 脚本对象
     * @return 脚本实例
     */
    @SuppressWarnings("resource")
    public @Nullable Script newInstance(ScriptPO spo) {
        if (Objects.isNull(spo) || Objects.isNull(spo.getType()) || StrKit.isBlank(spo.getContent())) {
            return null;
        }
        var script = switch (spo.getType()) {
            case JAVASCRIPT -> new JsScript(spo);
            case SPEL -> new SpelScript(spo);
        };
        beanFactory.autowireBean(script);
        beanFactory.initializeBean(script, script.getClass().getSimpleName());
        return script;
    }
}
