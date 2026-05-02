package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class WriterOptionResolver {
    private static final PropertyPlaceholderHelper PLACEHOLDER_HELPER = new PropertyPlaceholderHelper("${", "}");

    private WriterOptionResolver() {
    }

    public static String stringOption(WriterVO writer, String name, Row row) {
        Object value = option(writer, name);
        if (value == null) {
            return null;
        }
        return resolve(value.toString(), row);
    }

    public static boolean booleanOption(WriterVO writer, String name) {
        Object value = option(writer, name);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    public static Map<String, String> stringMapOption(WriterVO writer, String name, Row row) {
        Object value = option(writer, name);
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, String> resolved = new LinkedHashMap<>();
        map.forEach((key, item) -> resolved.put(String.valueOf(key), resolve(item == null ? "" : item.toString(), row)));
        return resolved;
    }

    public static String resolve(String template, Row row) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        Properties properties = new Properties();
        row.values().forEach((key, value) -> properties.put(key, value == null ? "" : value.toString()));
        return PLACEHOLDER_HELPER.replacePlaceholders(template, properties);
    }

    private static Object option(WriterVO writer, String name) {
        if (writer.getOptions() == null) {
            return null;
        }
        return writer.getOptions().get(name);
    }
}
