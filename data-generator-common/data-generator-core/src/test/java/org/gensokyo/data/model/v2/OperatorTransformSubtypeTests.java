/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import org.gensokyo.data.json.JsonSubtypeRegistry;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests proving the new {@code json}/{@code mask}/{@code lookup} operator subtypes resolve
 * through {@link JsonSubtypeRegistry} (AutoService discovery) and that the legacy {@code sql} subtype
 * is unaffected (additive schema change, D-13).
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
class OperatorTransformSubtypeTests {

    /**
     * Builds a mapper with {@link TransformVO} subtypes registered the same way the production factory does.
     *
     * @return JSON mapper resolving polymorphic transform subtypes by {@code type}
     */
    private static JsonMapper mapper() {
        return JsonMapper.builder()
                .registerSubtypes(JsonSubtypeRegistry.loadSubtypes(TransformVO.class))
                .build();
    }

    @Test
    void jsonTypeResolvesToJsonTransformVO() {
        JsonMapper mapper = mapper();

        // type: json with parse + opt-in flatten fields (D-02)
        TransformVO parsed = mapper.readValue(
                "{\"type\":\"json\",\"sourceColumn\":\"payload\",\"flatten\":true,\"separator\":\"_\"}",
                TransformVO.class);

        JsonTransformVO json = assertInstanceOf(JsonTransformVO.class, parsed);
        assertEquals("json", json.getType());
        assertEquals("payload", json.getSourceColumn());
        assertTrue(json.isFlatten());
        assertEquals("_", json.getSeparator());
    }

    @Test
    void maskTypeResolvesToMaskTransformVOWithRules() {
        JsonMapper mapper = mapper();

        // type: mask carrying a list of MaskRuleVO (D-03)
        TransformVO parsed = mapper.readValue(
                "{\"type\":\"mask\",\"rules\":[{\"column\":\"email\",\"strategy\":\"email\"}]}",
                TransformVO.class);

        MaskTransformVO mask = assertInstanceOf(MaskTransformVO.class, parsed);
        assertEquals("mask", mask.getType());
        assertEquals(1, mask.getRules().size());
        assertEquals("email", mask.getRules().get(0).getColumn());
        assertEquals("email", mask.getRules().get(0).getStrategy());
    }

    @Test
    void lookupTypeResolvesToLookupTransformVO() {
        JsonMapper mapper = mapper();

        // type: lookup against an in-template named source (D-04)
        TransformVO parsed = mapper.readValue(
                "{\"type\":\"lookup\",\"source\":\"ref\",\"leftKey\":\"id\",\"rightKey\":\"id\",\"columns\":[\"name\"]}",
                TransformVO.class);

        LookupTransformVO lookup = assertInstanceOf(LookupTransformVO.class, parsed);
        assertEquals("lookup", lookup.getType());
        assertEquals("ref", lookup.getSource());
        assertEquals("id", lookup.getLeftKey());
        assertEquals("id", lookup.getRightKey());
        assertEquals(List.of("name"), lookup.getColumns());
    }

    @Test
    void uppercaseAliasAlsoResolves() {
        JsonMapper mapper = mapper();

        // @JsonSubType value yields both UPPER id and lowercase alias (JsonSubtypeRegistry.namedTypes)
        TransformVO parsed = mapper.readValue("{\"type\":\"JSON\",\"sourceColumn\":\"p\"}", TransformVO.class);

        assertInstanceOf(JsonTransformVO.class, parsed);
    }

    @Test
    void legacySqlTypeStillResolves() {
        JsonMapper mapper = mapper();

        // additive proof (D-13): existing templates keep parsing unchanged
        TransformVO parsed = mapper.readValue("{\"type\":\"sql\",\"sql\":\"SELECT 1\"}", TransformVO.class);

        SqlTransformVO sql = assertInstanceOf(SqlTransformVO.class, parsed);
        assertEquals("sql", sql.getType());
        assertFalse(sql.getSql().isBlank());
    }
}
