/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.faker;

import net.datafaker.Faker;
import org.gensokyo.data.generator.constant.Const;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SPEL测试类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/6 , Version 1.0.0
 */
class SpelTests {

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final StandardEvaluationContext context = new StandardEvaluationContext();
    private final Faker faker = new Faker(Locale.CHINA);

    @SuppressWarnings("unchecked")
    @Test
    void case1() {
        context.setVariable(Const.SCRIPT_VAR_FAKER, faker);
        String exp = "{#faker.name.fullName,#faker.name.fullName,#faker.name.fullName}";
        List<String> nameList = parser.parseExpression(exp)
                .getValue(context, List.class);
        Assertions.assertNotNull(nameList);
        System.out.println("fullName ===> " + String.join(",", nameList));
    }

    @SuppressWarnings("unchecked")
    @Test
    void case2() {
        context.setVariable(Const.SCRIPT_VAR_FAKER, faker);
        String exp = "{#faker.address.streetAddress,#faker.address.streetAddress,#faker.address.streetAddress}";
        List<String> addressList = parser.parseExpression(exp)
                .getValue(context, List.class);
        Assertions.assertNotNull(addressList);
        System.out.println("address ===> " + String.join(",", addressList));
    }

    @SuppressWarnings("unchecked")
    @Test
    void case3() {
        context.setVariable(Const.SCRIPT_VAR_FAKER, faker);
        String exp = "{#faker.expression(\"#{letterify 'test????test'}\"),#faker.expression(\"#{letterify 'test????test'}\"),#faker.expression(\"#{letterify 'test????test'}\")}";
        List<String> addressList = parser.parseExpression(exp)
                .getValue(context, List.class);
        Assertions.assertNotNull(addressList);
        System.out.println("fullName ===> " + String.join(",", addressList));
    }

    @Test
    void case4() {
        String exp = "{#faker.expression(\"#{letterify 'test????test'}\")}[4]";
        String exp2 = "{#faker.expression(\"#{letterify 'test????test'}\")}";
        String exp3 = "#faker.expression(\"#{letterify 'test????test'}\")[4]";
        String exp4 = "#faker.expression(\"#{letterify 'test????test'}\")";
        String exp5 = "#faker.expression(\"#{letterify 'test????test'}\")[4.1]";
        var r1 = "^\\{.+\\}$";
        var r2 = "^((?!\\{).+(?!\\}))\\[(\\d+)\\]$";
        var r3 = r1 + "|" + r2;
        Assertions.assertFalse(exp.matches(r1));
        Assertions.assertTrue(exp2.matches(r1));
        Assertions.assertTrue(exp3.matches(r2));
        Assertions.assertFalse(exp4.matches(r2));
        Assertions.assertFalse(exp5.matches(r2));

        Assertions.assertFalse(exp.matches(r3));
        Assertions.assertTrue(exp2.matches(r3));
        Assertions.assertTrue(exp3.matches(r3));
        Assertions.assertFalse(exp4.matches(r3));
        Assertions.assertFalse(exp5.matches(r3));
        Pattern p = Pattern.compile(r3);
        Matcher m = p.matcher(exp2);
        if (m.find()) {
            System.out.println(m.groupCount());
        }
    }

    @Test
    void case5() {
        context.setVariable(Const.SCRIPT_VAR_FAKER, faker);
        String exp = "#faker.expression(\"#{date.past '1','DAYS','yyMMddHHmmss'}\")";
        String str = parser.parseExpression(exp).getValue(context, String.class);
        Assertions.assertNotNull(str);
        System.out.println("date ===> " + str);
    }
}
