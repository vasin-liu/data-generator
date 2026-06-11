package org.gensokyo.data.calcite;

import org.apache.calcite.sql.SqlBasicFunction;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.type.SqlOperandTypeChecker;
import org.apache.calcite.sql.type.SqlReturnTypeInference;

public record TemplateV2SqlFunction(String name,
                                    SqlReturnTypeInference returnTypeInference,
                                    SqlOperandTypeChecker operandTypeChecker,
                                    TemplateV2SqlFunctionEvaluator evaluator) {
    public TemplateV2SqlFunction {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("SQL function name must not be blank");
        }
        if (returnTypeInference == null) {
            throw new IllegalArgumentException("SQL function return type inference must not be null");
        }
        if (operandTypeChecker == null) {
            throw new IllegalArgumentException("SQL function operand type checker must not be null");
        }
        if (evaluator == null) {
            throw new IllegalArgumentException("SQL function evaluator must not be null");
        }
    }

    public SqlFunction toSqlFunction() {
        return SqlBasicFunction.create(name, returnTypeInference, operandTypeChecker);
    }
}
