/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.script;

import org.gensokyo.data.constant.Const;
import org.gensokyo.data.faker.DataFaker;
import org.gensokyo.data.po.ScriptStagePO;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.character.StrKit;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Objects;

/**
 * SPEL脚本处理类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public class SpelScript implements Script {

    private SpelExpressionParser parser;
    private StandardEvaluationContext sec;
    private ScriptStagePO spo;

    public SpelScript(final ScriptStagePO spo) {
        this.spo = Objects.requireNonNull(spo);
        this.parser = new SpelExpressionParser();
        this.sec = new StandardEvaluationContext();
        this.sec.addPropertyAccessor(new MapAccessor());
    }

    public SpelScript dataFaker(DataFaker dataFaker) {
        this.sec.setVariable(Const.SCRIPT_VAR_FAKER, Objects.requireNonNull(dataFaker));
        return this;
    }

    @Override
    public Value eval(Value dataset, Object... args) {
        if (StrKit.isNotBlank(spo.getContent())) {
            this.sec.setVariable(Const.SCRIPT_VAR_DATASET, dataset.get());
            Object result = parser.parseExpression(spo.getContent()).getValue(this.sec);
            if (Objects.nonNull(result)) {
                return SingleValue.of(result);
            }
        }
        return dataset;
    }

    @Override
    public void close() throws Exception {
        this.sec.setVariable(Const.SCRIPT_VAR_FAKER, null);
        this.sec.setVariable(Const.SCRIPT_VAR_DATASET, null);
        //set null
        this.sec = null;
        this.spo = null;
        this.parser = null;
    }
}
