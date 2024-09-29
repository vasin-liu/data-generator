/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.json;

import com.fasterxml.jackson.annotation.JacksonAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SPI方式加载子类型注解
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/9 , Version 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonSubType {
    /**
     * 子类型唯一标识符，解析时统一将标识符转换为大写，再进行匹配（不允许多个子类型使用相同的标识符）
     *
     * @return 子类型标识符
     */
    String value() default "";

    /**
     * 是否为默认实现类
     *
     * @return true/false
     */
    boolean isDefault() default false;
}
