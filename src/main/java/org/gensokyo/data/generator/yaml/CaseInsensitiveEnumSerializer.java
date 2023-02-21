/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.yaml;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.scalar.ScalarSerializer;

/**
 * 忽略大小写的枚举解析
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
public class CaseInsensitiveEnumSerializer<T extends Enum<T>> implements ScalarSerializer<T> {

    private final Class<T> classType;

    public CaseInsensitiveEnumSerializer(final Class<T> classType) {
        this.classType = classType;
    }

    @Override
    public String write(T object) throws YamlException {
        return object.name().toLowerCase();
    }

    @Override
    public T read(String value) throws YamlException {
        return Enum.valueOf(classType, value.toUpperCase());
    }
}
