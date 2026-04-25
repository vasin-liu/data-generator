/*
 * Copyright 漏 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address锛歅CI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou锛孋hina锛圸ip code锛?10653锛?
 */
package org.gensokyo.data.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.jsontype.TypeIdResolver;

import java.util.Map;

/**
 * 鑷畾涔夊睘鎬у瓙绫诲瀷瑙ｆ瀽鍣?
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/1 , Version 1.0.0
 */
public class SpiSubTypeIdResolver implements TypeIdResolver {
    private final Map<String, Class<?>> subtypeRegistry;
    private final Class<?> defaultSubtype;

    SpiSubTypeIdResolver(Map<String, Class<?>> subtypeRegistry, Class<?> defaultSubtype) {
        this.subtypeRegistry = subtypeRegistry;
        this.defaultSubtype = defaultSubtype;
    }

    @Override
    public void init(JavaType baseType) throws JacksonException {
        // No-op
    }

    @Override
    public String idFromValue(DatabindContext context, Object value) throws JacksonException {
        return value.getClass().getSimpleName().toUpperCase();
    }

    @Override
    public String idFromValueAndType(DatabindContext context, Object value, Class<?> suggestedType) throws JacksonException {
        return idFromValue(context, value);
    }

    @Override
    public String idFromBaseType(DatabindContext context) throws JacksonException {
        return null;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws JacksonException {
        Class<?> subtype = subtypeRegistry.getOrDefault(id.toUpperCase(), defaultSubtype);
        if (subtype == null) {
            throw new IllegalArgumentException("Unknown subtype: " + id);
        }
        return context.getTypeFactory().constructType(subtype);
    }

    @Override
    public String getDescForKnownTypeIds() {
        return "Subtypes registered via @JsonSubType";
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.NAME;
    }
}
