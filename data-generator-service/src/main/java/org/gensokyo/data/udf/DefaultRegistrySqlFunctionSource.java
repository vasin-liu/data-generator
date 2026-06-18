/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.sql.type.SqlOperandTypeChecker;
import org.apache.calcite.sql.type.SqlReturnTypeInference;
import org.gensokyo.data.calcite.TemplateV2SqlFunction;
import org.gensokyo.data.calcite.udf.GraalJsScriptUdfExecutor;
import org.gensokyo.data.calcite.udf.RegistrySqlFunctionSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default {@link RegistrySqlFunctionSource} backed by the in-memory UDF registry.
 *
 * <p>Translates every published {@link UdfType#SQL} and {@link UdfType#SCRIPT} entry into a
 * SQL-callable {@link TemplateV2SqlFunction} whose evaluator runs the entry's GraalJS body
 * (D-10, D-11, D-13). Java plugins are intentionally excluded: they load through the existing
 * directory/PF4J path, not a parallel classloader (D-09).
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
public class DefaultRegistrySqlFunctionSource implements RegistrySqlFunctionSource {

    private static final int SCRIPT_TIMEOUT_MS = 5000;

    private final UdfRegistry registry;
    private final GraalJsScriptUdfExecutor executor;

    /**
     * @param registry backing UDF registry
     * @param executor GraalJS script UDF executor
     */
    public DefaultRegistrySqlFunctionSource(UdfRegistry registry, GraalJsScriptUdfExecutor executor) {
        this.registry = registry;
        this.executor = executor;
    }

    /**
     * @return SQL functions for all published SQL and script UDFs in the registry
     */
    @Override
    public List<TemplateV2SqlFunction> publishedSqlFunctions() {
        List<TemplateV2SqlFunction> functions = new ArrayList<>();
        for (UdfRecord record : registry.list(Optional.empty())) {
            if (record.state() != UdfLifecycleState.PUBLISHED) {
                continue;
            }
            if (record.type() != UdfType.SQL && record.type() != UdfType.SCRIPT) {
                continue;
            }
            functions.add(toSqlFunction(record));
        }
        return functions;
    }

    private TemplateV2SqlFunction toSqlFunction(UdfRecord record) {
        ScriptUdfPayload payload = ScriptUdfPayload.parse(record.payload());
        String script = payload.script();
        return new TemplateV2SqlFunction(
                payload.sqlName(),
                returnTypeInference(payload.returnType()),
                operandTypeChecker(payload.argCount()),
                context -> executor.execute(script, context.arguments(), SCRIPT_TIMEOUT_MS));
    }

    private static SqlReturnTypeInference returnTypeInference(String returnType) {
        return switch (returnType) {
            case "INTEGER" -> ReturnTypes.INTEGER_NULLABLE;
            case "BIGINT" -> ReturnTypes.BIGINT_NULLABLE;
            case "BOOLEAN" -> ReturnTypes.BOOLEAN_NULLABLE;
            case "DOUBLE" -> ReturnTypes.DOUBLE_NULLABLE;
            default -> ReturnTypes.VARCHAR_NULLABLE;
        };
    }

    private static SqlOperandTypeChecker operandTypeChecker(int argCount) {
        return switch (argCount) {
            case 0 -> OperandTypes.NILADIC;
            case 2 -> OperandTypes.ANY_ANY;
            default -> OperandTypes.ANY;
        };
    }
}
