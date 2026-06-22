/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.transform;

import org.gensokyo.data.calcite.sql.CalciteExecutionContext;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.JsonTransformVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link JsonTransformFactory}: parse-only, flatten, and parse-failure paths.
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
class JsonTransformFactoryTests {

    @Test
    void parsesIntoTargetColumnWhenNotFlattened() {
        RowSchema schema = schema(new ColumnDef("payload", "VARCHAR", false));
        List<Row> rows = List.of(new Row(Map.of("payload", "{\"a\":1,\"b\":\"x\"}")));
        CalciteExecutionContext context = new CalciteExecutionContext().addTable("input", schema, rows);

        JsonTransformVO transform = new JsonTransformVO();
        transform.setSourceColumn("payload");
        transform.setTargetColumn("parsed");

        CalciteRowTransformer.TransformResult result = new JsonTransformFactory().apply(transform, context);

        Object parsed = result.rows().getFirst().values().get("parsed");
        Assertions.assertInstanceOf(Map.class, parsed);
        Assertions.assertTrue(columnNames(result).contains("parsed"));
    }

    @Test
    void flattensNestedKeysWithSeparator() {
        RowSchema schema = schema(new ColumnDef("payload", "VARCHAR", false));
        List<Row> rows = List.of(new Row(Map.of("payload", "{\"addr\":{\"city\":\"GZ\"},\"id\":7}")));
        CalciteExecutionContext context = new CalciteExecutionContext().addTable("input", schema, rows);

        JsonTransformVO transform = new JsonTransformVO();
        transform.setSourceColumn("payload");
        transform.setFlatten(true);

        CalciteRowTransformer.TransformResult result = new JsonTransformFactory().apply(transform, context);

        Map<String, Object> values = result.rows().getFirst().values();
        Assertions.assertEquals("GZ", values.get("addr.city"));
        Assertions.assertEquals(7, ((Number) values.get("id")).intValue());
        Assertions.assertTrue(columnNames(result).contains("addr.city"));
    }

    @Test
    void parseFailureNamesColumnWithBoundedSnippet() {
        RowSchema schema = schema(new ColumnDef("payload", "VARCHAR", false));
        List<Row> rows = List.of(new Row(Map.of("payload", "{not-json")));
        CalciteExecutionContext context = new CalciteExecutionContext().addTable("input", schema, rows);

        JsonTransformVO transform = new JsonTransformVO();
        transform.setSourceColumn("payload");
        transform.setTargetColumn("parsed");

        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new JsonTransformFactory().apply(transform, context));
        Assertions.assertTrue(error.getMessage().contains("payload"));
    }

    private static List<String> columnNames(CalciteRowTransformer.TransformResult result) {
        return result.schema().getColumns().stream().map(ColumnDef::getName).toList();
    }

    private static RowSchema schema(ColumnDef... columns) {
        RowSchema rowSchema = new RowSchema();
        rowSchema.setColumns(List.of(columns));
        return rowSchema;
    }
}
