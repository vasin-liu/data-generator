/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.script;

import org.gensokyo.data.constant.Const;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;
import org.junit.jupiter.api.Test;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * SPEL测试类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/7 , Version 1.0.0
 */
class SpelTests {

    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Test
    void case_array_value_1() {
        Value[] values = new Value[]{
                new MapValue() {{
                    put("a", SingleValue.of("1"));
                    put("b", SingleValue.of("2"));
                }}
        };
        var sec = new StandardEvaluationContext();
        sec.addPropertyAccessor(new ArrayValuePropertyAccessor());
        sec.addPropertyAccessor(new MapValuePropertyAccessor());
        sec.addPropertyAccessor(new ListValuePropertyAccessor());
        sec.addPropertyAccessor(new MapAccessor());
        sec.addPropertyAccessor(new SingleValuePropertyAccessor());
        sec.setRootObject(values);
        sec.setVariable(Const.SCRIPT_VAR_DATASET, values);

        Object result = parser.parseExpression("[0]['b']").getValue(sec);
        System.out.println("===>" + result);
    }

    @Test
    void case_array_value_2() {
        Value[] values = new Value[]{
                new MapValue() {{
                    put("a", SingleValue.of("1"));
                    put("b", SingleValue.of("2"));
                }},
                new MapValue() {{
                    put("c", SingleValue.of("3"));
                    put("d", SingleValue.of("4"));
                }}
        };
        var sec = new StandardEvaluationContext();
        sec.addPropertyAccessor(new ArrayValuePropertyAccessor());
        sec.addPropertyAccessor(new MapValuePropertyAccessor());
        sec.addPropertyAccessor(new ListValuePropertyAccessor());
        sec.addPropertyAccessor(new MapAccessor());
        sec.addPropertyAccessor(new SingleValuePropertyAccessor());
        sec.setVariable(Const.SCRIPT_VAR_DATASET, values);

        Object result = parser.parseExpression("#dataset[1]['c']").getValue(sec);
        System.out.println("===>" + result);
    }

    @Test
    void case_map_value() {

    }

    @Test
    void case_list_value() {

    }

    @Test
    void case_single_value_1() {
        Value value = SingleValue.of("1");
        var sec = new StandardEvaluationContext();
        sec.addPropertyAccessor(new ArrayValuePropertyAccessor());
        sec.addPropertyAccessor(new MapValuePropertyAccessor());
        sec.addPropertyAccessor(new ListValuePropertyAccessor());
        sec.addPropertyAccessor(new MapAccessor());
        sec.addPropertyAccessor(new SingleValuePropertyAccessor());
        sec.setRootObject(value);
        sec.setVariable(Const.SCRIPT_VAR_DATASET, value);

        Object result = parser.parseExpression("#dataset").getValue(sec);
        System.out.println("===>" + result);
    }
}
