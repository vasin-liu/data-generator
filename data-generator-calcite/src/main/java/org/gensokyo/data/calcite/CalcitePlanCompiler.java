package org.gensokyo.data.calcite;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSelect;

public class CalcitePlanCompiler {
    private final TemplateV2SqlFunctionRegistry sqlFunctionRegistry;

    public CalcitePlanCompiler() {
        this(TemplateV2SqlFunctionRegistry.builtIn());
    }

    public CalcitePlanCompiler(TemplateV2SqlFunctionRegistry sqlFunctionRegistry) {
        this.sqlFunctionRegistry = sqlFunctionRegistry;
    }

    public CalciteCompiledPlan compile(String sql, CalciteExecutionContext context) {
        CalciteSqlValidationResult validation = new CalciteSqlValidator(sqlFunctionRegistry).validate(sql, context);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getMessage());
        }
        SqlNode node = validation.getSqlNode();
        if (!(node instanceof SqlSelect select)) {
            throw new UnsupportedOperationException("Only SELECT statements are supported in the current V2 skeleton");
        }
        return new CalciteCompiledPlan(sql, select);
    }
}
