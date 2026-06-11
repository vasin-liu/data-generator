/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.json;

import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.TemplateVO;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public final class TemplateJsonCodec {
    private static volatile ObjectMapper mapper;
    private static volatile long mapperVersion = Long.MIN_VALUE;

    private TemplateJsonCodec() {
    }

    /**
     * @return version-aware JSON mapper with template polymorphic subtypes registered.
     */
    public static ObjectMapper mapper() {
        long currentVersion = JsonSubtypeRegistry.version();
        ObjectMapper current = mapper;
        if (current == null || mapperVersion != currentVersion) {
            synchronized (TemplateJsonCodec.class) {
                current = mapper;
                if (current == null || mapperVersion != currentVersion) {
                    mapper = TemplateObjectMapperFactory.buildJsonMapper();
                    mapperVersion = currentVersion;
                    current = mapper;
                }
            }
        }
        return current;
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
}
