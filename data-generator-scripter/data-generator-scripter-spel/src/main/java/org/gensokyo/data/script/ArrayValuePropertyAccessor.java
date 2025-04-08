/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.script;

import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.lang.NonNull;

import java.util.Objects;

/**
 * 数组属性访问器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/3/31 , Version 1.0.0
 */
public class ArrayValuePropertyAccessor implements PropertyAccessor {
    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class[]{Value[].class};
    }

    @Override
    public boolean canRead(@NonNull EvaluationContext context, Object target, @NonNull String name) throws AccessException {
        return Objects.nonNull(target)
                && target.getClass().isArray()
                && Value.class.isAssignableFrom(target.getClass().getComponentType());
    }

    @NonNull
    @Override
    public TypedValue read(@NonNull EvaluationContext context, Object target, @NonNull String name) throws AccessException {
        if (target instanceof Value[] values) {
            if (SpelUtils.isInteger(name)) {
                int index = Integer.parseInt(name);
                if (index >= 0 && index < values.length) {
                    Object unwrapped = values[index];
                    return read(unwrapped, name);
                }
            } else {
                if (values.length == 1) {
                    Object unwrapped = values[0];
                    return read(unwrapped, name);
                }
            }
        }
        throw new AccessException("Invalid array access: " + name);
    }

    private TypedValue read(Object unwrapped, String name) throws AccessException {
        if (unwrapped instanceof MapValue mv) {
            return SpelUtils.extractMapValue(mv, name);
        }

        if (unwrapped instanceof ListValue lv) {
            return SpelUtils.extractListValue(lv, name);
        }

        if (unwrapped instanceof SingleValue sv) {
            return new TypedValue(sv.get());
        }

        return new TypedValue(unwrapped);
    }

    @Override
    public boolean canWrite(@NonNull EvaluationContext context, Object target, @NonNull String name) throws AccessException {
        // 这里不支持写入
        return false;
    }

    @Override
    public void write(@NonNull EvaluationContext context, Object target, @NonNull String name, Object newValue) throws AccessException {
        throw new UnsupportedOperationException("Write not supported");
    }
}
