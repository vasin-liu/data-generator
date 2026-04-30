package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.RowSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class CalciteSqlValidatorTests {

    @Test
    void validatesSimpleProjection() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addSchema("input", schema("value", "BIGINT"));

        CalciteSqlValidationResult result = new CalciteSqlValidator().validate("SELECT value FROM input", context);

        Assertions.assertTrue(result.isValid(), result.getMessage());
        Assertions.assertNotNull(result.getSqlNode());
    }

    @Test
    void reportsUnknownColumn() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addSchema("input", schema("value", "BIGINT"));

        CalciteSqlValidationResult result = new CalciteSqlValidator().validate("SELECT missing FROM input", context);

        Assertions.assertFalse(result.isValid());
        Assertions.assertNotNull(result.getMessage());
    }

    private RowSchema schema(String column, String type) {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(new ColumnDef(column, type, true)));
        return schema;
    }
}
