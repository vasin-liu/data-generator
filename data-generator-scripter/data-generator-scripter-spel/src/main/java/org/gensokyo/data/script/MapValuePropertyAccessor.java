/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.script;

import org.gensokyo.data.value.MapValue;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.lang.NonNull;

/**
 * 映射属性访问器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/3/31 , Version 1.0.0
 */
public class MapValuePropertyAccessor implements PropertyAccessor {
    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class[]{MapValue.class};
    }

    @Override
    public boolean canRead(@NonNull EvaluationContext context, Object target, @NonNull String name) throws AccessException {
        return target instanceof MapValue;
    }

    @NonNull
    @Override
    public TypedValue read(@NonNull EvaluationContext context, Object target, @NonNull String name) throws AccessException {
        if (target instanceof MapValue mv) {
            return SpelUtils.extractMapValue(mv, name);
        }
        throw new AccessException("Map does not contain a value for key '" + name + "'");
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
