/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sql;

import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SpelColumnMapping;
import org.gensokyo.data.model.v2.SpelTransformVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link SpelTransformFactory}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
class SpelTransformFactoryTests {

    @Test
    void evaluatesFakerExpression() {
        RowSchema schema = schema(new ColumnDef("id", "VARCHAR", false));
        List<Row> rows = List.of(new Row(Map.of("id", "x")));
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addTable("input", schema, rows);

        SpelTransformVO transform = new SpelTransformVO();
        transform.setColumns(List.of(mapping("n", "#faker.number.numberBetween(1,5)")));

        SpelTransformFactory factory = new SpelTransformFactory();
        CalciteRowTransformer.TransformResult result = factory.apply(transform, context);

        Object n = result.rows().getFirst().values().get("n");
        Assertions.assertInstanceOf(Number.class, n);
        int value = ((Number) n).intValue();
        Assertions.assertTrue(value >= 1 && value <= 5);
    }

    @Test
    void addsComputedColumnFromSpel() {
        RowSchema schema = schema(new ColumnDef("id", "VARCHAR", false));
        List<Row> rows = List.of(new Row(Map.of("id", "a")));
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addTable("input", schema, rows);

        SpelTransformVO transform = new SpelTransformVO();
        transform.setColumns(List.of(mapping("label", "#row['id'] + '-1'")));

        SpelTransformFactory factory = new SpelTransformFactory();
        CalciteRowTransformer.TransformResult result = factory.apply(transform, context);

        Assertions.assertEquals("a-1", result.rows().getFirst().values().get("label"));
        Assertions.assertEquals("a", result.rows().getFirst().values().get("id"));
    }

    private static SpelColumnMapping mapping(String name, String expression) {
        SpelColumnMapping mapping = new SpelColumnMapping();
        mapping.setName(name);
        mapping.setExpression(expression);
        return mapping;
    }

    private static RowSchema schema(ColumnDef... columns) {
        RowSchema rowSchema = new RowSchema();
        rowSchema.setColumns(List.of(columns));
        return rowSchema;
    }
}
