/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.factory;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.generator.domain.Context;
import org.gensokyo.data.generator.domain.ScriptPO;
import org.gensokyo.data.generator.faker.DataFaker;
import org.gensokyo.data.generator.script.JsScript;
import org.gensokyo.data.generator.script.Script;
import org.gensokyo.data.generator.script.SpelScript;
import org.gensokyo.kit.character.StrKit;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.Nullable;

import java.util.Objects;

/**
 * 脚本工厂
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@RequiredArgsConstructor
public class ScriptFactory implements Factory {
    private final AutowireCapableBeanFactory beanFactory;
    private final DataFaker dataFaker;

    @SuppressWarnings("resource")
    public @Nullable Script newInstance(ScriptPO spo, Context ctx) {
        if (Objects.isNull(spo) || Objects.isNull(spo.getType()) || StrKit.isBlank(spo.getContent())) {
            return null;
        }
        var script = switch (spo.getType()) {
            case JAVASCRIPT -> new JsScript(spo, ctx);
            case SPEL -> new SpelScript(spo, ctx, dataFaker);
        };
        beanFactory.autowireBean(script);
        return script;
    }
}
