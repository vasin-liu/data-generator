/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import java.util.Locale;

/**
 * Supported UDF artifact kinds in the unified registry.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
public enum UdfType {

    /** PF4J JAR plugin artifact ({@code java-plugin}). */
    JAVA_PLUGIN("java-plugin"),

    /** GraalJS callable function ({@code script}). */
    SCRIPT("script"),

    /** Calcite SQL function definition ({@code sql}). */
    SQL("sql");

    private final String jsonName;

    UdfType(String jsonName) {
        this.jsonName = jsonName;
    }

    /**
     * @return stable external type name used in roadmap and APIs
     */
    public String jsonName() {
        return jsonName;
    }

    /**
     * Parses a type name from registry input.
     *
     * @param value type string (enum name or json name)
     * @return matching type
     * @throws IllegalArgumentException when unknown
     */
    public static UdfType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UDF type is required");
        }
        String normalized = value.trim();
        for (UdfType type : values()) {
            if (type.name().equalsIgnoreCase(normalized) || type.jsonName.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown UDF type: " + value);
    }

    @Override
    public String toString() {
        return jsonName;
    }
}
