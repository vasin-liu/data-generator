package org.gensokyo.data.model.v2;

import java.io.Serializable;
import java.util.Map;

public record Row(Map<String, Object> values) implements Serializable {
    public Object get(String name) {
        return values.get(name);
    }

    public String getString(String name) {
        Object value = get(name);
        return value == null ? null : value.toString();
    }
}
