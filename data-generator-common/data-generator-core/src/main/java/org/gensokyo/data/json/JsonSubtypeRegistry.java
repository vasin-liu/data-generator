package org.gensokyo.data.json;

import tools.jackson.databind.jsontype.NamedType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class JsonSubtypeRegistry {
    private static final Map<Class<?>, Map<Class<?>, NamedType[]>> MANUAL_SUBTYPES = new ConcurrentHashMap<>();
    private static final AtomicLong VERSION = new AtomicLong();

    private JsonSubtypeRegistry() {
    }

    public static long version() {
        return VERSION.get();
    }

    public static <T> NamedType[] loadSubtypes(Class<T> parent) {
        LinkedHashMap<String, NamedType> resolved = new LinkedHashMap<>();
        for (T instance : ServiceLoader.load(parent)) {
            addNamedTypes(parent, instance.getClass(), resolved);
        }
        MANUAL_SUBTYPES.getOrDefault(parent, Map.of())
                .values()
                .forEach(namedTypes -> {
                    for (NamedType namedType : namedTypes) {
                        mergeNamedType(parent, namedType, resolved);
                    }
                });
        return resolved.values().toArray(NamedType[]::new);
    }

    public static <T> void registerSubtype(Class<T> parent, Class<? extends T> subtype) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(subtype, "subtype");
        MANUAL_SUBTYPES
                .computeIfAbsent(parent, ignored -> new ConcurrentHashMap<>())
                .put(subtype, namedTypes(subtype));
        VERSION.incrementAndGet();
    }

    public static <T> void registerSubtypes(Class<T> parent, ClassLoader classLoader) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(classLoader, "classLoader");
        for (T instance : ServiceLoader.load(parent, classLoader)) {
            @SuppressWarnings("unchecked")
            Class<? extends T> subtype = (Class<? extends T>) instance.getClass();
            registerSubtype(parent, subtype);
        }
    }

    private static NamedType[] namedTypes(Class<?> subtype) {
        JsonSubType annotation = subtype.getAnnotation(JsonSubType.class);
        if (annotation == null || blank(annotation.value())) {
            return new NamedType[]{new NamedType(subtype)};
        }
        String typeId = annotation.value();
        return new NamedType[]{
                new NamedType(subtype, typeId),
                new NamedType(subtype, typeId.toLowerCase())
        };
    }

    private static void addNamedTypes(Class<?> parent, Class<?> subtype, Map<String, NamedType> resolved) {
        for (NamedType namedType : namedTypes(subtype)) {
            mergeNamedType(parent, namedType, resolved);
        }
    }

    private static void mergeNamedType(Class<?> parent, NamedType candidate, Map<String, NamedType> resolved) {
        String key = candidate.hasName() ? candidate.getName() : candidate.getType().getName();
        NamedType existing = resolved.putIfAbsent(key, candidate);
        if (existing != null && !existing.getType().equals(candidate.getType()) && prefer(candidate, existing)) {
            resolved.put(key, candidate);
        }
    }

    private static boolean prefer(NamedType candidate, NamedType existing) {
        String candidateName = candidate.getType().getName();
        String existingName = existing.getType().getName();
        if (candidateName.startsWith("org.gensokyo.data.model.") && !existingName.startsWith("org.gensokyo.data.model.")) {
            return true;
        }
        if (!candidateName.startsWith("org.gensokyo.data.model.") && existingName.startsWith("org.gensokyo.data.model.")) {
            return false;
        }
        return candidateName.compareTo(existingName) < 0;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
