/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sql;

import org.gensokyo.data.calcite.TemplateV2SqlFunction;
import org.gensokyo.data.calcite.TemplateV2SqlFunctionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Unit tests for the internal {@code V2_JSON_EXTRACT} scalar function (D-11/D-12): registration in
 * {@link TemplateV2SqlFunctionRegistry#builtIn()} and dot-path extraction semantics.
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
class TemplateV2JsonSqlFunctionTests {

    @Test
    void functionIsRegisteredOnceWithV2Prefix() {
        List<TemplateV2SqlFunction> matches = TemplateV2SqlFunctionRegistry.builtIn().functions().stream()
                .filter(function -> "V2_JSON_EXTRACT".equals(function.name()))
                .toList();
        Assertions.assertEquals(1, matches.size());
        Assertions.assertTrue(matches.getFirst().name().startsWith("V2_"));

        Optional<TemplateV2SqlFunction> found = TemplateV2SqlFunctionRegistry.builtIn().find("v2_json_extract");
        Assertions.assertTrue(found.isPresent());
    }

    @Test
    void extractsValueAtDotPath() {
        Object extracted = TemplateV2JsonSqlFunctions.jsonExtract(
                context("{\"addr\":{\"city\":\"GZ\"}}", "addr.city"));
        Assertions.assertEquals("GZ", extracted);
    }

    @Test
    void returnsNullOnUnresolvablePathOrBadJson() {
        Assertions.assertNull(TemplateV2JsonSqlFunctions.jsonExtract(context("{\"a\":1}", "missing")));
        Assertions.assertNull(TemplateV2JsonSqlFunctions.jsonExtract(context("{not-json", "a")));
        Assertions.assertNull(TemplateV2JsonSqlFunctions.jsonExtract(context(null, "a")));
    }

    private static TemplateV2SqlFunctionContext context(Object... arguments) {
        return new TemplateV2SqlFunctionContext(Arrays.asList(arguments));
    }
}
