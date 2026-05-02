package org.gensokyo.data.calcite;

import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.sql.util.SqlOperatorTables;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TemplateV2SqlFunctionRegistry {
    private final Map<String, TemplateV2SqlFunction> functions;

    public TemplateV2SqlFunctionRegistry(List<TemplateV2SqlFunction> functions) {
        Map<String, TemplateV2SqlFunction> indexed = new LinkedHashMap<>();
        for (TemplateV2SqlFunction function : functions) {
            indexed.put(normalize(function.name()), function);
        }
        this.functions = Map.copyOf(indexed);
    }

    public static TemplateV2SqlFunctionRegistry builtIn() {
        return new TemplateV2SqlFunctionRegistry(List.of(
                new TemplateV2SqlFunction("V2_TO_DATE", ReturnTypes.DATE_NULLABLE, OperandTypes.ANY,
                        context -> context.dateArgument(0)),
                new TemplateV2SqlFunction("V2_FORMAT_DATE", ReturnTypes.VARCHAR_NULLABLE, OperandTypes.ANY_ANY,
                        context -> context.dateArgument(1).format(DateTimeFormatter.ofPattern(
                                TemplateV2SqlFunctionContext.toJavaDatePattern(context.stringArgument(0))))),
                new TemplateV2SqlFunction("V2_DATE_ADD", ReturnTypes.DATE_NULLABLE, OperandTypes.ANY_ANY,
                        context -> context.dateArgument(0).plusDays(context.intArgument(1))),
                new TemplateV2SqlFunction("V2_DATE_SUB", ReturnTypes.DATE_NULLABLE, OperandTypes.ANY_ANY,
                        context -> context.dateArgument(0).minusDays(context.intArgument(1))),
                new TemplateV2SqlFunction("V2_DATE_DIFF", ReturnTypes.BIGINT_NULLABLE, OperandTypes.ANY_ANY,
                        context -> ChronoUnit.DAYS.between(context.dateArgument(1), context.dateArgument(0)))
        ));
    }

    public TemplateV2SqlFunctionRegistry with(TemplateV2SqlFunction function) {
        List<TemplateV2SqlFunction> merged = new ArrayList<>(functions.values());
        merged.add(function);
        return new TemplateV2SqlFunctionRegistry(merged);
    }

    public List<TemplateV2SqlFunction> functions() {
        return List.copyOf(functions.values());
    }

    public Optional<TemplateV2SqlFunction> find(String name) {
        return Optional.ofNullable(functions.get(normalize(name)));
    }

    public SqlOperatorTable operatorTable() {
        return SqlOperatorTables.of(functions.values().stream()
                .map(TemplateV2SqlFunction::toSqlFunction)
                .toList());
    }

    public static String normalize(String name) {
        return name.toUpperCase(Locale.ROOT);
    }
}
