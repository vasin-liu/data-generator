/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Resolves writer {@code options} map entries for sink adapters and SQL builders.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class WriterOptionResolver {
    private static final PropertyPlaceholderHelper PLACEHOLDER_HELPER = new PropertyPlaceholderHelper("${", "}");

    private WriterOptionResolver() {
    }

    public static String stringOption(WriterVO writer, String name, Row row) {
        Object value = option(writer, name);
        if (value == null) {
            return null;
        }
        if (row == null) {
            return value.toString();
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

    /**
     * Resolves upsert key columns from {@code options.upsertKeys} (YAML array).
     * Falls back to legacy {@code options.conflictColumns} comma-string for PostgreSQL compatibility.
     *
     * @param writer JDBC or generic writer configuration
     * @return upsert key column names, possibly empty when unset
     */
    public static List<String> upsertKeysOption(WriterVO writer) {
        Object upsertKeys = option(writer, "upsertKeys");
        if (upsertKeys instanceof List<?> list) {
            List<String> keys = new ArrayList<>();
            for (Object item : list) {
                if (item != null && StringUtils.hasText(item.toString())) {
                    keys.add(item.toString().trim());
                }
            }
            if (!keys.isEmpty()) {
                return List.copyOf(keys);
            }
        }
        // Legacy PG conflictColumns comma-string — prefer upsertKeys for new templates.
        Object conflictColumns = option(writer, "conflictColumns");
        if (conflictColumns != null && StringUtils.hasText(conflictColumns.toString())) {
            List<String> keys = new ArrayList<>();
            for (String part : conflictColumns.toString().split(",")) {
                if (StringUtils.hasText(part.trim())) {
                    keys.add(part.trim());
                }
            }
            return List.copyOf(keys);
        }
        return List.of();
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
