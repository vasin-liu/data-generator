/*
 * Copyright 漏 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address锛歅CI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou锛孋hina锛圸ip code锛?10653锛?
 */
package org.gensokyo.data.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.core.Version;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.jsontype.TypeResolverBuilder;
import tools.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.google.auto.service.AutoService;
import org.gensokyo.kit.collect.MapKit;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPI鏂瑰紡鍔犺浇鐨勫瓙绫诲瀷妯″潡
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/9 , Version 1.0.0
 */
@AutoService(JacksonModule.class)
public class SpiSubtypeModule extends JacksonModule {

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
            public TypeResolverBuilder<?> findTypeResolverBuilder(MapperConfig<?> config, Annotated ac) {
                JsonTypeInfo info = ac.getAnnotation(JsonTypeInfo.class);
                if (Objects.nonNull(info)) {
                    JavaType baseType = ac.getType();
                    Class<?> clazz = findBaseClass(ac.getAnnotation(JsonSubType.class), baseType);
                    registerTypes(clazz);
                    Class<?> defaultSubtype = defaultSubtypeRegistry.get(clazz);
                    JsonTypeInfo.Value settings = JsonTypeInfo.Value.construct(
                            JsonTypeInfo.Id.NAME,
                            JsonTypeInfo.As.EXISTING_PROPERTY,
                            info.property(),
                            defaultSubtype,
                            true,
                            null
                    );
                    return new StdTypeResolverBuilder()
                            .init(settings, new SpiSubTypeIdResolver(subtypeRegistry.get(clazz), defaultSubtype));
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

