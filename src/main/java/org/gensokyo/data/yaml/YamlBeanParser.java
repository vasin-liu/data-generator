/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.yaml;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlReader;
import org.gensokyo.data.constant.FieldType;
import org.gensokyo.data.constant.ReaderType;
import org.gensokyo.data.constant.ScriptType;
import org.gensokyo.data.constant.WriterType;
import org.gensokyo.data.exception.DataGeneratorException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

/**
 * YamlBean解析器实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/27 , Version 1.0.0
 */
public class YamlBeanParser implements YamlParser {

    private final YamlConfig config = new YamlConfig();

    public YamlBeanParser() {
        config.setScalarSerializer(ScriptType.class, new CaseInsensitiveEnumSerializer<>(ScriptType.class));
        config.setScalarSerializer(FieldType.class, new CaseInsensitiveEnumSerializer<>(FieldType.class));
        config.setScalarSerializer(ReaderType.class, new CaseInsensitiveEnumSerializer<>(ReaderType.class));
        config.setScalarSerializer(WriterType.class, new CaseInsensitiveEnumSerializer<>(WriterType.class));
        config.setScalarSerializer(Class.class, new ClassSerializer());
    }

    @Override
    public <T> T parse(File file, Class<T> clazz) {
        if (Objects.isNull(file) || Objects.isNull(clazz)) {
            return null;
        }
        try {
            var reader = new YamlReader(new FileReader(file), config);
            return reader.read(clazz);
        } catch (IOException e) {
            throw new DataGeneratorException("文件 [ " + file.getAbsolutePath() + " ] 解析出现异常", e);
        }
    }
}
