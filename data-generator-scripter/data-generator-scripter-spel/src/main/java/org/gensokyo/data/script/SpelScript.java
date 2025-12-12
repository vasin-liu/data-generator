/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.script;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.script.vars.Variable;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.character.StrKit;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Collection;
import java.util.Objects;

/**
 * SPEL脚本处理类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class SpelScript<S extends ScriptStageVO, T extends SpelScriptVO> implements Script<S, T> {

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ScriptFactory scriptFactory;

    @Override
    public Value eval(final StageContext<S> ctx, final T spo, final Value dataset, Object... args) {
        if (Objects.isNull(spo) || StrKit.isBlank(spo.getContent())) {
            return dataset;
        }

        //数据解包
        var dv = DatasetKit.unwrap(dataset.get());
        try {
            var sec = new StandardEvaluationContext();
            sec.addPropertyAccessor(new MapAccessor());
            sec.addPropertyAccessor(new SingleValuePropertyAccessor());
            sec.setVariable(Const.SCRIPT_VAR_DATASET, dv);
            if (spo.isDatasetAsRootObject()) {
                sec.setRootObject(dv);
            }
            sec.setVariable(Const.SCRIPT_VAR_ARGS, args);
            Collection<Variable> variables = scriptFactory.getVariables();
            for (Variable variable : variables) {
                sec.setVariable(variable.name(), variable.value());
            }
            Object result = parser.parseExpression(spo.getContent()).getValue(sec);
            return DatasetKit.toValue(result);
        } catch (Exception e) {
            throw new DataGeneratorException(String.format("执行脚本出现异常，脚本类型：%s，脚本内容：%s，执行对象值为：%s",
                    spo.getType(), spo.getContent(), dv), e);
        }
    }
}
