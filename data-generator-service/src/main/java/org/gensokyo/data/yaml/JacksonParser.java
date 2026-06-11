/*
 * Copyright 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 */
package org.gensokyo.data.yaml;

import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.json.JsonSubtypeRegistry;
import org.gensokyo.data.json.TemplateObjectMapperFactory;
import org.gensokyo.kit.character.StrKit;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;

import static tools.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

public class JacksonParser implements YamlParser {
    private volatile ObjectMapper mapper;
    private volatile long mapperVersion = Long.MIN_VALUE;

    public JacksonParser() {
    }

    private ObjectMapper mapper() {
        long currentVersion = JsonSubtypeRegistry.version();
        ObjectMapper current = mapper;
        if (current == null || mapperVersion != currentVersion) {
            synchronized (this) {
                current = mapper;
                if (current == null || mapperVersion != currentVersion) {
                    mapper = buildMapper();
                    mapperVersion = currentVersion;
                    current = mapper;
                }
            }
        }
        return current;
    }

    private ObjectMapper buildMapper() {
        var builder = new ObjectMapper(new YAMLFactory()).rebuild();
        TemplateObjectMapperFactory.registerTemplateSubtypes(builder);
        return builder
                .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
                .findAndAddModules()
                .build();
    }

    @Override
    public <T> T parse(File file, Class<T> clazz) {
        if (Objects.isNull(file) || Objects.isNull(clazz)) {
            return null;
        }
        try {
            return mapper().readValue(file, clazz);
        }
        catch (JacksonException e) {
            throw new DataGeneratorException("Failed to parse YAML file [" + file.getAbsolutePath() + "]", e);
        }
    }

    @Override
    public <T> T parse(InputStream is, Class<T> clazz) {
        if (Objects.isNull(is) || Objects.isNull(clazz)) {
            return null;
        }
        try {
            return mapper().readValue(is, clazz);
        }
        catch (JacksonException e) {
            throw new DataGeneratorException("Failed to parse YAML input stream", e);
        }
    }

    @Override
    public <T> T parse(String content, Class<T> clazz) {
        if (StrKit.isBlank(content) || Objects.isNull(clazz)) {
            return null;
        }
        try {
            return mapper().readValue(content, clazz);
        }
        catch (JacksonException e) {
            throw new DataGeneratorException("Failed to parse YAML content [" + content + "]", e);
        }
    }

    @Override
    public String dump(Object value) {
        if (Objects.isNull(value)) {
            return null;
        }
        try {
            return mapper().writeValueAsString(value);
        }
        catch (JacksonException e) {
            throw new DataGeneratorException("Failed to serialize object to YAML", e);
        }
    }
}
