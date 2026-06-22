/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import org.gensokyo.data.api.console.dto.TransformCatalogEntryView;
import org.gensokyo.data.api.console.dto.TransformCatalogParam;

import java.util.List;

/**
 * Authored descriptors for the built-in Template V2 transform operators (D-06, D-07).
 *
 * <p>Each entry carries the operator type, a description, its parameter schema, and a short YAML usage
 * example so an operator can author the transform directly from the catalog. Built-in operators have no
 * {@code sqlName}. The internal {@code V2_JSON_EXTRACT} scalar function is intentionally NOT listed —
 * catalog granularity stays at operator/UDF level (D-12).</p>
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
public final class BuiltinTransformCatalog {

    /** Source discriminator value for built-in operators (D-06). */
    public static final String KIND_BUILTIN = "BUILTIN";

    private BuiltinTransformCatalog() {
    }

    /**
     * Returns the authored descriptors for every built-in transform operator.
     *
     * @return immutable list of built-in catalog entries (json/mask/lookup/sql/spel/js)
     */
    public static List<TransformCatalogEntryView> entries() {
        return List.of(json(), mask(), lookup(), sql(), spel(), js());
    }

    private static TransformCatalogEntryView json() {
        List<TransformCatalogParam> params = List.of(
                new TransformCatalogParam("sourceColumn", "string", true,
                        "Input column whose string value is parsed as JSON on each row"),
                new TransformCatalogParam("targetColumn", "string", false,
                        "Column holding the parsed object when flatten is false"),
                new TransformCatalogParam("flatten", "boolean", false,
                        "When true, nested keys are flattened into separate columns"),
                new TransformCatalogParam("separator", "string", false,
                        "Separator used to compose flattened column names (default '.')"));
        String example = """
                transform:
                  - type: json
                    sourceColumn: payload
                    flatten: true
                    separator: "."
                """;
        return new TransformCatalogEntryView("json", KIND_BUILTIN,
                "Parse a JSON string column into an object, optionally flattening nested keys", params, example, null);
    }

    private static TransformCatalogEntryView mask() {
        List<TransformCatalogParam> params = List.of(
                new TransformCatalogParam("rules", "list", true,
                        "List of {column, strategy}; strategy is one of email/phone/credit-card/generic-fixed"));
        String example = """
                transform:
                  - type: mask
                    rules:
                      - column: email
                        strategy: email
                """;
        return new TransformCatalogEntryView("mask", KIND_BUILTIN,
                "Redact column values in place using named strategies", params, example, null);
    }

    private static TransformCatalogEntryView lookup() {
        List<TransformCatalogParam> params = List.of(
                new TransformCatalogParam("source", "string", true,
                        "Name of the in-template source to join against"),
                new TransformCatalogParam("leftKey", "string", true, "Input-row column used as the join key"),
                new TransformCatalogParam("rightKey", "string", true,
                        "Lookup-source column matched against leftKey"),
                new TransformCatalogParam("columns", "list", true,
                        "Lookup-source columns projected onto the enriched output row"));
        String example = """
                transform:
                  - type: lookup
                    source: departments
                    leftKey: dept_id
                    rightKey: id
                    columns: [name]
                """;
        return new TransformCatalogEntryView("lookup", KIND_BUILTIN,
                "Enrich rows by joining an in-template named source on a key", params, example, null);
    }

    private static TransformCatalogEntryView sql() {
        List<TransformCatalogParam> params = List.of(
                new TransformCatalogParam("sql", "string", true,
                        "Calcite SQL statement applied over table 'input'"));
        String example = """
                transform:
                  - type: sql
                    sql: SELECT id, UPPER(name) AS name FROM input
                """;
        return new TransformCatalogEntryView("sql", KIND_BUILTIN,
                "Transform rows with a Calcite SQL statement", params, example, null);
    }

    private static TransformCatalogEntryView spel() {
        List<TransformCatalogParam> params = List.of(
                new TransformCatalogParam("columns", "list", true,
                        "List of {name, expression}; each SpEL expression computes one output column"));
        String example = """
                transform:
                  - type: spel
                    columns:
                      - name: label
                        expression: "#row['id'] + '-1'"
                """;
        return new TransformCatalogEntryView("spel", KIND_BUILTIN,
                "Compute columns with SpEL expressions evaluated per row", params, example, null);
    }

    private static TransformCatalogEntryView js() {
        List<TransformCatalogParam> params = List.of(
                new TransformCatalogParam("script", "string", true,
                        "GraalJS script that mutates the bound 'row' object per row"),
                new TransformCatalogParam("timeoutMs", "integer", false,
                        "Per-row script execution timeout in milliseconds"));
        String example = """
                transform:
                  - type: js
                    script: "row.amount = row.amount * 2"
                """;
        return new TransformCatalogEntryView("js", KIND_BUILTIN,
                "Transform each row with a sandboxed GraalJS script", params, example, null);
    }
}
