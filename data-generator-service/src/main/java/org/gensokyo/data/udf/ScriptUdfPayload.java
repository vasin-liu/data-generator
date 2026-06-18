/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Parsed JSON payload shared by SQL ({@link UdfType#SQL}) and script ({@link UdfType#SCRIPT}) UDFs.
 *
 * <p>Both UDF kinds reduce to a SQL-callable GraalJS function: a {@code sqlName}, an argument
 * count, a return type, and a callable script body. Script UDFs additionally carry input/output
 * JSON Schemas validated at publish time (D-12).
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
public record ScriptUdfPayload(String sqlName,
                               int argCount,
                               String returnType,
                               String script,
                               boolean hasInputSchema,
                               boolean hasOutputSchema) {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final List<String> SUPPORTED_RETURN_TYPES =
            List.of("VARCHAR", "INTEGER", "BIGINT", "BOOLEAN", "DOUBLE");

    /**
     * Parses an inline JSON payload for a SQL or script UDF.
     *
     * @param payload UTF-8 JSON bytes
     * @return parsed payload view
     * @throws UdfRegistryException when JSON is malformed or required fields are missing/invalid
     */
    public static ScriptUdfPayload parse(byte[] payload) {
        JsonNode root = readTree(payload);
        String sqlName = text(root, "sqlName");
        if (sqlName == null || sqlName.isBlank()) {
            throw invalid("UDF_PAYLOAD_INVALID", "sqlName", "sqlName is required");
        }
        String script = text(root, "script");
        if (script == null || script.isBlank()) {
            throw invalid("UDF_PAYLOAD_INVALID", "script", "script is required");
        }
        String returnType = text(root, "returnType");
        if (returnType == null || !SUPPORTED_RETURN_TYPES.contains(returnType.toUpperCase())) {
            throw invalid("UDF_PAYLOAD_INVALID", "returnType",
                    "returnType must be one of " + SUPPORTED_RETURN_TYPES);
        }
        int argCount = root.has("argCount") ? root.get("argCount").asInt() : 1;
        if (argCount < 0 || argCount > 2) {
            throw invalid("UDF_PAYLOAD_INVALID", "argCount", "argCount must be between 0 and 2");
        }
        boolean hasInputSchema = isSchemaObject(root.get("inputSchema"));
        boolean hasOutputSchema = isSchemaObject(root.get("outputSchema"));
        return new ScriptUdfPayload(sqlName.trim(), argCount, returnType.toUpperCase(), script,
                hasInputSchema, hasOutputSchema);
    }

    private static JsonNode readTree(byte[] payload) {
        if (payload == null || payload.length == 0) {
            throw invalid("UDF_PAYLOAD_INVALID", "payload", "payload must not be empty");
        }
        try {
            return MAPPER.readTree(new String(payload, StandardCharsets.UTF_8));
        } catch (JacksonException e) {
            throw invalid("UDF_PAYLOAD_INVALID", "payload", "payload is not valid JSON: " + e.getMessage());
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node == null || node.isNull() ? null : node.asString();
    }

    private static boolean isSchemaObject(JsonNode node) {
        return node != null && node.isObject() && !node.isEmpty();
    }

    private static UdfRegistryException invalid(String code, String field, String message) {
        return new UdfRegistryException(code, message, List.of(new UdfValidationError(code, field, message)));
    }
}
