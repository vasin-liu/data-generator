/*
 * Copyright 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 */
package org.gensokyo.data.yaml;

import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.generator.GeneratorVO;
import org.gensokyo.data.model.vo.iterator.IteratorVO;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.scripter.ScriptVO;
import org.gensokyo.data.model.vo.selector.reader.ReaderSelectStrategyVO;
import org.gensokyo.data.model.vo.selector.value.ValueSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.gensokyo.kit.character.StrKit;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ServiceLoader;
import java.util.ArrayList;
import java.util.Objects;

import static tools.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

public class JacksonParser implements YamlParser {
    private final ObjectMapper mapper;

    public JacksonParser() {
        mapper = new ObjectMapper(new YAMLFactory())
                .rebuild()
                .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
                .findAndAddModules()
                .registerSubtypes(loadSubtypes(GeneratorVO.class))
                .registerSubtypes(loadSubtypes(IteratorVO.class))
                .registerSubtypes(loadSubtypes(ReaderVO.class))
                .registerSubtypes(loadSubtypes(ScriptVO.class))
                .registerSubtypes(loadSubtypes(ReaderSelectStrategyVO.class))
                .registerSubtypes(loadSubtypes(ValueSelectStrategyVO.class))
                .registerSubtypes(loadSubtypes(StageVO.class))
                .registerSubtypes(loadSubtypes(WriterVO.class))
                .registerSubtypes(loadSubtypes(SourceVO.class))
                .registerSubtypes(loadSubtypes(TransformVO.class))
                .build();
    }

    private <T> NamedType[] loadSubtypes(Class<T> parent) {
        var subtypes = new ArrayList<NamedType>();
        for (T instance : ServiceLoader.load(parent)) {
            Class<?> subtype = instance.getClass();
            JsonSubType annotation = subtype.getAnnotation(JsonSubType.class);
            if (annotation == null || StrKit.isBlank(annotation.value())) {
                subtypes.add(new NamedType(subtype));
            } else {
                var typeId = annotation.value();
                subtypes.add(new NamedType(subtype, typeId));
                subtypes.add(new NamedType(subtype, typeId.toLowerCase()));
            }
        }
        return subtypes.toArray(NamedType[]::new);
    }

    @Override
    public <T> T parse(File file, Class<T> clazz) {
        if (Objects.isNull(file) || Objects.isNull(clazz)) {
            return null;
        }
        try {
            return mapper.readValue(file, clazz);
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
            return mapper.readValue(is, clazz);
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
            return mapper.readValue(content, clazz);
        }
        catch (JacksonException e) {
            throw new DataGeneratorException("Failed to parse YAML content [" + content + "]", e);
        }
    }
}
