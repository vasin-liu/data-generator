/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

import java.util.Map;

/**
 * 自定义属性子类型解析器
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
    public void init(JavaType baseType) {
        // No-op
    }

    @Override
    public String idFromValue(Object value) {
        return value.getClass().getSimpleName().toUpperCase();
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return idFromValue(value);
    }

    @Override
    public String idFromBaseType() {
        return null;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
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