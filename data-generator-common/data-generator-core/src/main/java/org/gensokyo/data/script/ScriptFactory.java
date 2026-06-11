/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.script;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.gensokyo.data.model.vo.scripter.ScriptVO;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.script.vars.Variable;
import org.gensokyo.data.util.TypeKit;
import org.gensokyo.kit.collect.MapKit;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
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

    private final ApplicationContext ctx;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public @Nullable <S extends ScriptStageVO, T extends ScriptVO> Script<S, T> newInstance(T svo) {
        if (Objects.isNull(svo) || Objects.isNull(svo.getType())) {
            log.error("数据脚本处理器类型或者脚本类型为空，无法创建数据脚本处理器");
            return null;
        }

        Script<S, T> script = null;
        Map<String, Script> services = ctx.getBeansOfType(Script.class);

        for (Script<?, ?> service : services.values()) {
            if (TypeKit.isMatchingType(Script.class, service, ScriptStageVO.class, svo.getClass())) {
                script = (Script<S, T>) service;
            }
        }

        Assert.notNull(script, "未找到类型为 " + svo.getType() + " 的数据脚本处理器类");
        return script;
    }

    public Collection<Variable> getVariables() {
        Map<String, Variable> services = ctx.getBeansOfType(Variable.class);
        return MapKit.isNotEmpty(services) ? services.values() : Collections.emptyList();
    }
}
