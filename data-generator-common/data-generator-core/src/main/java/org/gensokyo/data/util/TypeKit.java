/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.util;

import org.gensokyo.kit.collect.ArrayKit;
import org.springframework.core.ResolvableType;

import java.util.Objects;
import java.util.stream.IntStream;

/**
 * 类型工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/9 , Version 1.0.0
 */
public class TypeKit {

    private TypeKit() {
        throw new UnsupportedOperationException();
    }

    public static <P, C> boolean isMatchingType(P parent, C child, Class<?>... types) {
        if (Objects.isNull(parent) || Objects.isNull(child) || ArrayKit.isEmpty(types)) {
            return false;
        }
        ResolvableType resolvableType = ResolvableType.forClass(getClass(child))
                .as(Objects.requireNonNull(getClass(parent)));

        return IntStream.range(0, types.length)
                .allMatch(i -> {
                    Class<?> genericType = resolvableType.getGeneric(i).resolve();
                    return Objects.nonNull(genericType) && genericType.equals(types[i]);
                });
    }

    private static <T> Class<?> getClass(T t) {
        if (Objects.isNull(t)) {
            return null;
        }
        Class<?> clazz;
        if (t instanceof Class<?> c) {
            clazz = c;
        } else {
            clazz = t.getClass();
        }
        return clazz;
    }
}
