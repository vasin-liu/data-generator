/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.ScriptStagePO;
import org.gensokyo.data.po.WriteStagePO;
import org.gensokyo.data.script.ScriptFactory;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * 数据脚本处理阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
public class ScriptStage extends AbstractStage {
    private ScriptFactory scriptFactory;

    public ScriptStage(StageContext ctx) {
        super(ctx);
    }

    @Autowired
    public void setScriptFactory(ScriptFactory scriptFactory) {
        this.scriptFactory = scriptFactory;
    }

    @Override
    public Value internalExecute(Value input) {
        if (ctx.stage() instanceof ScriptStagePO spo) {
            try (var script = scriptFactory.newInstance(spo)) {
                if (Objects.nonNull(script)) {
                    return script.eval(input);
                }
            } catch (Exception e) {
                log.error(String.format("执行脚本[%s]出现异常：", JsonKit.write(ctx.stage())), e);
            }
            return input;
        }
        throw new DataGeneratorException(String.format("当前阶段要求的配置值类型为：[%s] ，实际的配置值类型为：[%s]",
                ScriptStagePO.class.getName(), ctx.stage().getClass().getName()));
    }
}
