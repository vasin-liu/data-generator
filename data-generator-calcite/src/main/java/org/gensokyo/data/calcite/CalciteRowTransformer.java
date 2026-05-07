package org.gensokyo.data.calcite;

import lombok.Getter;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.fun.SqlCase;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Getter
public class CalciteRowTransformer {
    private final String sql;
    private final TemplateV2SqlFunctionRegistry sqlFunctionRegistry;
    private final CalcitePlanCompiler compiler;

    public CalciteRowTransformer(String sql) {
        this(sql, TemplateV2SqlFunctionRegistry.builtIn());
    }

    public CalciteRowTransformer(String sql, TemplateV2SqlFunctionRegistry sqlFunctionRegistry) {
        this.sql = sql;
        this.sqlFunctionRegistry = sqlFunctionRegistry;
        this.compiler = new CalcitePlanCompiler(sqlFunctionRegistry);
    }

    public TransformResult transform(CalciteExecutionContext context) {
        CalciteCompiledPlan plan = compiler.compile(sql, context);
        SqlSelect select = plan.getSelect();
        List<Row> input = materialize(select.getFrom(), context);

        List<Row> filtered = input.stream()
                .filter(row -> matches(row, select.getWhere()))
                .toList();
        if (requiresAggregation(select, plan.getOrderBy())) {
            return aggregateTransform(plan, select, filtered);
        }
        List<Row> ordered = applyOrderBy(filtered, plan.getOrderBy());
        List<Row> paged = applyLimitAndOffset(ordered, plan.getOffset(), plan.getFetch());
        RowSchema outputSchema = buildSchema(select.getSelectList());
        List<Row> output = paged.stream()
                .map(row -> project(row, select.getSelectList()))
                .toList();
        return new TransformResult(outputSchema, output);
    }

    private TransformResult aggregateTransform(CalciteCompiledPlan plan, SqlSelect select, List<Row> filtered) {
        Map<String, SqlNode> aliasExpressions = selectAliasExpressions(select.getSelectList());
        List<AggregateFrame> grouped = groupRows(filtered, select.getGroup());
        List<AggregateFrame> havingPassed = grouped.stream()
                .filter(frame -> matches(frame, select.getHaving(), aliasExpressions))
                .toList();
        List<AggregateFrame> ordered = applyOrderByFrames(havingPassed, plan.getOrderBy(), aliasExpressions);
        List<AggregateFrame> paged = applyLimitAndOffset(ordered, plan.getOffset(), plan.getFetch());
        RowSchema outputSchema = buildSchema(select.getSelectList());
        List<Row> output = paged.stream()
                .map(frame -> project(frame, select.getSelectList(), aliasExpressions))
                .toList();
        return new TransformResult(outputSchema, output);
    }

    private boolean requiresAggregation(SqlSelect select, SqlNodeList orderBy) {
        return hasGroupBy(select.getGroup())
                || hasAggregate(select.getSelectList())
                || hasAggregate(select.getHaving())
                || hasAggregate(orderBy);
    }

    private boolean hasGroupBy(SqlNodeList groupBy) {
        return groupBy != null && !groupBy.isEmpty();
    }

