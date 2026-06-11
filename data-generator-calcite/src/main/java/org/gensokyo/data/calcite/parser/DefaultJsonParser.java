/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.parser;

import org.gensokyo.data.calcite.*;

import org.gensokyo.data.model.v2.JsonSourceVO;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default {@link JsonParser} implementation using Jackson 3 {@link JsonNode} trees.
 *
 * @author Gensokyo
 * @since 3.0.0
 */
public class DefaultJsonParser implements JsonParser {
    private final ObjectMapper objectMapper;

    /**
     * Creates a parser backed by a default {@link ObjectMapper}.
     */
    public DefaultJsonParser() {
        this(new ObjectMapper());
    }

    /**
     * Creates a parser backed by the given {@link ObjectMapper}.
     *
     * @param objectMapper mapper used to decode JSON content into nodes
     */
    public DefaultJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses JSON content into rows of plain Java values.
     *
     * @param source  JSON source metadata including optional dotted root selector
     * @param content raw JSON document text
     * @return one map per JSON object at the resolved root (or list elements under an array root)
     * @throws IllegalArgumentException if JSON parsing fails or the root selector cannot be resolved
     */
    @Override
    public List<Map<String, Object>> parse(JsonSourceVO source, String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            if (root == null || root.isNull()) {
                return List.of();
            }
            root = selectRoot(source, root);
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

    private JsonNode selectRoot(JsonSourceVO source, JsonNode root) {
        String selector = source.getRoot();
        if (selector == null || selector.isBlank()) {
            return root;
        }
        JsonNode current = root;
        for (String segment : selector.split("\\.")) {
            if (segment.isBlank()) {
                throw new IllegalArgumentException("JSON source root selector must not contain blank path segments: "
                        + selector);
            }
            current = selectSegment(current, segment, selector);
            if (current == null) {
                throw new IllegalArgumentException("JSON source root selector [" + selector
                        + "] did not match segment [" + segment + "]: " + source.getPath());
            }
            if (current.isNull()) {
                return current;
            }
        }
        return current;
    }

    private JsonNode selectSegment(JsonNode node, String segment, String selector) {
        int bracketIndex = segment.indexOf('[');
        if (bracketIndex < 0) {
            return node.get(segment);
        }
        String fieldName = segment.substring(0, bracketIndex);
        JsonNode current = fieldName.isBlank() ? node : node.get(fieldName);
        int index = bracketIndex;
        while (index < segment.length()) {
            if (segment.charAt(index) != '[') {
                throw new IllegalArgumentException("Unsupported JSON source root selector segment [" + segment
                        + "] in selector [" + selector + "]");
            }
            int close = segment.indexOf(']', index);
            if (close < 0) {
                throw new IllegalArgumentException("Unclosed array index in JSON source root selector [" + selector + "]");
            }
            int arrayIndex = parseArrayIndex(segment.substring(index + 1, close), selector);
            current = current == null ? null : current.get(arrayIndex);
            index = close + 1;
        }
        return current;
    }

    private int parseArrayIndex(String value, String selector) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid array index in JSON source root selector [" + selector + "]", e);
        }
    }

    private Map<String, Object> toRow(JsonNode node) {
        if (!node.isObject()) {
            return Map.of("value", toValue(node));
        }
        Map<String, Object> row = new LinkedHashMap<>();
        // Jackson 3: prefer forEachEntry over deprecated properties() iteration on JsonNode.
        node.forEachEntry((key, value) -> row.put(key, toValue(value)));
        return row;
    }

    private Object toValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isString()) {
            return node.asString();
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
