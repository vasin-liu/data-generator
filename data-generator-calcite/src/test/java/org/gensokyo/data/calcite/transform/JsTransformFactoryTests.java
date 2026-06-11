/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.transform;

import org.gensokyo.data.calcite.sql.CalciteExecutionContext;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.JsTransformVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link JsTransformFactory}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
class JsTransformFactoryTests {

    @Test
    void doublesAmountColumnViaRowScript() {
        RowSchema schema = schema(new ColumnDef("amount", "INT", false));
        List<Row> rows = List.of(new Row(Map.of("amount", 3)));
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addTable("input", schema, rows);

        JsTransformVO transform = new JsTransformVO();
        transform.setScript("row.amount = row.amount * 2");

        JsTransformFactory factory = new JsTransformFactory();
        CalciteRowTransformer.TransformResult result = factory.apply(transform, context);

        Assertions.assertEquals(6, result.rows().getFirst().values().get("amount"));
    }

    private static RowSchema schema(ColumnDef... columns) {
        RowSchema rowSchema = new RowSchema();
        rowSchema.setColumns(List.of(columns));
        return rowSchema;
    }
}
