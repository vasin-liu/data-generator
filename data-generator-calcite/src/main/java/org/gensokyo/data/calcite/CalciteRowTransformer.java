package org.gensokyo.data.calcite;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.fun.SqlCase;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public class CalciteRowTransformer {
    private final String sql;
    private final CalcitePlanCompiler compiler = new CalcitePlanCompiler();

    public TransformResult transform(CalciteExecutionContext context) {
        CalciteCompiledPlan plan = compiler.compile(sql, context);
        SqlSelect select = plan.getSelect();
        List<Row> input = materialize(select.getFrom(), context);

        List<Row> filtered = input.stream()
                .filter(row -> matches(row, select.getWhere()))
                .toList();
        RowSchema outputSchema = buildSchema(select.getSelectList());
        List<Row> output = filtered.stream()
                .map(row -> project(row, select.getSelectList()))
                .toList();
        return new TransformResult(outputSchema, output);
    }

    private List<Row> materialize(SqlNode from, CalciteExecutionContext context) {
        if (from instanceof SqlIdentifier identifier) {
            return rowsForTable(identifier.getSimple(), identifier.getSimple(), context);
        }
        if (from instanceof SqlBasicCall call && call.getKind() == SqlKind.AS) {
            return rowsForAliasedTable(call, context);
        }
        if (from instanceof SqlJoin join) {
            return joinRows(join, context);
        }
        throw new UnsupportedOperationException("Unsupported FROM clause in current V2 skeleton: " + from.getKind());
    }

    private List<Row> rowsForAliasedTable(SqlBasicCall call, CalciteExecutionContext context) {
        if (!(call.operand(0) instanceof SqlIdentifier identifier) || !(call.operand(1) instanceof SqlIdentifier alias)) {
            throw new UnsupportedOperationException("Only simple table aliases are supported in the current V2 skeleton");
        }
        return rowsForTable(identifier.getSimple(), alias.getSimple(), context);
    }

    private List<Row> rowsForTable(String tableName, String alias, CalciteExecutionContext context) {
        List<Row> sourceRows = context.getData().get(tableName);
        if (sourceRows == null) {
            throw new IllegalArgumentException("Source table not found: " + tableName);
        }
        String normalizedAlias = alias.toLowerCase(Locale.ROOT);
        return sourceRows.stream()
                .map(row -> qualifyRow(row, normalizedAlias))
                .toList();
    }

    private List<Row> joinRows(SqlJoin join, CalciteExecutionContext context) {
        String joinType = join.getJoinType().name();
        if (!"INNER".equals(joinType) && !"COMMA".equals(joinType)) {
            throw new UnsupportedOperationException("Only INNER JOIN is supported in the current V2 skeleton");
        }
        List<Row> leftRows = materialize(join.getLeft(), context);
        List<Row> rightRows = materialize(join.getRight(), context);
        List<Row> joined = new ArrayList<>();
        for (Row left : leftRows) {
            for (Row right : rightRows) {
                Row merged = mergeRows(left, right);
                if (matches(merged, join.getCondition())) {
                    joined.add(merged);
                }
            }
        }
        return joined;
    }

    private Row qualifyRow(Row row, String alias) {
        Map<String, Object> values = new LinkedHashMap<>();
        row.values().forEach((key, value) -> {
            String normalizedKey = key == null ? null : key.toLowerCase(Locale.ROOT);
            if (normalizedKey != null) {
                values.put(alias + "." + normalizedKey, value);
                values.putIfAbsent(normalizedKey, value);
            }
        });
        return new Row(values);
    }

    private Row mergeRows(Row left, Row right) {
        Map<String, Object> merged = new LinkedHashMap<>(left.values());
        right.values().forEach(merged::putIfAbsent);
        return new Row(merged);
    }

    private boolean matches(Row row, SqlNode where) {
        if (where == null) {
            return true;
        }
        Object value = evaluate(row, where);
        return value instanceof Boolean booleanValue && booleanValue;
    }

    private RowSchema buildSchema(SqlNodeList selectList) {
        RowSchema schema = new RowSchema();
        List<ColumnDef> columns = new ArrayList<>();
        for (SqlNode node : selectList) {
            String columnName = columnName(node);
            columns.add(new ColumnDef(columnName, "ANY", true));
        }
        schema.setColumns(columns);
        return schema;
    }

    private Row project(Row row, SqlNodeList selectList) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (SqlNode node : selectList) {
            values.put(columnName(node), evaluate(row, expression(node)));
        }
        return new Row(values);
    }

    private String columnName(SqlNode node) {
        if (node.getKind() == SqlKind.AS && node instanceof SqlBasicCall call && call.operand(1) instanceof SqlIdentifier alias) {
            return alias.getSimple();
        }
        if (node instanceof SqlIdentifier identifier) {
            return identifier.getSimple();
        }
        return node.toSqlString(org.apache.calcite.sql.dialect.AnsiSqlDialect.DEFAULT)
                .getSql()
                .toLowerCase(Locale.ROOT);
    }

    private SqlNode expression(SqlNode node) {
        if (node.getKind() == SqlKind.AS && node instanceof SqlBasicCall call) {
            return call.operand(0);
        }
        return node;
    }

    private Object evaluate(Row row, SqlNode node) {
        if (node instanceof SqlIdentifier identifier) {
            return resolveIdentifier(row, identifier);
        }
        if (node instanceof SqlLiteral literal) {
            return literal.toValue();
        }
        if (node instanceof SqlCase sqlCase) {
            return evaluateCase(row, sqlCase);
        }
        if (node instanceof SqlBasicCall call) {
            return evaluateCall(row, call);
        }
        throw new UnsupportedOperationException("Unsupported SQL expression in current V2 skeleton: " + node.getKind());
    }

    private Object evaluateCall(Row row, SqlBasicCall call) {
        SqlKind kind = call.getKind();
        return switch (kind) {
            case CAST -> evaluate(row, call.operand(0));
            case PLUS -> asBigDecimal(evaluate(row, call.operand(0))).add(asBigDecimal(evaluate(row, call.operand(1))));
            case MINUS -> asBigDecimal(evaluate(row, call.operand(0))).subtract(asBigDecimal(evaluate(row, call.operand(1))));
            case TIMES -> asBigDecimal(evaluate(row, call.operand(0))).multiply(asBigDecimal(evaluate(row, call.operand(1))));
            case DIVIDE -> asBigDecimal(evaluate(row, call.operand(0))).divide(asBigDecimal(evaluate(row, call.operand(1))));
            case EQUALS -> Objects.equals(normalizeComparable(evaluate(row, call.operand(0))), normalizeComparable(evaluate(row, call.operand(1))));
            case NOT_EQUALS -> !Objects.equals(normalizeComparable(evaluate(row, call.operand(0))), normalizeComparable(evaluate(row, call.operand(1))));
            case GREATER_THAN -> compare(row, call) > 0;
            case GREATER_THAN_OR_EQUAL -> compare(row, call) >= 0;
            case LESS_THAN -> compare(row, call) < 0;
            case LESS_THAN_OR_EQUAL -> compare(row, call) <= 0;
            case AND -> (Boolean) evaluate(row, call.operand(0)) && (Boolean) evaluate(row, call.operand(1));
            case OR -> (Boolean) evaluate(row, call.operand(0)) || (Boolean) evaluate(row, call.operand(1));
            case IS_NULL -> evaluate(row, call.operand(0)) == null;
            case IS_NOT_NULL -> evaluate(row, call.operand(0)) != null;
            default -> throw new UnsupportedOperationException("Unsupported SQL operator in current V2 skeleton: " + kind);
        };
    }

    private Object evaluateCase(Row row, SqlCase sqlCase) {
        SqlNodeList whenOperands = sqlCase.getWhenOperands();
        SqlNodeList thenOperands = sqlCase.getThenOperands();
        for (int i = 0; i < whenOperands.size(); i++) {
            Object condition = evaluate(row, whenOperands.get(i));
            if (condition instanceof Boolean booleanCondition && booleanCondition) {
                return evaluate(row, thenOperands.get(i));
            }
        }
        SqlNode elseOperand = sqlCase.getElseOperand();
        return elseOperand == null ? null : evaluate(row, elseOperand);
    }

    private Object resolveIdentifier(Row row, SqlIdentifier identifier) {
        if (identifier.names.isEmpty()) {
            return null;
        }
        if (identifier.names.size() == 1) {
            return row.get(simpleName(identifier).toLowerCase(Locale.ROOT));
        }
        String qualified = String.join(".", identifier.names).toLowerCase(Locale.ROOT);
        Object value = row.get(qualified);
        return value != null ? value : row.get(simpleName(identifier).toLowerCase(Locale.ROOT));
    }

    private String simpleName(SqlIdentifier identifier) {
        return identifier.names.getLast();
    }

    private int compare(Row row, SqlBasicCall call) {
        BigDecimal left = asBigDecimal(evaluate(row, call.operand(0)));
        BigDecimal right = asBigDecimal(evaluate(row, call.operand(1)));
        return left.compareTo(right);
    }

    private Object normalizeComparable(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros();
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).stripTrailingZeros();
        }
        return value;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String stringValue) {
            return new BigDecimal(stringValue);
        }
        throw new IllegalArgumentException("Expected numeric value but got: " + value);
    }

    public record TransformResult(RowSchema schema, List<Row> rows) {
    }
}
