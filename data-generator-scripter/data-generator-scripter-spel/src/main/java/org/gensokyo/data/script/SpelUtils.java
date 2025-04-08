/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.script;

import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.Value;
import org.springframework.expression.AccessException;
import org.springframework.expression.TypedValue;

/**
 * SPEL工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/2 , Version 1.0.0
 */
public class SpelUtils {

    private SpelUtils() {
        throw new UnsupportedOperationException();
    }

    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static TypedValue extractMapValue(MapValue mv, String name) throws AccessException {
        if (mv.size() == 1) {
            return new TypedValue(mv.values()
                    .stream()
                    .map(val -> {
                        if (val instanceof MapValue v && mv.containsKey(name)) {
                            return v.get(name).get();
                        }
                        return val.get();
                    })
                    .findFirst()
                    .orElseThrow());
        }
        if (mv.containsKey(name)) {
            return new TypedValue(mv.get(name).get());

        }
        throw new AccessException("Map does not contain a value for key '" + name + "'");
    }

    public static TypedValue extractListValue(ListValue lv, String name) throws AccessException {
        if (lv.size() == 1) {
            Value val = lv.get(0);
            if (val instanceof MapValue mv && mv.containsKey(name)) {
                return new TypedValue(mv.get(name));
            }
            return new TypedValue(val.get());
        }

        if (SpelUtils.isInteger(name)) {
            int index = Integer.parseInt(name);
            if (index >= 0 && index < lv.size()) {
                return new TypedValue(lv.get(index).get());
            }
        }
        throw new AccessException("List does not contain a value for index '" + name + "'");
    }
}
