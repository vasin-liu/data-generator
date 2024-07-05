/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.prompt;

/**
 * 模板格式枚举
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/27 , Version 1.0.0
 */
public enum TemplateFormat {

    ST("ST");

    private final String value;

    TemplateFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TemplateFormat fromValue(String value) {
        for (TemplateFormat templateFormat : TemplateFormat.values()) {
            if (templateFormat.getValue().equals(value)) {
                return templateFormat;
            }
        }
        throw new IllegalArgumentException("Invalid TemplateFormat value: " + value);
    }

}
