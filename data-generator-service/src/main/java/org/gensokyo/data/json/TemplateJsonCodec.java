package org.gensokyo.data.json;

import org.gensokyo.data.exception.DataGeneratorException;
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
import org.gensokyo.kit.character.StrKit;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.NamedType;

import java.util.ArrayList;
import java.util.ServiceLoader;

import static tools.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

public final class TemplateJsonCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper()
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
            return MAPPER.readValue(content, clazz);
        } catch (JacksonException e) {
            throw new DataGeneratorException("Failed to read template JSON", e);
        }
    }

    public static TemplateVO read(String content) {
        return read(content, TemplateVO.class);
    }

    private static String writeValue(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new DataGeneratorException("Failed to write template JSON", e);
        }
    }

    private static <T> NamedType[] loadSubtypes(Class<T> parent) {
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
}
