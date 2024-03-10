/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.ScriptStagePO;
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
public class ScriptStage extends AbstractStage<ScriptStagePO> {
    private ScriptFactory scriptFactory;

    public ScriptStage(StageContext<ScriptStagePO> ctx) {
        super(ctx);
    }

    @Autowired
    public void setScriptFactory(ScriptFactory scriptFactory) {
        this.scriptFactory = scriptFactory;
    }

    @Override
    public Value internalExecute(Value input) {
        var spo = ctx.stage();
        try {
            var script = scriptFactory.newInstance(spo);
            if (Objects.nonNull(script)) {
                return script.eval(spo, input);
            }
            return input;
        } catch (Exception e) {
            throw new DataGeneratorException(String.format("字段 %s 的执行脚本阶段失败，脚本类型为：%s ，脚本内容为：%s ，输入值为：%s。",
                    ctx.field().getName(), spo.getScriptType(), spo.getContent(), JsonKit.write(input.get())), e);
        }
    }
}
