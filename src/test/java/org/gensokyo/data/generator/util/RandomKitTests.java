/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.util;

import net.datafaker.Faker;
import org.gensokyo.data.util.RandomKit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * 随机工具测试类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/11 , Version 1.0.0
 */
class RandomKitTests {

    @Test
    void case1() {
        var str = RandomKit.alpha(6);
        Assertions.assertEquals(6, str.length());
    }

    @Test
    void case2() {
        var str = RandomKit.alphaUpper(6);
        Assertions.assertEquals(6, str.length());
    }

    @Test
    void case3() {
        var str = RandomKit.alphaLower(6);
        Assertions.assertEquals(6, str.length());
    }

    @Test
    void case4() {
        var str = RandomKit.alphanumeric(6);
        Assertions.assertEquals(6, str.length());
    }

    @Test
    void case5() {
        var str = RandomKit.numeric(6);
        Assertions.assertEquals(6, str.length());
    }

    @Test
    void case6() {
        var faker = new Faker(Locale.CHINA);
        var v = faker.vehicle();
        var pn = v.licensePlate();
        System.out.println(pn);
        Assertions.assertNotNull(pn);
    }

    @Test
    void case7() {
        IntStream.rangeClosed('A', 'Z')
                .filter(value -> !List.of('I', 'O').contains((char) value))
                .mapToObj(value -> String.valueOf((char) value)).toList()
                .forEach(System.out::println);
        System.out.println(Math.random());
        System.out.println(RandomKit.id());
        Assertions.assertTrue(RandomKit.id() > 0);
    }
}
