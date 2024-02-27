/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.constant;

/**
 * 全局常量定义
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 1.0.0
 */
public final class Const {
    private Const() {
        throw new UnsupportedOperationException();
    }

    public static final int BATCH_SIZE = 1000;

    public static final String COMMA = ",";
    public static final String COLON = ":";
    public static final String DOT = ".";
    public static final String VERTICAL = "|";
    public static final String NULL = "\\N";
    public static final String LF = "\n";
    public static final String CRLF = "\r\n";

    public static final String SCRIPT_VAR_CTX = "ctx";
    public static final String SCRIPT_VAR_FAKER = "faker";
    public static final String SCRIPT_VAR_DATASET = "dataset";
}
