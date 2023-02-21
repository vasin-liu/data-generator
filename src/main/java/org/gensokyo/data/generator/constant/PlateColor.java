/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.constant;

/**
 * 号牌颜色
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/29 , Version 1.0.0
 */
public enum PlateColor {
    BLUE, BLACK, WHITE, YELLOW, GREEN, GRADIENT_GREEN, GRADIENT_YELLOW_GREEN;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
