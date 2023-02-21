/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.util;

import org.joda.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 时间工具测试类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/2/1 , Version 1.0.0
 */
class DatetimeKitTests {

    @Test
    void case1() {
        String format1 = DatetimeKit.humanized(Duration.millis(231231));
        String format2 = DatetimeKit.humanized(23123131);
        String format3 = DatetimeKit.humanized(28493);
        String format4 = DatetimeKit.humanized(284930000);
        String format5 = DatetimeKit.humanized(999999999);
        System.out.println(format1);
        System.out.println(format2);
        System.out.println(format3);
        System.out.println(format4);
        System.out.println(format5);
        Assertions.assertEquals("3分钟, 51秒, 231毫秒", format1);
    }
}
