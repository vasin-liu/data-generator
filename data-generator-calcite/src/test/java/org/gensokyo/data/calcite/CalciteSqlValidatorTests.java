package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.codec.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;
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
    void validatesDistinctProjection() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addSchema("input", schema("value", "BIGINT"));

        CalciteSqlValidationResult result = new CalciteSqlValidator().validate(
                "SELECT DISTINCT value FROM input ORDER BY value DESC",
                context
        );

        Assertions.assertTrue(result.isValid(), result.getMessage());
    }

    @Test
    void reportsUnknownColumn() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addSchema("input", schema("value", "BIGINT"));

        CalciteSqlValidationResult result = new CalciteSqlValidator().validate("SELECT missing FROM input", context);

        Assertions.assertFalse(result.isValid());
        Assertions.assertNotNull(result.getMessage());
    }

    @Test
    void validatesAggregateWithoutGroupBy() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addSchema("input", schema(
                        new ColumnDef("category", "VARCHAR", true),
                        new ColumnDef("amount", "BIGINT", true)
                ));

        CalciteSqlValidationResult result = new CalciteSqlValidator().validate(
                "SELECT COUNT(*) AS row_count, SUM(amount) AS total_amount FROM input",
                context
        );

        Assertions.assertTrue(result.isValid(), result.getMessage());
    }

    @Test
    void validatesSimpleGroupBy() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addSchema("input", schema(
                        new ColumnDef("category", "VARCHAR", true),
                        new ColumnDef("amount", "BIGINT", true)
                ));

        CalciteSqlValidationResult result = new CalciteSqlValidator().validate(
                "SELECT category, COUNT(*) AS row_count, SUM(amount) AS total_amount FROM input GROUP BY category",
                context
        );

        Assertions.assertTrue(result.isValid(), result.getMessage());
    }

    @Test
    void validatesDistinctAggregates() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addSchema("input", schema(
                        new ColumnDef("category", "VARCHAR", true),
                        new ColumnDef("amount", "BIGINT", true)
                ));

        CalciteSqlValidationResult result = new CalciteSqlValidator().validate(
                "SELECT category, COUNT(DISTINCT amount) AS distinct_amount_count, "
                        + "SUM(DISTINCT amount) AS distinct_amount_sum "
                        + "FROM input GROUP BY category",
                context
        );

        Assertions.assertTrue(result.isValid(), result.getMessage());
    }

    @Test
    void validatesGroupByHaving() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addSchema("input", schema(
                        new ColumnDef("category", "VARCHAR", true),
                        new ColumnDef("amount", "BIGINT", true)
                ));

        CalciteSqlValidationResult result = new CalciteSqlValidator().validate(
                "SELECT category, COUNT(*) AS row_count FROM input GROUP BY category HAVING COUNT(*) >= 1",
                context
        );

        Assertions.assertTrue(result.isValid(), result.getMessage());
    }

    @Test
    void validatesGeoSqlFunctions() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addSchema("geo_in", schema(
                        new ColumnDef("lat", "DOUBLE", true),
                        new ColumnDef("lon", "DOUBLE", true),
                        new ColumnDef("geometry", "VARCHAR", true)
                ));

        CalciteSqlValidationResult result = new CalciteSqlValidator().validate(
                """
                SELECT lat, lon,
                       V2_GEO_DISTANCE_METERS(lat, lon, 22.2, 113.2) AS dist_m,
                       V2_GEO_WKT_BUFFER(geometry, 500) AS buf
                FROM geo_in
                WHERE V2_GEO_WITHIN_RADIUS(lat, lon, 22.2, 113.2, 5000)
                  AND V2_GEO_POINT_IN_WKT(lat, lon, V2_GEO_WKT_BUFFER('POINT(113.2 22.2)', 1000))
                """,
                context
        );

        Assertions.assertTrue(result.isValid(), result.getMessage());
    }

    @Test
    void validatesGroupByOrderByAlias() {
        CalciteExecutionContext context = new CalciteExecutionContext()
                .addSchema("input", schema(
                        new ColumnDef("category", "VARCHAR", true),
                        new ColumnDef("amount", "BIGINT", true)
                ));

        CalciteSqlValidationResult result = new CalciteSqlValidator().validate(
                "SELECT category, COUNT(*) AS row_count FROM input GROUP BY category ORDER BY row_count DESC",
                context
        );

        Assertions.assertTrue(result.isValid(), result.getMessage());
    }

    private RowSchema schema(String column, String type) {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(new ColumnDef(column, type, true)));
        return schema;
    }

    private RowSchema schema(ColumnDef... columns) {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(columns));
        return schema;
    }
}
