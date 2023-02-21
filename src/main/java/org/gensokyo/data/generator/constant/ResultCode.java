/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.constant;

/**
 * 响应代码枚举
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 1.0.0
 */
public enum ResultCode {
    /**
     * 请求成功
     */
    OK("0", "请求成功"),
    /**
     * 请求失败
     */
    FAILURE("9", "请求失败");

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return this.code;
    }

    public String message() {
        return this.message;
    }
}
