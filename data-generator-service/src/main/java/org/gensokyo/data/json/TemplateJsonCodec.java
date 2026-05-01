package org.gensokyo.data.json;

import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.json.JsonSubtypeRegistry;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.generator.GeneratorVO;
import org.gensokyo.data.model.vo.iterator.IteratorVO;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.scripter.ScriptVO;
import org.gensokyo.data.model.vo.selector.reader.ReaderSelectStrategyVO;
import org.gensokyo.data.model.vo.selector.value.ValueSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static tools.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

public final class TemplateJsonCodec {
    private static volatile ObjectMapper mapper;
    private static volatile long mapperVersion = Long.MIN_VALUE;

    private TemplateJsonCodec() {
    }

    public static String write(TemplateVO template) {
        return writeValue(template);
    }

    public static <T> String write(T value) {
        return writeValue(value);
    }

    public static <T> T read(String content, Class<T> clazz) {
        try {
            return mapper().readValue(content, clazz);
        } catch (JacksonException e) {
            throw new DataGeneratorException("Failed to read template JSON", e);
        }
    }

    public static TemplateVO read(String content) {
        return read(content, TemplateVO.class);
    }

    private static String writeValue(Object value) {
        try {
            return mapper().writeValueAsString(value);
        } catch (JacksonException e) {
            throw new DataGeneratorException("Failed to write template JSON", e);
        }
    }

    private static ObjectMapper mapper() {
        long currentVersion = JsonSubtypeRegistry.version();
        ObjectMapper current = mapper;
        if (current == null || mapperVersion != currentVersion) {
            synchronized (TemplateJsonCodec.class) {
                current = mapper;
                if (current == null || mapperVersion != currentVersion) {
                    mapper = new ObjectMapper()
                            .rebuild()
                            .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
                            .findAndAddModules()
                            .registerSubtypes(JsonSubtypeRegistry.loadSubtypes(GeneratorVO.class))
                            .registerSubtypes(JsonSubtypeRegistry.loadSubtypes(IteratorVO.class))
                            .registerSubtypes(JsonSubtypeRegistry.loadSubtypes(ReaderVO.class))
                            .registerSubtypes(JsonSubtypeRegistry.loadSubtypes(ScriptVO.class))
                            .registerSubtypes(JsonSubtypeRegistry.loadSubtypes(ReaderSelectStrategyVO.class))
                            .registerSubtypes(JsonSubtypeRegistry.loadSubtypes(ValueSelectStrategyVO.class))
                            .registerSubtypes(JsonSubtypeRegistry.loadSubtypes(StageVO.class))
                            .registerSubtypes(JsonSubtypeRegistry.loadSubtypes(WriterVO.class))
                            .registerSubtypes(JsonSubtypeRegistry.loadSubtypes(SourceVO.class))
                            .registerSubtypes(JsonSubtypeRegistry.loadSubtypes(TransformVO.class))
                            .build();
                    mapperVersion = currentVersion;
                    current = mapper;
                }
            }
        }
        return current;
    }
}
