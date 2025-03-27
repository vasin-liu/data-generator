/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.DataSet;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
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
public class ScriptStage extends AbstractStage<ScriptStageVO> {
    private ScriptFactory scriptFactory;

    public ScriptStage(StageContext<ScriptStageVO> ctx) {
        super(ctx);
    }

    @Autowired
    public void setScriptFactory(ScriptFactory scriptFactory) {
        this.scriptFactory = scriptFactory;
    }

    @Override
    public Value internalExecute(Value input) {
        var spo = ctx.stage().getLanguage();
        if (Objects.isNull(spo)) {
            return input;
        }
        try {
            var script = scriptFactory.newInstance(spo);
            if (Objects.nonNull(script)) {
                var dataset = DataSet.getOrCreate(ctx.template().getId(), ctx.template().getInstanceId());
                return script.eval(StageContext.from(ctx), spo, input, dataset);
            }
            return input;
        } catch (Exception e) {
            var msg = String.format("执行数据脚本处理阶段失败，输入值为：%s，上下文信息为：%s", input, JsonKit.write(ctx));
            throw new DataGeneratorException(msg, e);
        }
    }
}
