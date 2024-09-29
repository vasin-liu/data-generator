/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.google.auto.service.AutoService;
import org.gensokyo.kit.collect.MapKit;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPI方式加载的子类型模块
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/9 , Version 1.0.0
 */
@AutoService(Module.class)
public class SpiSubtypeModule extends Module {

    private final Map<Class<?>, Map<String, Class<?>>> subtypeRegistry = new ConcurrentHashMap<>();
    private final Map<Class<?>, Class<?>> defaultSubtypeRegistry = new ConcurrentHashMap<>();

    @Override
    public String getModuleName() {
        return ModuleVersion.VERSION.getArtifactId();
    }

    @Override
    public Version version() {
        return ModuleVersion.VERSION;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.insertAnnotationIntrospector(new AnnotationIntrospector() {
            @Override
            public Version version() {
                return ModuleVersion.VERSION;
            }

            @Override
            public TypeResolverBuilder<?> findTypeResolver(MapperConfig<?> config, AnnotatedClass ac, JavaType baseType) {
                JsonTypeInfo info = ac.getAnnotation(JsonTypeInfo.class);
                if (Objects.nonNull(info)) {
                    Class<?> clazz = findBaseClass(ac.getAnnotation(JsonSubType.class), baseType);
                    registerTypes(clazz);
                    Class<?> defaultSubtype = defaultSubtypeRegistry.get(clazz);
                    return new StdTypeResolverBuilder()
                            .init(JsonTypeInfo.Id.NAME, new SpiSubTypeIdResolver(subtypeRegistry.get(clazz), defaultSubtype))
                            .inclusion(JsonTypeInfo.As.EXISTING_PROPERTY)
                            .typeProperty(info.property())
                            .defaultImpl(defaultSubtype)
                            .typeIdVisibility(true);
                }
                return null;
            }
        });
    }

    private Class<?> findBaseClass(JsonSubType annotation, JavaType baseType) {
        if (Objects.isNull(annotation)) {
            return baseType.getRawClass();
        } else {
            JavaType superClass = baseType.getSuperClass();
            if (Objects.isNull(superClass)) {
                return null;
            }
            JsonSubType subType = superClass.getRawClass().getAnnotation(JsonSubType.class);
            return findBaseClass(subType, superClass);
        }
    }

    public <S> void registerTypes(Class<S> parent) {
        if (Objects.isNull(parent) || subtypeRegistry.containsKey(parent)) {
            return;
        }
        var subtypes = new ConcurrentHashMap<String, Class<?>>();
        for (S instance : java.util.ServiceLoader.load(parent)) {
            Class<?> child = instance.getClass();
            JsonSubType jsonSubType = AnnotationUtils.findAnnotation(child, JsonSubType.class);
            if (Objects.nonNull(jsonSubType)) {
                subtypes.put(jsonSubType.value().toUpperCase(), child);
                if (jsonSubType.isDefault()) {
                    defaultSubtypeRegistry.put(parent, child);
                }
            }
        }
        if (MapKit.isNotEmpty(subtypes)) {
            subtypeRegistry.put(parent, subtypes);
        }
    }
}
