/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.yaml;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.scalar.ScalarSerializer;
import org.apache.logging.log4j.util.Strings;
import org.gensokyo.data.generator.converter.StringConverter;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 类对象解析器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/6 , Version 1.0.0
 */
public class ClassSerializer implements ScalarSerializer<Class<?>> {

    @Override
    public String write(Class<?> object) throws YamlException {
        if (Objects.isNull(object)) {
            return Strings.EMPTY;
        }
        return object.getName();
    }

    @Override
    public Class<?> read(String value) throws YamlException {
        if (!StringUtils.hasText(value)) {
            return StringConverter.class;
        }
        try {
            return Class.forName(value);
        } catch (ClassNotFoundException e) {
            throw new DataGeneratorException(e);
        }
    }
}
