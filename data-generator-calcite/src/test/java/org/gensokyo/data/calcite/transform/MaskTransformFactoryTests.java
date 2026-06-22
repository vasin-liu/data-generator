/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.transform;

import org.gensokyo.data.calcite.sql.CalciteExecutionContext;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.MaskRuleVO;
import org.gensokyo.data.model.v2.MaskTransformVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link MaskTransformFactory}: the four named strategies, schema invariance,
 * and PII-safe fail-fast on an unknown strategy.
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
class MaskTransformFactoryTests {

    @Test
    void masksEachStrategyInPlaceWithoutAddingColumns() {
        RowSchema schema = schema(
                new ColumnDef("email", "VARCHAR", false),
                new ColumnDef("phone", "VARCHAR", false),
                new ColumnDef("card", "VARCHAR", false),
                new ColumnDef("name", "VARCHAR", false));
        Map<String, Object> values = Map.of(
                "email", "john@example.com",
                "phone", "13800001234",
                "card", "4111-1111-1111-1111",
                "name", "John");
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addTable("input", schema, List.of(new Row(values)));

        MaskTransformVO transform = new MaskTransformVO();
        transform.setRules(List.of(
                rule("email", "email"),
                rule("phone", "phone"),
                rule("card", "credit-card"),
                rule("name", "generic-fixed")));

        CalciteRowTransformer.TransformResult result = new MaskTransformFactory().apply(transform, context);
        Map<String, Object> masked = result.rows().getFirst().values();

        Assertions.assertEquals("j***@example.com", masked.get("email"));
        Assertions.assertEquals("1234", ((String) masked.get("phone")).substring(7));
        Assertions.assertTrue(((String) masked.get("phone")).startsWith("*******"));
        Assertions.assertTrue(((String) masked.get("card")).endsWith("1111"));
        Assertions.assertTrue(((String) masked.get("card")).startsWith("****-****-****-"));
        Assertions.assertEquals("****", masked.get("name"));
        // Output schema must equal the input schema (masking adds no columns).
        Assertions.assertEquals(4, result.schema().getColumns().size());
    }

    @Test
    void unknownStrategyFailsFastWithoutLeakingValue() {
        RowSchema schema = schema(new ColumnDef("email", "VARCHAR", false));
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addTable("input", schema, List.of(new Row(Map.of("email", "secret@example.com"))));

        MaskTransformVO transform = new MaskTransformVO();
        transform.setRules(List.of(rule("email", "rot13")));

        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new MaskTransformFactory().apply(transform, context));
        Assertions.assertTrue(error.getMessage().contains("rot13"));
        Assertions.assertFalse(error.getMessage().contains("secret@example.com"));
    }

    private static MaskRuleVO rule(String column, String strategy) {
        MaskRuleVO rule = new MaskRuleVO();
        rule.setColumn(column);
        rule.setStrategy(strategy);
        return rule;
    }

    private static RowSchema schema(ColumnDef... columns) {
        RowSchema rowSchema = new RowSchema();
        rowSchema.setColumns(List.of(columns));
        return rowSchema;
    }
}
