/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.script;

import org.gensokyo.data.generator.constant.Const;
import org.gensokyo.data.generator.domain.Context;
import org.gensokyo.data.generator.domain.ScriptPO;
import org.gensokyo.data.generator.faker.DataFaker;
import org.gensokyo.kit.character.StrKit;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Objects;

/**
 * SPEL表达式引擎脚本处理
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/30 , Version 1.0.0
 */
public class SpelScript implements Script {

    private SpelExpressionParser parser;
    private StandardEvaluationContext sec;
    private ScriptPO script;

    public SpelScript(final ScriptPO script, final Context ctx, final DataFaker dataFaker) {
        this.script = Objects.requireNonNull(script);
        this.parser = new SpelExpressionParser();
        this.sec = new StandardEvaluationContext();
        this.sec.addPropertyAccessor(new MapAccessor());
        this.sec.setVariable(Const.SCRIPT_VAR_FAKER, Objects.requireNonNull(dataFaker));
        this.sec.setVariable(Const.SCRIPT_VAR_CTX, ctx);
    }

    @Override
    public Object eval(Object dataset, Object... args) {
        if (StrKit.isNotBlank(script.getContent())) {
            this.sec.setVariable(Const.SCRIPT_VAR_DATASET, dataset);
            return parser.parseExpression(script.getContent()).getValue(sec);
        }
        return dataset;
    }

    @Override
    public void close() throws Exception {
        this.sec.setVariable(Const.SCRIPT_VAR_FAKER, null);
        this.sec.setVariable(Const.SCRIPT_VAR_CTX, null);
        this.sec.setVariable(Const.SCRIPT_VAR_DATASET, null);
        //set null
        this.sec = null;
        this.script = null;
        this.parser = null;
    }
}
