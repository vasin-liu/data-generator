/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.util;

import java.util.Objects;

/**
 * 布尔工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/12 , Version 1.0.0
 */
public final class BoolKit {

    private BoolKit() {
        throw new UnsupportedOperationException();
    }

    public static boolean unboxing(Boolean value) {
        if (Objects.isNull(value)) {
            return false;
        }
        return value;
    }
}
