/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.gensokyo.data.exception.DataGeneratorException;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

/**
 * Jackson解析器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/27 , Version 1.0.0
 */
public class JacksonParser implements YamlParser {
    private final ObjectMapper mapper;

    public JacksonParser() {
        mapper = JsonMapper.builder(new YAMLFactory())
                //忽略枚举大小写
                .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
                //添加Jackson模块
                .findAndAddModules()
                .build();
    }

    @Override
    public <T> T parse(File file, Class<T> clazz) {
        if (Objects.isNull(file) || Objects.isNull(clazz)) {
            return null;
        }
        try {
            return mapper.readValue(file, clazz);
        } catch (IOException e) {
            throw new DataGeneratorException("文件 [ " + file.getAbsolutePath() + " ] 解析出现异常", e);
        }
    }
}
