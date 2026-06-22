/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sql;

import org.gensokyo.data.calcite.TemplateV2SqlFunctionContext;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Internal Calcite scalar functions for extracting values from JSON strings inside {@code sql} transforms.
 *
 * <p>These functions are registered under the reserved {@code V2_} prefix so they never collide with the
 * UDF {@code sqlName} namespace, and they are deliberately NOT exposed in the operator catalog (internal
 * only, D-11/D-12). The {@code json} transform operator parses in pure Java and is independent of these
 * functions.</p>
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
public final class TemplateV2JsonSqlFunctions {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TemplateV2JsonSqlFunctions() {
    }

    /**
     * Extracts the value at a simple dot path from a JSON string and returns it as VARCHAR.
     *
     * <p>Arguments: {@code (json, path)} where {@code path} is a dot-separated object path such as
     * {@code addr.city}. Returns SQL {@code NULL} when either argument is null, the JSON cannot be parsed,
     * or the path does not resolve to a value.</p>
     *
     * @param context SQL function arguments: json string, dot path
     * @return the resolved value as a string, or {@code null}
     */
    public static String jsonExtract(TemplateV2SqlFunctionContext context) {
        Object jsonArg = context.argument(0);
        Object pathArg = context.argument(1);
        if (jsonArg == null || pathArg == null) {
            return null;
        }
        String json = jsonArg.toString();
        String path = pathArg.toString();
        if (json.isBlank() || path.isBlank()) {
            return null;
        }
        JsonNode node;
        try {
            node = MAPPER.readTree(json);
        } catch (JacksonException e) {
            // Internal SQL scalar follows SQL NULL semantics rather than failing the run.
            return null;
        }
        for (String segment : path.split("\\.")) {
            if (node == null || node.isNull()) {
                return null;
            }
            node = node.get(segment);
        }
        if (node == null || node.isNull()) {
            return null;
        }
        return node.isString() ? node.asString() : node.toString();
    }
}
