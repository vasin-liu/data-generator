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
import org.gensokyo.data.po.reader.SpelReaderPO;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
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
public class SpelReader<T extends SpelReaderPO> implements Reader<T> {

    private final DataFaker dataFaker;

    @Override
    public Value read(final ReaderContext<T> ctx, final Value input) {
        var parser = new SpelExpressionParser();
        var sec = new StandardEvaluationContext();
        sec.addPropertyAccessor(new MapAccessor());
        sec.setVariable(Const.SCRIPT_VAR_FAKER, Objects.requireNonNull(dataFaker));
        final String rightBrace1 = "{";
        final String rightBrace2 = "#{";
        final String leftBrace = "}";
        var rpo = ctx.reader();
        var exp = rpo.getExp();
        try {
            if (!exp.startsWith(rightBrace1) && !exp.startsWith(rightBrace2)) {
                exp = rightBrace1.concat(exp);
            }
            if (!exp.endsWith(leftBrace)) {
                exp = exp.concat(leftBrace);
            }
            final String script = exp;
            var list = new ArrayList<>();
            var times = Math.max(rpo.getTimes(), 1);
            for (int i = 0; i < times; i++) {
                Supplier<Object> evalResult = () -> parser.parseExpression(script).getValue(sec, List.class);
                list.add(evalResult);
            }
            return ListValue.fromObjectCollection(list);
        } catch (Exception e) {
            throw new DataGeneratorException(String.format("字段 %s 在执行 %s 类型表达式 %s 出现异常：",
                    ctx.field().getName(), rpo.getType(), exp), e);
        }
    }
}
