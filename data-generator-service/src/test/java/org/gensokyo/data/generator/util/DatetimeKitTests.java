/*
 * Copyright 漏 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address锛歅CI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou锛孋hina锛圸ip code锛?10653锛?
 */
package org.gensokyo.data.generator.util;

import org.gensokyo.data.util.DatetimeKit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * 鏃堕棿宸ュ叿娴嬭瘯绫?
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/2/1 , Version 1.0.0
 */
class DatetimeKitTests {

    @Test
    void case1() {
        String format1 = DatetimeKit.humanized(Duration.ZERO);
        String format2 = DatetimeKit.humanized(23123131);
        String format3 = DatetimeKit.humanized(28493);
        String format4 = DatetimeKit.humanized(284930000);
        String format5 = DatetimeKit.humanized(999999999);
        System.out.println(format1);
        System.out.println(format2);
        System.out.println(format3);
        System.out.println(format4);
        System.out.println(format5);
        Assertions.assertEquals("", format1);
        Assertions.assertEquals("28\u79d2, 493\u6beb\u79d2", format3);
    }
}
