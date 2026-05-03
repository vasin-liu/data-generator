package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.JsonSourceVO;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultJsonParser implements JsonParser {
    private final ObjectMapper objectMapper;

    public DefaultJsonParser() {
        this(new ObjectMapper());
    }

    public DefaultJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Map<String, Object>> parse(JsonSourceVO source, String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            if (root == null || root.isNull()) {
                return List.of();
            }
            if (root.isArray()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (JsonNode element : root) {
                    rows.add(toRow(element));
                }
                return rows;
            }
            return List.of(toRow(root));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to parse JSON source: " + source.getPath(), e);
        }
    }

    private Map<String, Object> toRow(JsonNode node) {
        if (!node.isObject()) {
            return Map.of("value", toValue(node));
        }
        Map<String, Object> row = new LinkedHashMap<>();
        node.properties().forEach(entry -> row.put(entry.getKey(), toValue(entry.getValue())));
        return row;
    }

    private Object toValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isInt() || node.isLong()) {
            return node.asLong();
        }
        if (node.isFloat() || node.isDouble() || node.isBigDecimal() || node.isNumber()) {
            return node.asDouble();
        }
        return node.toString();
    }
}
