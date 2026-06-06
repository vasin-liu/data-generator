/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.json;

import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.v2.workflow.WorkflowStepVO;
import org.gensokyo.data.model.vo.generator.GeneratorVO;
import org.gensokyo.data.model.vo.iterator.IteratorVO;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.scripter.ScriptVO;
import org.gensokyo.data.model.vo.selector.reader.ReaderSelectStrategyVO;
import org.gensokyo.data.model.vo.selector.value.ValueSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import static tools.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

/**
 * Builds JSON {@link ObjectMapper} instances with Template V2 polymorphic subtype registration.
 *
 * @author Gensokyo
 * @since 2026-06-04
 */
public final class TemplateObjectMapperFactory {

    private TemplateObjectMapperFactory() {
    }

    /**
     * @return JSON mapper with template model subtypes registered from {@link JsonSubtypeRegistry}.
     */
    public static JsonMapper buildJsonMapper() {
        SimpleModule longAsString = new SimpleModule();
        longAsString.addSerializer(Long.class, ToStringSerializer.instance);
        longAsString.addSerializer(Long.TYPE, ToStringSerializer.instance);
        JsonMapper.Builder builder = JsonMapper.builder();
        registerTemplateSubtypes(builder);
        return builder
                .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
                .addModule(longAsString)
                .findAndAddModules()
                .build();
    }

    /**
     * Registers the same polymorphic subtypes used for template YAML on a mapper builder.
     *
     * @param builder mapper builder (JSON or YAML factory)
     */
    public static void registerTemplateSubtypes(MapperBuilder<?, ?> builder) {
        builder
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
                .registerSubtypes(JsonSubtypeRegistry.loadSubtypes(WorkflowStepVO.class));
    }
}
