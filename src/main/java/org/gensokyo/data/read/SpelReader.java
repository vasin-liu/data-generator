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
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spring 表达式引擎读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/26 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class SpelReader implements Reader {
    private final Pattern p = Pattern.compile("^\\{.+\\}$|^((?!\\{).+(?!\\}))\\[(\\d+)\\]$");

    private final DataFaker dataFaker;


    @SuppressWarnings("unchecked")
    @Override
    public Value read(final ReaderContext ctx, final Value input) {
        var parser = new SpelExpressionParser();
        var sec = new StandardEvaluationContext();
        sec.addPropertyAccessor(new MapAccessor());
        sec.setVariable(Const.SCRIPT_VAR_FAKER, Objects.requireNonNull(dataFaker));
        var rpo = ctx.reader();
        if (rpo.getDataSet() instanceof String dataset) {
            try {
                Matcher m = p.matcher(dataset);
                if (m.find()) {
                    var num = m.group(2);
                    if (Objects.isNull(num)) {
                        //原生SPEL表达式
                        var el = m.group(0);
                        var evalResult = parser.parseExpression(el).getValue(sec, List.class);
                        return ListValue.fromObjectCollection(evalResult);
                    } else {
                        //自定义SPEL表达式
                        var list = new ArrayList<>();
                        var el = m.group(1);
                        for (int i = 0; i < Integer.parseInt(num); i++) {
                            list.add(parser.parseExpression(el).getValue(sec));
                        }
                        return ListValue.fromObjectCollection(list);
                    }
                }
                throw new DataGeneratorException(String.format("字段 %s 所配置的类型为：%s 的表达式 %s 目前暂时不支持",
                        ctx.field().getName(), rpo.getType(), rpo.getDataSet()));
            } catch (Exception e) {
                throw new DataGeneratorException(String.format("字段 %s 在执行 %s 类型表达式 %s 出现异常：",
                        ctx.field().getName(), rpo.getType(), rpo.getDataSet()), e);
            }
        }
        return Value.EMPTY;
    }
}
