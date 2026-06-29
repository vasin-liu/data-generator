/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.parser;

import org.gensokyo.data.calcite.*;

import org.gensokyo.data.model.v2.JsonSourceVO;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonToken;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Default {@link JsonParser} implementation using Jackson 3 {@link JsonNode} trees.
 *
 * @author Gensokyo
 * @since 3.0.0
 */
public class DefaultJsonParser implements JsonParser {
    private final ObjectMapper objectMapper;
    private final ObjectReader arrayElementReader;

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
        this.arrayElementReader = objectMapper.readerFor(JsonNode.class)
                .without(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
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

    /**
     * Parses a single NDJSON line into a row map.
     *
     * @param line       one line of NDJSON text (must not be blank)
     * @param lineNumber 1-based physical line number for actionable errors
     * @return row values for the JSON object on this line
     * @throws IllegalArgumentException when the line is blank, not a JSON object, or malformed
     */
    public Map<String, Object> parseNdjsonLine(String line, long lineNumber) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("NDJSON line [" + lineNumber + "] must not be blank");
        }
        try {
            JsonNode node = objectMapper.readTree(line);
            if (node == null || node.isNull()) {
                throw new IllegalArgumentException("NDJSON line [" + lineNumber + "] must not be JSON null");
            }
            if (!node.isObject()) {
                throw new IllegalArgumentException(
                        "NDJSON line [" + lineNumber + "] must be a JSON object, got " + node.getNodeType());
            }
            return toRow(node);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(
                    "Failed to parse NDJSON at line [" + lineNumber + "]: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * Opens a streaming iterator over elements of a top-level JSON array without loading the full document.
     *
     * @param reader UTF-8 (or caller-selected charset) reader positioned at the start of the file
     * @return closeable iterator yielding one row map per array element
     */
    public ArrayElementIterator openArrayElementIterator(Reader reader) {
        return new ArrayElementIterator(reader);
    }

    /**
     * Streaming iterator for top-level JSON array elements.
     */
    public final class ArrayElementIterator implements Iterator<Map<String, Object>>, AutoCloseable {

        private final tools.jackson.core.JsonParser parser;
        private final boolean parserCreated;
        private Map<String, Object> nextRow;
        private boolean finished;
        private long elementIndex;
        private boolean closed;

        private ArrayElementIterator(Reader reader) {
            try {
                TokenStreamFactory factory = objectMapper.tokenStreamFactory();
                this.parser = factory.createParser(reader);
                this.parserCreated = true;
                advanceToNextElement();
            } catch (JacksonException ex) {
                throw new IllegalArgumentException("Failed to open JSON array stream: " + ex.getOriginalMessage(), ex);
            }
        }

        @Override
        public boolean hasNext() {
            return nextRow != null;
        }

        @Override
        public Map<String, Object> next() {
            if (nextRow == null) {
                throw new NoSuchElementException("No more JSON array elements");
            }
            Map<String, Object> current = nextRow;
            advanceToNextElement();
            return current;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (parserCreated && parser != null) {
                try {
                    parser.close();
                } catch (JacksonException ex) {
                    throw new IllegalStateException("Failed to close JSON array parser", ex);
                }
            }
        }

        private void advanceToNextElement() {
            if (finished) {
                nextRow = null;
                return;
            }
            try {
                if (elementIndex == 0) {
                    JsonToken token = parser.nextToken();
                    if (token == null || token == JsonToken.END_ARRAY) {
                        finished = true;
                        nextRow = null;
                        return;
                    }
                    if (token != JsonToken.START_ARRAY) {
                        throw new IllegalArgumentException(
                                "JSON array source must start with '[' but found token [" + token + "]");
                    }
                }
                JsonToken token = parser.nextToken();
                if (token == null || token == JsonToken.END_ARRAY) {
                    finished = true;
                    nextRow = null;
                    return;
                }
                elementIndex++;
                JsonNode node = arrayElementReader.readValue(parser);
                nextRow = toRow(node);
            } catch (JacksonException ex) {
                throw new IllegalArgumentException(
                        "Failed to parse JSON array element at index [" + elementIndex + "]: "
                                + ex.getOriginalMessage(),
                        ex);
            }
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
