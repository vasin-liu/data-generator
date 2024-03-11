/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import org.gensokyo.data.util.RandomKit;
import org.gensokyo.kit.json.JsonKit;
import org.junit.jupiter.api.Test;

/**
 * 随机工具测试类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/5 , Version 1.0.0
 */
class RandomKitTest {

    @Test
    void case1() {
        System.out.println(JsonKit.write(RandomKit.seq(0, 10)));
    }
}
