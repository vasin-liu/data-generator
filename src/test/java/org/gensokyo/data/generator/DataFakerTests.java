/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import org.gensokyo.data.generator.faker.DataFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

/**
 * 数据生成测试类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/2/8 , Version 1.0.0
 */
class DataFakerTests {

    @Test
    void case1() {
        var faker = new DataFaker(Locale.CHINA);
        var vp = faker.vehicleCN();
        var coach = vp.coach();
        var normal = vp.normal();
        var hongkong = vp.hongkong();
        var macau = vp.macau();
        var police = vp.police();
        Assertions.assertNotNull(coach);
    }
}
