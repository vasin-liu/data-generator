/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.read;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.ReaderContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.faker.DataFaker;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Spring 表达式引擎读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/26 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DirectSpelReader implements Reader {

    private final DataFaker dataFaker;

    @Override
    public Value read(final ReaderContext ctx, final Value input) {
        var parser = new SpelExpressionParser();
        var sec = new StandardEvaluationContext();
        sec.setVariable(Const.SCRIPT_VAR_FAKER, Objects.requireNonNull(dataFaker));
        final String rightBrace1 = "{";
        final String rightBrace2 = "#{";
        final String leftBrace = "}";
        if (ctx.reader().getDataSet() instanceof String dataset) {
            try {
                if (!dataset.startsWith(rightBrace1) && !dataset.startsWith(rightBrace2)) {
                    dataset = rightBrace1.concat(dataset);
                }
                if (!dataset.endsWith(leftBrace)) {
                    dataset = dataset.concat(leftBrace);
                }
                final String script = dataset;
                Supplier<Object> evalResult = () -> parser.parseExpression(script).getValue(sec, List.class);
                return SingleValue.of(evalResult);
            } catch (Exception e) {
                throw new DataGeneratorException(String.format("字段 %s 在执行 %s 类型表达式 %s 出现异常：",
                        ctx.field().getName(), ctx.reader().getType(), ctx.reader().getDataSet()), e);
            }
        }
        return Value.EMPTY;
    }
}

