package org.gensokyo.data.calcite;

import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeFactory.FieldInfoBuilder;
import org.apache.calcite.sql.type.SqlTypeName;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.RowSchema;

final class CalciteSchemaFactory {
    private CalciteSchemaFactory() {
    }

    static RelDataTypeFactory typeFactory() {
        return new JavaTypeFactoryImpl();
    }

    static RelDataType toRelType(RowSchema schema, RelDataTypeFactory typeFactory) {
        FieldInfoBuilder builder = typeFactory.builder();
        for (ColumnDef column : schema.getColumns()) {
            RelDataType columnType = typeFactory.createSqlType(resolveType(column.getLogicalType()));
            builder.add(column.getName(), columnType).nullable(column.isNullable());
        }
        return builder.build();
    }

    private static SqlTypeName resolveType(String type) {
        if (type == null || type.isBlank()) {
            return SqlTypeName.ANY;
        }
        try {
            return SqlTypeName.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return SqlTypeName.ANY;
        }
    }
}
