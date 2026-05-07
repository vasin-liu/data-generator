package org.gensokyo.data.calcite.codec;

import org.gensokyo.data.calcite.*;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RowJsonCodec {
    private RowJsonCodec() {
    }

    public static String toJsonObject(Map<String, Object> values) {
        Map<String, Object> ordered = new LinkedHashMap<>(values);
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : ordered.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(escape(entry.getKey())).append('"').append(':');
            appendJsonValue(builder, entry.getValue());
            first = false;
        }
        return builder.append('}').toString();
    }

    public static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void appendJsonValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else {
            builder.append('"').append(escape(value.toString())).append('"');
        }
    }
}
