/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.yaml;

import java.io.File;
import java.io.InputStream;

/**
 * Yaml文件解析接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/27 , Version 1.0.0
 */
public interface YamlParser {

    /**
     * 解析yaml文件
     *
     * @param file  yaml文件
     * @param clazz 解析的类
     * @param <T>   解析的对象类型
     * @return 解析后的对象
     */
    <T> T parse(File file, Class<T> clazz);

    /**
     * 解析yaml文件
     *
     * @param is    yaml输入流
     * @param clazz 解析的类
     * @param <T>   解析的对象类型
     * @return 解析后的对象
     */
    <T> T parse(InputStream is, Class<T> clazz);

    /**
     * 解析yaml文件
     *
     * @param content yaml文件内容
     * @param clazz   解析的类
     * @param <T>     解析的对象类型
     * @return 解析后的对象
     */
    <T> T parse(String content, Class<T> clazz);
}
