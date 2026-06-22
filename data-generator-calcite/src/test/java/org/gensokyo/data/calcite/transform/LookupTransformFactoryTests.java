/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.transform;

import org.gensokyo.data.calcite.sql.CalciteExecutionContext;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.LookupTransformVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link LookupTransformFactory}: successful enrichment plus the three fail-fast
 * error paths (missing source, duplicate key, lookup miss).
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
class LookupTransformFactoryTests {

    @Test
    void enrichesInputRowsWithProjectedColumns() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addTable("input", schema(new ColumnDef("dept_id", "INT", false)),
                        List.of(new Row(Map.of("dept_id", 1)), new Row(Map.of("dept_id", 2))))
                .addTable("departments",
                        schema(new ColumnDef("id", "INT", false), new ColumnDef("name", "VARCHAR", false)),
                        List.of(new Row(Map.of("id", 1, "name", "Sales")), new Row(Map.of("id", 2, "name", "Eng"))));

        LookupTransformVO transform = lookup("departments", "dept_id", "id", List.of("name"));

        CalciteRowTransformer.TransformResult result = new LookupTransformFactory().apply(transform, context);

        Assertions.assertEquals("Sales", result.rows().get(0).values().get("name"));
        Assertions.assertEquals("Eng", result.rows().get(1).values().get("name"));
        Assertions.assertTrue(result.schema().getColumns().stream()
                .anyMatch(column -> "name".equalsIgnoreCase(column.getName())));
    }

    @Test
    void missingSourceFailsFast() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addTable("input", schema(new ColumnDef("dept_id", "INT", false)),
                        List.of(new Row(Map.of("dept_id", 1))));

        LookupTransformVO transform = lookup("departments", "dept_id", "id", List.of("name"));

        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new LookupTransformFactory().apply(transform, context));
        Assertions.assertTrue(error.getMessage().contains("departments"));
    }

    @Test
    void duplicateKeyFailsFast() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addTable("input", schema(new ColumnDef("dept_id", "INT", false)),
                        List.of(new Row(Map.of("dept_id", 1))))
                .addTable("departments",
                        schema(new ColumnDef("id", "INT", false), new ColumnDef("name", "VARCHAR", false)),
                        List.of(new Row(Map.of("id", 1, "name", "Sales")), new Row(Map.of("id", 1, "name", "Eng"))));

        LookupTransformVO transform = lookup("departments", "dept_id", "id", List.of("name"));

        IllegalStateException error = Assertions.assertThrows(IllegalStateException.class,
                () -> new LookupTransformFactory().apply(transform, context));
        Assertions.assertTrue(error.getMessage().contains("Duplicate"));
    }

    @Test
    void lookupMissFailsFast() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addTable("input", schema(new ColumnDef("dept_id", "INT", false)),
                        List.of(new Row(Map.of("dept_id", 99))))
                .addTable("departments",
                        schema(new ColumnDef("id", "INT", false), new ColumnDef("name", "VARCHAR", false)),
                        List.of(new Row(Map.of("id", 1, "name", "Sales"))));

        LookupTransformVO transform = lookup("departments", "dept_id", "id", List.of("name"));

        IllegalStateException error = Assertions.assertThrows(IllegalStateException.class,
                () -> new LookupTransformFactory().apply(transform, context));
        Assertions.assertTrue(error.getMessage().contains("99"));
    }

    private static LookupTransformVO lookup(String source, String leftKey, String rightKey, List<String> columns) {
        LookupTransformVO transform = new LookupTransformVO();
        transform.setSource(source);
        transform.setLeftKey(leftKey);
        transform.setRightKey(rightKey);
        transform.setColumns(columns);
        return transform;
    }

    private static RowSchema schema(ColumnDef... columns) {
        RowSchema rowSchema = new RowSchema();
        rowSchema.setColumns(List.of(columns));
        return rowSchema;
    }
}
