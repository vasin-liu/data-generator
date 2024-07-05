/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import org.gensokyo.kit.Assert;
import org.gensokyo.kit.collect.CollectKit;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 模型参数工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/27 , Version 1.0.0
 */
public final class ModelOptionsUtils {

    public final static ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .registerModule(new JavaTimeModule());

    private final static List<String> BEAN_MERGE_FIELD_EXCISIONS = List.of("class");

    private static ConcurrentHashMap<Class<?>, List<String>> REQUEST_FIELD_NAMES_PER_CLASS = new ConcurrentHashMap<Class<?>, List<String>>();

    private static AtomicReference<SchemaGenerator> SCHEMA_GENERATOR_CACHE = new AtomicReference<>();

    private ModelOptionsUtils() {

    }

    public static Map<String, Object> jsonToMap(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, MAP_TYPE_REF);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<>() {
    };

    public static <T> T jsonToObject(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to json: " + json, e);
        }
    }

    public static String toJsonString(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T merge(Object source, Object target, Class<T> clazz, List<String> acceptedFieldNames) {

        if (source == null) {
            source = Map.of();
        }

        List<String> requestFieldNames = CollectKit.isEmpty(acceptedFieldNames)
                ? REQUEST_FIELD_NAMES_PER_CLASS.computeIfAbsent(clazz, ModelOptionsUtils::getJsonPropertyValues)
                : acceptedFieldNames;

        if (CollectKit.isEmpty(requestFieldNames)) {
            throw new IllegalArgumentException("No @JsonProperty fields found in the " + clazz.getName());
        }

        Map<String, Object> sourceMap = ModelOptionsUtils.objectToMap(source);
        Map<String, Object> targetMap = ModelOptionsUtils.objectToMap(target);

        targetMap.putAll(sourceMap.entrySet()
                .stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));

        targetMap = targetMap.entrySet()
                .stream()
                .filter(e -> requestFieldNames.contains(e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        return ModelOptionsUtils.mapToClass(targetMap, clazz);
    }

    public static <T> T merge(Object source, Object target, Class<T> clazz) {
        return ModelOptionsUtils.merge(source, target, clazz, null);
    }

    public static Map<String, Object> objectToMap(Object source) {
        if (source == null) {
            return new HashMap<>();
        }
        try {
            String json = OBJECT_MAPPER.writeValueAsString(source);
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
                    })
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T mapToClass(Map<String, Object> source, Class<T> clazz) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(source);
            return OBJECT_MAPPER.readValue(json, clazz);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getJsonPropertyValues(Class<?> clazz) {
        List<String> values = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            JsonProperty jsonPropertyAnnotation = field.getAnnotation(JsonProperty.class);
            if (jsonPropertyAnnotation != null) {
                values.add(jsonPropertyAnnotation.value());
            }
        }
        return values;
    }

    public static <I, S extends I, T extends S> T copyToTarget(S sourceBean, Class<I> sourceInterfaceClazz,
                                                               Class<T> targetBeanClazz) {

        Assert.notNull(sourceInterfaceClazz, "SourceOptionsClazz must not be null");
        Assert.notNull(targetBeanClazz, "TargetOptionsClazz must not be null");

        if (sourceBean == null) {
            return null;
        }

        if (sourceBean.getClass().isAssignableFrom(targetBeanClazz)) {
            return (T) sourceBean;
        }

        try {
            T targetOptions = targetBeanClazz.getConstructor().newInstance();

            ModelOptionsUtils.mergeBeans(sourceBean, targetOptions, sourceInterfaceClazz, true);

            return targetOptions;
        }
        catch (Exception e) {
            throw new RuntimeException(
                    "Failed to convert the " + sourceInterfaceClazz.getName() + " into " + targetBeanClazz.getName(),
                    e);
        }
    }

    public static <I, S extends I, T extends S> T mergeBeans(S source, T target, Class<I> sourceInterfaceClazz,
                                                             boolean overrideNonNullTargetValues) {
        Assert.notNull(source, "Source object must not be null");
        Assert.notNull(target, "Target object must not be null");

        BeanWrapper sourceBeanWrap = new BeanWrapperImpl(source);
        BeanWrapper targetBeanWrap = new BeanWrapperImpl(target);

        List<String> interfaceNames = Arrays.stream(sourceInterfaceClazz.getMethods()).map(Method::getName).toList();

        for (PropertyDescriptor descriptor : sourceBeanWrap.getPropertyDescriptors()) {

            if (!BEAN_MERGE_FIELD_EXCISIONS.contains(descriptor.getName())
                    && interfaceNames.contains(toGetName(descriptor.getName()))) {

                String propertyName = descriptor.getName();
                Object value = sourceBeanWrap.getPropertyValue(propertyName);

                // Copy value to the target object
                if (value != null) {
                    var targetValue = targetBeanWrap.getPropertyValue(propertyName);

                    if (targetValue == null || overrideNonNullTargetValues) {
                        targetBeanWrap.setPropertyValue(propertyName, value);
                    }
                }
            }
        }

        return target;
    }

    private static String toGetName(String name) {
        return "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static String getJsonSchema(Class<?> clazz, boolean toUpperCaseTypeValues) {

        if (SCHEMA_GENERATOR_CACHE.get() == null) {
            JacksonModule jacksonModule = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED);
            //Swagger2Module swaggerModule = new Swagger2Module();

            SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12,
                    OptionPreset.PLAIN_JSON)
                    .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                    .with(Option.PLAIN_DEFINITION_KEYS)
                    //.with(swaggerModule)
                    .with(jacksonModule);

            SchemaGeneratorConfig config = configBuilder.build();
            SchemaGenerator generator = new SchemaGenerator(config);
            SCHEMA_GENERATOR_CACHE.compareAndSet(null, generator);
        }

        ObjectNode node = SCHEMA_GENERATOR_CACHE.get().generateSchema(clazz);
        if (toUpperCaseTypeValues) { // Required for OpenAPI 3.0 (at least Vertex AI
            // version of it).
            toUpperCaseTypeValues(node);
        }

        return node.toPrettyString();
    }

    public static void toUpperCaseTypeValues(ObjectNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isObject()) {
                    toUpperCaseTypeValues((ObjectNode) value);
                }
                else if (value.isArray()) {
                    value.elements().forEachRemaining(element -> {
                        if (element.isObject() || element.isArray()) {
                            toUpperCaseTypeValues((ObjectNode) element);
                        }
                    });
                }
                else if (value.isTextual() && entry.getKey().equals("type")) {
                    String oldValue = node.get("type").asText();
                    node.put("type", oldValue.toUpperCase());
                }
            });
        }
        else if (node.isArray()) {
            node.elements().forEachRemaining(element -> {
                if (element.isObject() || element.isArray()) {
                    toUpperCaseTypeValues((ObjectNode) element);
                }
            });
        }
    }

}