    private boolean hasAggregate(SqlNode node) {
        if (node == null) {
            return false;
        }
        if (node instanceof SqlNodeList nodeList) {
            for (SqlNode child : nodeList) {
                if (hasAggregate(child)) {
                    return true;
                }
            }
            return false;
        }
        if (node instanceof SqlCase sqlCase) {
            return hasAggregate(sqlCase.getWhenOperands())
                    || hasAggregate(sqlCase.getThenOperands())
                    || hasAggregate(sqlCase.getElseOperand());
        }
        if (node instanceof SqlBasicCall call) {
            if (isAggregateCall(call)) {
                return true;
            }
            for (SqlNode child : call.getOperandList()) {
                if (hasAggregate(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAggregateCall(SqlBasicCall call) {
        String operatorName = call.getOperator().getName().toUpperCase(Locale.ROOT);
        return call.getOperator().isAggregator() || switch (operatorName) {
            case "COUNT", "SUM", "AVG", "MIN", "MAX" -> true;
            default -> false;
        };
    }

    private Map<String, SqlNode> selectAliasExpressions(SqlNodeList selectList) {
        Map<String, SqlNode> aliases = new LinkedHashMap<>();
        for (SqlNode node : selectList) {
            if (node.getKind() == SqlKind.AS
                    && node instanceof SqlBasicCall call
                    && call.operand(1) instanceof SqlIdentifier alias) {
                aliases.put(alias.getSimple().toLowerCase(Locale.ROOT), expression(node));
            }
        }
        return aliases;
    }

    private List<AggregateFrame> groupRows(List<Row> rows, SqlNodeList groupBy) {
        if (!hasGroupBy(groupBy)) {
            return List.of(new AggregateFrame(rows, rows.isEmpty() ? new Row(Map.of()) : rows.getFirst()));
        }
        Map<GroupKey, List<Row>> groupedRows = new LinkedHashMap<>();
        Map<GroupKey, Row> representatives = new LinkedHashMap<>();
        for (Row row : rows) {
            GroupKey key = new GroupKey(groupKeyValues(row, groupBy));
            groupedRows.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
            representatives.putIfAbsent(key, row);
        }
        List<AggregateFrame> frames = new ArrayList<>();
        for (Map.Entry<GroupKey, List<Row>> entry : groupedRows.entrySet()) {
            frames.add(new AggregateFrame(entry.getValue(), representatives.get(entry.getKey())));
        }
        return frames;
    }

    private List<Object> groupKeyValues(Row row, SqlNodeList groupBy) {
        List<Object> values = new ArrayList<>(groupBy.size());
        for (SqlNode node : groupBy) {
            values.add(normalizeComparable(evaluate(row, node)));
        }
        return values;
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

    private List<Row> applyOrderBy(List<Row> rows, SqlNodeList orderBy) {
        if (orderBy == null || orderBy.isEmpty()) {
            return rows;
        }
        List<Row> sorted = new ArrayList<>(rows);
        sorted.sort(orderComparator(orderBy));
        return sorted;
    }

    private List<AggregateFrame> applyOrderByFrames(List<AggregateFrame> frames,
                                                    SqlNodeList orderBy,
                                                    Map<String, SqlNode> aliasExpressions) {
        if (orderBy == null || orderBy.isEmpty()) {
            return frames;
        }
        List<AggregateFrame> sorted = new ArrayList<>(frames);
        sorted.sort(orderFrameComparator(orderBy, aliasExpressions));
        return sorted;
    }

    private Comparator<Row> orderComparator(SqlNodeList orderBy) {
        Comparator<Row> comparator = null;
        for (SqlNode node : orderBy) {
            Comparator<Row> next = comparatorForOrderSpec(orderSpec(node));
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        return comparator == null ? (left, right) -> 0 : comparator;
    }

    private Comparator<AggregateFrame> orderFrameComparator(SqlNodeList orderBy,
                                                            Map<String, SqlNode> aliasExpressions) {
        Comparator<AggregateFrame> comparator = null;
        for (SqlNode node : orderBy) {
            Comparator<AggregateFrame> next = comparatorForOrderSpec(orderSpec(node), aliasExpressions);
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        return comparator == null ? (left, right) -> 0 : comparator;
    }

    private Comparator<Row> comparatorForOrderSpec(OrderSpec spec) {
        return comparingRows(spec.expression(), spec.ascending(), spec.nullsLast());
    }

    private Comparator<AggregateFrame> comparatorForOrderSpec(OrderSpec spec,
                                                              Map<String, SqlNode> aliasExpressions) {
        return comparingFrames(spec.expression(), spec.ascending(), spec.nullsLast(), aliasExpressions);
    }

    private Comparator<Row> comparingRows(SqlNode expression, boolean ascending) {
        return comparingRows(expression, ascending, false);
    }

    private Comparator<Row> comparingRows(SqlNode expression, boolean ascending, boolean nullsLast) {
        return (left, right) -> {
            Object leftValue = evaluate(left, expression);
            Object rightValue = evaluate(right, expression);
            int compared = compareValues(leftValue, rightValue, nullsLast);
            return ascending ? compared : -compared;
        };
    }

    private Comparator<AggregateFrame> comparingFrames(SqlNode expression,
                                                       boolean ascending,
                                                       boolean nullsLast,
                                                       Map<String, SqlNode> aliasExpressions) {
        return (left, right) -> {
            SqlNode resolved = resolveAggregateExpression(expression, aliasExpressions);
            Object leftValue = evaluate(left, resolved, aliasExpressions);
            Object rightValue = evaluate(right, resolved, aliasExpressions);
            int compared = compareValues(leftValue, rightValue, nullsLast);
            return ascending ? compared : -compared;
        };
    }

    private Object literalValue(SqlNode node) {
        if (node instanceof SqlLiteral literal) {
            return literal.toValue();
        }
        throw new UnsupportedOperationException("Only literal OFFSET/FETCH values are supported in the current V2 skeleton");
    }

    private <T> List<T> applyLimitAndOffset(List<T> rows, SqlNode offsetNode, SqlNode fetchNode) {
        int fromIndex = offsetNode == null ? 0 : Math.max(0, toInt(literalValue(offsetNode)));
        if (fromIndex >= rows.size()) {
            return List.of();
        }
        int toIndex = rows.size();
        if (fetchNode != null) {
            int fetch = Math.max(0, toInt(literalValue(fetchNode)));
            toIndex = Math.min(rows.size(), fromIndex + fetch);
        }
        return rows.subList(fromIndex, toIndex);
    }

    private OrderSpec orderSpec(SqlNode node) {
        if (!(node instanceof SqlBasicCall call)) {
            return new OrderSpec(node, true, false);
        }
        return switch (call.getKind()) {
            case DESCENDING -> orderSpec(call.operand(0)).withDescending();
            case NULLS_FIRST -> orderSpec(call.operand(0)).withNullsFirst();
            case NULLS_LAST -> orderSpec(call.operand(0)).withNullsLast();
            default -> new OrderSpec(node, true, false);
        };
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
        JoinType joinType = join.getJoinType();
        if (joinType != JoinType.INNER && joinType != JoinType.COMMA && joinType != JoinType.LEFT) {
            throw new UnsupportedOperationException("Only INNER JOIN and LEFT JOIN are supported in the current V2 skeleton");
        }
        List<Row> leftRows = materialize(join.getLeft(), context);
        List<Row> rightRows = materialize(join.getRight(), context);
        List<String> rightColumns = collectColumns(rightRows);
        List<Row> joined = new ArrayList<>();
        for (Row left : leftRows) {
            boolean matched = false;
            for (Row right : rightRows) {
                Row merged = mergeRows(left, right);
                if (matches(merged, join.getCondition())) {
                    matched = true;
                    joined.add(merged);
                }
            }
            if (!matched && joinType == JoinType.LEFT) {
                joined.add(mergeRows(left, nullPaddedRow(rightColumns)));
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

    private List<String> collectColumns(List<Row> rows) {
        List<String> columns = new ArrayList<>();
        for (Row row : rows) {
            for (String key : row.values().keySet()) {
                if (!columns.contains(key)) {
                    columns.add(key);
                }
            }
        }
        return columns;
    }

    private Row nullPaddedRow(List<String> columns) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String column : columns) {
            values.put(column, null);
        }
        return new Row(values);
    }

    private boolean matches(Row row, SqlNode where) {
        if (where == null) {
            return true;
        }
        Object value = evaluate(row, where);
        return value instanceof Boolean booleanValue && booleanValue;
    }

    private boolean matches(AggregateFrame frame, SqlNode where, Map<String, SqlNode> aliasExpressions) {
        if (where == null) {
            return true;
        }
        return toBoolean(evaluate(frame, where, aliasExpressions));
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

    private Row project(AggregateFrame frame,
                        SqlNodeList selectList,
                        Map<String, SqlNode> aliasExpressions) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (SqlNode node : selectList) {
            values.put(columnName(node), evaluate(frame, expression(node), aliasExpressions));
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

    private SqlNode resolveAggregateExpression(SqlNode node, Map<String, SqlNode> aliasExpressions) {
        if (node instanceof SqlIdentifier identifier && identifier.names.size() == 1) {
            SqlNode aliased = aliasExpressions.get(identifier.getSimple().toLowerCase(Locale.ROOT));
            if (aliased != null) {
                return aliased;
            }
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

    private Object evaluate(AggregateFrame frame, SqlNode node, Map<String, SqlNode> aliasExpressions) {
        SqlNode resolved = resolveAggregateExpression(node, aliasExpressions);
        if (resolved instanceof SqlIdentifier identifier) {
            return resolveIdentifier(frame.representative(), identifier);
        }
        if (resolved instanceof SqlLiteral literal) {
            return literal.toValue();
        }
        if (resolved instanceof SqlCase sqlCase) {
            return evaluateCase(frame, sqlCase, aliasExpressions);
        }
        if (resolved instanceof SqlBasicCall call) {
            if (isAggregateCall(call)) {
                return evaluateAggregateCall(frame, call, aliasExpressions);
            }
            return evaluateCall(frame, call, aliasExpressions);
        }
        throw new UnsupportedOperationException("Unsupported SQL expression in current V2 skeleton: " + resolved.getKind());
    }

    private Object evaluateCall(Row row, SqlBasicCall call) {
        SqlKind kind = call.getKind();
        return switch (kind) {
            case CAST, CAST_NOT_NULL -> evaluate(row, call.operand(0));
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
            case NOT -> !toBoolean(evaluate(row, call.operand(0)));
            case IS_NULL -> evaluate(row, call.operand(0)) == null;
            case IS_NOT_NULL -> evaluate(row, call.operand(0)) != null;
            case LIKE -> like(row, call);
            case BETWEEN -> between(row, call);
            case IN -> in(row, call);
            case EXTRACT -> extract(row, call);
            default -> evaluateFunction(row, call);
        };
    }

    private Object evaluateCall(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        SqlKind kind = call.getKind();
        return switch (kind) {
            case CAST, CAST_NOT_NULL -> evaluate(frame, call.operand(0), aliasExpressions);
            case PLUS -> asBigDecimal(evaluate(frame, call.operand(0), aliasExpressions)).add(asBigDecimal(evaluate(frame, call.operand(1), aliasExpressions)));
            case MINUS -> asBigDecimal(evaluate(frame, call.operand(0), aliasExpressions)).subtract(asBigDecimal(evaluate(frame, call.operand(1), aliasExpressions)));
            case TIMES -> asBigDecimal(evaluate(frame, call.operand(0), aliasExpressions)).multiply(asBigDecimal(evaluate(frame, call.operand(1), aliasExpressions)));
            case DIVIDE -> asBigDecimal(evaluate(frame, call.operand(0), aliasExpressions)).divide(asBigDecimal(evaluate(frame, call.operand(1), aliasExpressions)));
            case EQUALS -> Objects.equals(normalizeComparable(evaluate(frame, call.operand(0), aliasExpressions)),
                    normalizeComparable(evaluate(frame, call.operand(1), aliasExpressions)));
            case NOT_EQUALS -> !Objects.equals(normalizeComparable(evaluate(frame, call.operand(0), aliasExpressions)),
                    normalizeComparable(evaluate(frame, call.operand(1), aliasExpressions)));
            case GREATER_THAN -> compare(frame, call, aliasExpressions) > 0;
            case GREATER_THAN_OR_EQUAL -> compare(frame, call, aliasExpressions) >= 0;
            case LESS_THAN -> compare(frame, call, aliasExpressions) < 0;
            case LESS_THAN_OR_EQUAL -> compare(frame, call, aliasExpressions) <= 0;
            case AND -> toBoolean(evaluate(frame, call.operand(0), aliasExpressions)) && toBoolean(evaluate(frame, call.operand(1), aliasExpressions));
            case OR -> toBoolean(evaluate(frame, call.operand(0), aliasExpressions)) || toBoolean(evaluate(frame, call.operand(1), aliasExpressions));
            case NOT -> !toBoolean(evaluate(frame, call.operand(0), aliasExpressions));
            case IS_NULL -> evaluate(frame, call.operand(0), aliasExpressions) == null;
            case IS_NOT_NULL -> evaluate(frame, call.operand(0), aliasExpressions) != null;
            case LIKE -> like(frame, call, aliasExpressions);
            case BETWEEN -> between(frame, call, aliasExpressions);
            case IN -> in(frame, call, aliasExpressions);
            case EXTRACT -> extract(frame, call, aliasExpressions);
            default -> evaluateFunction(frame, call, aliasExpressions);
        };
    }

    private boolean like(Row row, SqlBasicCall call) {
        Object candidate = evaluate(row, call.operand(0));
        if (candidate == null) {
            return false;
        }
        Object patternValue = evaluate(row, call.operand(1));
        if (patternValue == null) {
            return false;
        }
        Character escape = null;
        if (call.operandCount() > 2) {
            Object escapeValue = evaluate(row, call.operand(2));
            if (escapeValue != null) {
                String text = escapeValue.toString();
                escape = text.isEmpty() ? null : text.charAt(0);
            }
        }
        return candidate.toString().matches(likePatternToRegex(patternValue.toString(), escape));
    }

    private boolean like(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        Object candidate = evaluate(frame, call.operand(0), aliasExpressions);
        if (candidate == null) {
            return false;
        }
        Object patternValue = evaluate(frame, call.operand(1), aliasExpressions);
        if (patternValue == null) {
            return false;
        }
        Character escape = null;
        if (call.operandCount() > 2) {
            Object escapeValue = evaluate(frame, call.operand(2), aliasExpressions);
            if (escapeValue != null) {
                String text = escapeValue.toString();
                escape = text.isEmpty() ? null : text.charAt(0);
            }
        }
        return candidate.toString().matches(likePatternToRegex(patternValue.toString(), escape));
    }

    private boolean between(Row row, SqlBasicCall call) {
        Object value = evaluate(row, call.operand(0));
        Object lower = evaluate(row, call.operand(call.operandCount() - 2));
        Object upper = evaluate(row, call.operand(call.operandCount() - 1));
        return compareValues(value, lower, false) >= 0 && compareValues(value, upper, false) <= 0;
    }

    private boolean between(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        Object value = evaluate(frame, call.operand(0), aliasExpressions);
        Object lower = evaluate(frame, call.operand(call.operandCount() - 2), aliasExpressions);
        Object upper = evaluate(frame, call.operand(call.operandCount() - 1), aliasExpressions);
        return compareValues(value, lower, false) >= 0 && compareValues(value, upper, false) <= 0;
    }

    private boolean in(Row row, SqlBasicCall call) {
        Object value = evaluate(row, call.operand(0));
        Object normalizedValue = normalizeComparable(value);
        List<Object> candidates = new ArrayList<>();
        for (int i = 1; i < call.operandCount(); i++) {
            collectInCandidates(row, call.operand(i), candidates);
        }
        for (Object candidate : candidates) {
            if (Objects.equals(normalizedValue, normalizeComparable(candidate))) {
                return true;
            }
        }
        return false;
    }

    private boolean in(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        Object value = evaluate(frame, call.operand(0), aliasExpressions);
        Object normalizedValue = normalizeComparable(value);
        List<Object> candidates = new ArrayList<>();
        for (int i = 1; i < call.operandCount(); i++) {
            collectInCandidates(frame, call.operand(i), candidates, aliasExpressions);
        }
        for (Object candidate : candidates) {
            if (Objects.equals(normalizedValue, normalizeComparable(candidate))) {
                return true;
            }
        }
        return false;
    }

    private void collectInCandidates(Row row, SqlNode node, List<Object> candidates) {
        if (node instanceof SqlNodeList nodeList) {
            for (SqlNode child : nodeList) {
                collectInCandidates(row, child, candidates);
            }
            return;
        }
        if (node instanceof SqlBasicCall call && call.getKind() == SqlKind.ROW) {
            for (SqlNode child : call.getOperandList()) {
                collectInCandidates(row, child, candidates);
            }
            return;
        }
        candidates.add(evaluate(row, node));
    }

    private void collectInCandidates(AggregateFrame frame,
                                     SqlNode node,
                                     List<Object> candidates,
                                     Map<String, SqlNode> aliasExpressions) {
        if (node instanceof SqlNodeList nodeList) {
            for (SqlNode child : nodeList) {
                collectInCandidates(frame, child, candidates, aliasExpressions);
            }
            return;
        }
        if (node instanceof SqlBasicCall call && call.getKind() == SqlKind.ROW) {
            for (SqlNode child : call.getOperandList()) {
                collectInCandidates(frame, child, candidates, aliasExpressions);
            }
            return;
        }
        candidates.add(evaluate(frame, node, aliasExpressions));
    }

    private Object evaluateFunction(Row row, SqlBasicCall call) {
        String functionName = call.getOperator().getName().toUpperCase(Locale.ROOT);
        return switch (functionName) {
            case "COALESCE" -> coalesce(row, call);
            case "NULLIF" -> nullif(row, call);
            case "CONCAT" -> concat(row, call);
            case "UPPER" -> {
                Object value = evaluate(row, call.operand(0));
                yield value == null ? null : value.toString().toUpperCase(Locale.ROOT);
            }
            case "LOWER" -> {
                Object value = evaluate(row, call.operand(0));
                yield value == null ? null : value.toString().toLowerCase(Locale.ROOT);
            }
            case "TRIM" -> trim(row, call);
            case "CHAR_LENGTH", "CHARACTER_LENGTH", "LENGTH" -> length(row, call);
            case "SUBSTRING", "SUBSTR" -> substring(row, call);
            case "ABS" -> numeric(row, call, BigDecimal::abs);
            case "FLOOR" -> numeric(row, call, value -> value.setScale(0, RoundingMode.FLOOR));
            case "CEIL", "CEILING" -> numeric(row, call, value -> value.setScale(0, RoundingMode.CEILING));
            case "ROUND" -> round(row, call);
            case "YEAR" -> toLocalDate(evaluate(row, call.operand(0))).getYear();
            case "MONTH" -> toLocalDate(evaluate(row, call.operand(0))).getMonthValue();
            case "DAYOFMONTH", "DAY" -> toLocalDate(evaluate(row, call.operand(0))).getDayOfMonth();
            default -> evaluateRegisteredFunction(row, call, functionName);
        };
    }

    private Object evaluateFunction(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        String functionName = call.getOperator().getName().toUpperCase(Locale.ROOT);
        return switch (functionName) {
            case "COALESCE" -> firstNonNull(call.getOperandList(), operand -> evaluate(frame, operand, aliasExpressions));
            case "NULLIF" -> {
                Object left = evaluate(frame, call.operand(0), aliasExpressions);
                Object right = evaluate(frame, call.operand(1), aliasExpressions);
                yield Objects.equals(normalizeComparable(left), normalizeComparable(right)) ? null : left;
            }
            case "CONCAT" -> concat(call.getOperandList(), operand -> evaluate(frame, operand, aliasExpressions));
            case "UPPER" -> upper(evaluate(frame, call.operand(0), aliasExpressions));
            case "LOWER" -> lower(evaluate(frame, call.operand(0), aliasExpressions));
            case "TRIM" -> trim(evaluate(frame, call.operand(call.operandCount() - 1), aliasExpressions));
            case "CHAR_LENGTH", "CHARACTER_LENGTH", "LENGTH" -> length(evaluate(frame, call.operand(0), aliasExpressions));
            case "SUBSTRING", "SUBSTR" -> substring(frame, call, aliasExpressions);
            case "ABS" -> numeric(evaluate(frame, call.operand(0), aliasExpressions), BigDecimal::abs);
            case "FLOOR" -> numeric(evaluate(frame, call.operand(0), aliasExpressions), value -> value.setScale(0, RoundingMode.FLOOR));
            case "CEIL", "CEILING" -> numeric(evaluate(frame, call.operand(0), aliasExpressions), value -> value.setScale(0, RoundingMode.CEILING));
            case "ROUND" -> round(frame, call, aliasExpressions);
            case "YEAR" -> toLocalDate(evaluate(frame, call.operand(0), aliasExpressions)).getYear();
            case "MONTH" -> toLocalDate(evaluate(frame, call.operand(0), aliasExpressions)).getMonthValue();
            case "DAYOFMONTH", "DAY" -> toLocalDate(evaluate(frame, call.operand(0), aliasExpressions)).getDayOfMonth();
            default -> evaluateRegisteredFunction(frame, call, functionName, aliasExpressions);
        };
    }

    private Object evaluateAggregateCall(AggregateFrame frame,
                                         SqlBasicCall call,
                                         Map<String, SqlNode> aliasExpressions) {
        String functionName = call.getOperator().getName().toUpperCase(Locale.ROOT);
        return switch (functionName) {
            case "COUNT" -> count(frame, call, aliasExpressions);
            case "SUM" -> sum(frame, call, aliasExpressions);
            case "AVG" -> avg(frame, call, aliasExpressions);
            case "MIN" -> min(frame, call, aliasExpressions);
            case "MAX" -> max(frame, call, aliasExpressions);
            default -> throw new UnsupportedOperationException("Unsupported aggregate function in current V2 skeleton: "
                    + functionName);
        };
    }

    private long count(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        if (call.operandCount() == 0 || isStarOperand(call.operand(0))) {
            return frame.rows().size();
        }
        long count = 0;
        for (Row row : frame.rows()) {
            Object value = evaluate(row, call.operand(0));
            if (value != null) {
                count++;
            }
        }
        return count;
    }

    private BigDecimal sum(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        BigDecimal total = null;
        for (Row row : frame.rows()) {
            Object value = evaluate(row, call.operand(0));
            if (value != null) {
                total = total == null ? asBigDecimal(value) : total.add(asBigDecimal(value));
            }
        }
        return total;
    }

    private BigDecimal avg(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        BigDecimal total = null;
        int count = 0;
        for (Row row : frame.rows()) {
            Object value = evaluate(row, call.operand(0));
            if (value != null) {
                total = total == null ? asBigDecimal(value) : total.add(asBigDecimal(value));
                count++;
            }
        }
        if (total == null || count == 0) {
            return null;
        }
        return total.divide(BigDecimal.valueOf(count), 8, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private Object min(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        return extremum(frame, call, aliasExpressions, compared -> compared < 0);
    }

    private Object max(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        return extremum(frame, call, aliasExpressions, compared -> compared > 0);
    }

    private Object extremum(AggregateFrame frame,
                            SqlBasicCall call,
                            Map<String, SqlNode> aliasExpressions,
                            java.util.function.IntPredicate predicate) {
        Object best = null;
        for (Row row : frame.rows()) {
            Object candidate = evaluate(row, call.operand(0));
            if (candidate == null) {
                continue;
            }
            if (best == null || predicate.test(compareValues(candidate, best, false))) {
                best = candidate;
            }
        }
        return best;
    }

    private boolean isStarOperand(SqlNode node) {
        return node instanceof SqlIdentifier identifier
                && identifier.names.size() == 1
                && "*".equals(identifier.getSimple());
    }

    private Object coalesce(Row row, SqlBasicCall call) {
        for (SqlNode operand : call.getOperandList()) {
            Object value = evaluate(row, operand);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Object firstNonNull(List<SqlNode> operands, Function<SqlNode, Object> evaluator) {
        for (SqlNode operand : operands) {
            Object value = evaluator.apply(operand);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Object nullif(Row row, SqlBasicCall call) {
        Object left = evaluate(row, call.operand(0));
        Object right = evaluate(row, call.operand(1));
        return Objects.equals(normalizeComparable(left), normalizeComparable(right)) ? null : left;
    }

    private String concat(Row row, SqlBasicCall call) {
        StringBuilder builder = new StringBuilder();
        for (SqlNode operand : call.getOperandList()) {
            Object value = evaluate(row, operand);
            if (value != null) {
                builder.append(value);
            }
        }
        return builder.toString();
    }

    private String concat(List<SqlNode> operands, Function<SqlNode, Object> evaluator) {
        StringBuilder builder = new StringBuilder();
        for (SqlNode operand : operands) {
            Object value = evaluator.apply(operand);
            if (value != null) {
                builder.append(value);
            }
        }
        return builder.toString();
    }

    private Object trim(Row row, SqlBasicCall call) {
        SqlNode operand = call.operand(call.operandCount() - 1);
        Object value = evaluate(row, operand);
        return value == null ? null : value.toString().trim();
    }

    private Object trim(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private Object length(Row row, SqlBasicCall call) {
        Object value = evaluate(row, call.operand(0));
        return value == null ? null : value.toString().length();
    }

    private Object length(Object value) {
        return value == null ? null : value.toString().length();
    }

    private Object substring(Row row, SqlBasicCall call) {
        Object value = evaluate(row, call.operand(0));
        if (value == null) {
            return null;
        }
        String stringValue = value.toString();
        int start = Math.max(0, toInt(evaluate(row, call.operand(1))) - 1);
        if (start >= stringValue.length()) {
            return "";
        }
        if (call.operandCount() < 3) {
            return stringValue.substring(start);
        }
        int end = Math.min(stringValue.length(), start + Math.max(0, toInt(evaluate(row, call.operand(2)))));
        return stringValue.substring(start, end);
    }

    private Object substring(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        Object value = evaluate(frame, call.operand(0), aliasExpressions);
        if (value == null) {
            return null;
        }
        String stringValue = value.toString();
        int start = Math.max(0, toInt(evaluate(frame, call.operand(1), aliasExpressions)) - 1);
        if (start >= stringValue.length()) {
            return "";
        }
        if (call.operandCount() < 3) {
            return stringValue.substring(start);
        }
        int end = Math.min(stringValue.length(), start + Math.max(0, toInt(evaluate(frame, call.operand(2), aliasExpressions))));
        return stringValue.substring(start, end);
    }

    private Object numeric(Row row, SqlBasicCall call, java.util.function.UnaryOperator<BigDecimal> operator) {
        Object value = evaluate(row, call.operand(0));
        return value == null ? null : operator.apply(asBigDecimal(value));
    }

    private Object numeric(Object value, java.util.function.UnaryOperator<BigDecimal> operator) {
        return value == null ? null : operator.apply(asBigDecimal(value));
    }

    private Object round(Row row, SqlBasicCall call) {
        Object value = evaluate(row, call.operand(0));
        if (value == null) {
            return null;
        }
        int scale = call.operandCount() > 1 ? toInt(evaluate(row, call.operand(1))) : 0;
        return asBigDecimal(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private Object round(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        Object value = evaluate(frame, call.operand(0), aliasExpressions);
        if (value == null) {
            return null;
        }
        int scale = call.operandCount() > 1 ? toInt(evaluate(frame, call.operand(1), aliasExpressions)) : 0;
        return asBigDecimal(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private Object extract(Row row, SqlBasicCall call) {
        String unit = call.operand(0).toString().toUpperCase(Locale.ROOT);
        LocalDate date = toLocalDate(evaluate(row, call.operand(1)));
        if (unit.contains("YEAR")) {
            return date.getYear();
        }
        if (unit.contains("MONTH")) {
            return date.getMonthValue();
        }
        if (unit.contains("DAY")) {
            return date.getDayOfMonth();
        }
        throw new UnsupportedOperationException("Unsupported EXTRACT unit in current V2 skeleton: " + unit);
    }

    private Object extract(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        String unit = call.operand(0).toString().toUpperCase(Locale.ROOT);
        LocalDate date = toLocalDate(evaluate(frame, call.operand(1), aliasExpressions));
        if (unit.contains("YEAR")) {
            return date.getYear();
        }
        if (unit.contains("MONTH")) {
            return date.getMonthValue();
        }
        if (unit.contains("DAY")) {
            return date.getDayOfMonth();
        }
        throw new UnsupportedOperationException("Unsupported EXTRACT unit in current V2 skeleton: " + unit);
    }

    private Object evaluateRegisteredFunction(Row row, SqlBasicCall call, String functionName) {
        TemplateV2SqlFunction function = sqlFunctionRegistry.find(functionName)
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported SQL operator in current V2 skeleton: "
                        + call.getKind() + " / " + functionName));
        List<Object> arguments = call.getOperandList().stream()
                .map(operand -> evaluate(row, operand))
                .toList();
        return function.evaluator().evaluate(new TemplateV2SqlFunctionContext(arguments));
    }

    private Object evaluateRegisteredFunction(AggregateFrame frame,
                                              SqlBasicCall call,
                                              String functionName,
                                              Map<String, SqlNode> aliasExpressions) {
        TemplateV2SqlFunction function = sqlFunctionRegistry.find(functionName)
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported SQL operator in current V2 skeleton: "
                        + call.getKind() + " / " + functionName));
        List<Object> arguments = call.getOperandList().stream()
                .map(operand -> evaluate(frame, operand, aliasExpressions))
                .toList();
        return function.evaluator().evaluate(new TemplateV2SqlFunctionContext(arguments));
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

    private Object evaluateCase(AggregateFrame frame, SqlCase sqlCase, Map<String, SqlNode> aliasExpressions) {
        SqlNodeList whenOperands = sqlCase.getWhenOperands();
        SqlNodeList thenOperands = sqlCase.getThenOperands();
        for (int i = 0; i < whenOperands.size(); i++) {
            Object condition = evaluate(frame, whenOperands.get(i), aliasExpressions);
            if (toBoolean(condition)) {
                return evaluate(frame, thenOperands.get(i), aliasExpressions);
            }
        }
        SqlNode elseOperand = sqlCase.getElseOperand();
        return elseOperand == null ? null : evaluate(frame, elseOperand, aliasExpressions);
    }

    private Object resolveIdentifier(Row row, SqlIdentifier identifier) {
        if (identifier.names.isEmpty()) {
            return null;
        }
        if (identifier.names.size() == 1) {
            return row.get(simpleName(identifier).toLowerCase(Locale.ROOT));
        }
        String qualified = String.join(".", identifier.names).toLowerCase(Locale.ROOT);
        return row.get(qualified);
    }

    private String simpleName(SqlIdentifier identifier) {
        return identifier.names.getLast();
    }

    private int compare(Row row, SqlBasicCall call) {
        Object left = evaluate(row, call.operand(0));
        Object right = evaluate(row, call.operand(1));
        return compareValues(left, right, false);
    }

    private int compare(AggregateFrame frame, SqlBasicCall call, Map<String, SqlNode> aliasExpressions) {
        Object left = evaluate(frame, call.operand(0), aliasExpressions);
        Object right = evaluate(frame, call.operand(1), aliasExpressions);
        return compareValues(left, right, false);
    }

    private int compareValues(Object left, Object right, boolean nullsLast) {
        if (left == null || right == null) {
            if (left == null && right == null) {
                return 0;
            }
            return left == null
                    ? (nullsLast ? 1 : -1)
                    : (nullsLast ? -1 : 1);
        }
        if (isTemporal(left) || isTemporal(right)) {
            return toLocalDateTime(left).compareTo(toLocalDateTime(right));
        }
        if (left instanceof Number || right instanceof Number) {
            return asBigDecimal(left).compareTo(asBigDecimal(right));
        }
        if (left instanceof CharSequence || right instanceof CharSequence) {
            return String.valueOf(left).compareTo(String.valueOf(right));
        }
        if (left instanceof Boolean leftBoolean && right instanceof Boolean rightBoolean) {
            return Boolean.compare(leftBoolean, rightBoolean);
        }
        if (left instanceof Comparable<?> leftComparable
                && right instanceof Comparable<?> rightComparable
                && leftComparable.getClass().isInstance(rightComparable)) {
            @SuppressWarnings("unchecked")
            Comparable<Object> comparable = (Comparable<Object>) leftComparable;
            return comparable.compareTo(rightComparable);
        }
        return asBigDecimal(left).compareTo(asBigDecimal(right));
    }

    private Object normalizeComparable(Object value) {
        if (isTemporal(value)) {
            return toLocalDateTime(value);
        }
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

    private boolean isTemporal(Object value) {
        if (value instanceof LocalDateTime || value instanceof LocalDate || value instanceof Date) {
            return true;
        }
        String className = value == null ? "" : value.getClass().getName();
        return className.equals("org.apache.calcite.util.TimestampString")
                || className.equals("org.apache.calcite.util.DateString");
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().atStartOfDay();
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (value instanceof CharSequence text) {
            String normalized = text.toString().trim().replace(' ', 'T');
            if (normalized.length() == 10) {
                return LocalDate.parse(normalized).atStartOfDay();
            }
            return LocalDateTime.parse(normalized);
        }
        if (value != null) {
            String className = value.getClass().getName();
            if (className.equals("org.apache.calcite.util.TimestampString")
                    || className.equals("org.apache.calcite.util.DateString")) {
                return toLocalDateTime(value.toString());
            }
        }
        throw new IllegalArgumentException("Expected datetime value but got: " + value);
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (value instanceof String stringValue) {
            return LocalDate.parse(stringValue);
        }
        throw new IllegalArgumentException("Expected date value but got: " + value);
    }

    private int toInt(Object value) {
        return asBigDecimal(value).intValue();
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value instanceof CharSequence text) {
            return Boolean.parseBoolean(text.toString());
        }
        return value != null;
    }

    private Object upper(Object value) {
        return value == null ? null : value.toString().toUpperCase(Locale.ROOT);
    }

    private Object lower(Object value) {
        return value == null ? null : value.toString().toLowerCase(Locale.ROOT);
    }

    private String likePatternToRegex(String pattern, Character escape) {
        StringBuilder regex = new StringBuilder("^");
        boolean escaping = false;
        for (int i = 0; i < pattern.length(); i++) {
            char current = pattern.charAt(i);
            if (escape != null && current == escape && !escaping) {
                escaping = true;
                continue;
            }
            if (!escaping) {
                if (current == '%') {
                    regex.append(".*");
                    continue;
                }
                if (current == '_') {
                    regex.append('.');
                    continue;
                }
            }
            appendRegexLiteral(regex, current);
            escaping = false;
        }
        regex.append('$');
        return regex.toString();
    }

    private void appendRegexLiteral(StringBuilder builder, char current) {
        if ("\\.^$|?*+()[]{}".indexOf(current) >= 0) {
            builder.append('\\');
        }
        builder.append(current);
    }

    public record TransformResult(RowSchema schema, List<Row> rows) {
    }

    private record AggregateFrame(List<Row> rows, Row representative) {
    }

    private record GroupKey(List<Object> values) {
    }

    private record OrderSpec(SqlNode expression, boolean ascending, boolean nullsLast) {
        private OrderSpec withDescending() {
            return new OrderSpec(expression, false, nullsLast);
        }

        private OrderSpec withNullsFirst() {
            return new OrderSpec(expression, ascending, false);
        }

        private OrderSpec withNullsLast() {
            return new OrderSpec(expression, ascending, true);
        }
    }
}
