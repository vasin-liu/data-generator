/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.join;

import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.babel.SqlBabelParserImpl;
import org.apache.calcite.sql.validate.SqlConformance;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.gensokyo.data.calcite.runtime.EffectiveExecutionPolicy;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parses a single equi-join SQL transform into a {@link BroadcastJoinSpec} for chunked broadcast execution.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class BroadcastJoinSqlParser {

    private static final SqlConformance SQL_CONFORMANCE = SqlConformanceEnum.BABEL;
    private static final Pattern COUNT_STAR = Pattern.compile("(?i)\\bcount\\s*\\(\\s*\\*\\s*\\)");

    private BroadcastJoinSqlParser() {
    }

    /**
     * Parses the first SQL transform on a two-source template into a broadcast join plan.
     *
     * @param template template with fact + dimension sources and one join SQL transform
     * @return parsed join specification
     * @throws IllegalArgumentException if SQL shape is not a supported broadcast join
     */
    public static BroadcastJoinSpec parse(TemplateV2VO template) {
        String sql = requireFirstSql(template);
        SqlSelect select = unwrapSelect(parseQuery(sql));
        SqlJoin join = requireJoin(select.getFrom());
        if (join.getJoinType() != JoinType.INNER && join.getJoinType() != JoinType.LEFT) {
            throw new IllegalArgumentException("Broadcast join supports only INNER or LEFT JOIN");
        }
        if (select.getWhere() != null) {
            throw new IllegalArgumentException("Broadcast join v1 does not support WHERE on the transform SQL");
        }

        String leftSource = tableNameFromFromNode(join.getLeft());
        String rightSource = tableNameFromFromNode(join.getRight());
        String leftAlias = aliasFromFromNode(join.getLeft());
        String rightAlias = aliasFromFromNode(join.getRight());
        if (leftSource == null || rightSource == null) {
            throw new IllegalArgumentException("Broadcast join requires simple table references in FROM");
        }

        Map<String, SourceVO> sources = template.getSources();
        if (sources == null || sources.size() != 2) {
            throw new IllegalArgumentException("Broadcast join requires exactly two template sources");
        }

        int broadcastMaxRows = EffectiveExecutionPolicy.resolve(template.getExecutionPolicy()).broadcastMaxRows();
        String factSource = resolveFactSource(sources, broadcastMaxRows);
        String dimSource = resolveDimSource(sources, broadcastMaxRows);
        if (!sources.containsKey(leftSource) || !sources.containsKey(rightSource)) {
            throw new IllegalArgumentException("JOIN tables must match template source names");
        }

        String factAlias = factSource.equals(leftSource) ? leftAlias : rightAlias;
        String dimAlias = dimSource.equals(leftSource) ? leftAlias : rightAlias;
        EquiKey equiKey = parseEquiKey(join.getCondition(), leftAlias, rightAlias);
        String factJoinColumn = columnForAlias(equiKey, factAlias);
        String dimJoinColumn = columnForAlias(equiKey, dimAlias);

        List<BroadcastJoinSpec.OutputColumn> outputColumns = parseProjections(
                select.getSelectList(), leftAlias, rightAlias, leftSource, rightSource, factSource, dimSource);
        RowSchema outputSchema = new RowSchema();
        List<ColumnDef> columns = new ArrayList<>();
        for (BroadcastJoinSpec.OutputColumn column : outputColumns) {
            columns.add(new ColumnDef(column.name(), "ANY", true));
        }
        outputSchema.setColumns(columns);

        return new BroadcastJoinSpec(
                join.getJoinType(),
                factSource,
                dimSource,
                factJoinColumn,
                dimJoinColumn,
                outputColumns,
                outputSchema);
    }

    private static String resolveFactSource(Map<String, SourceVO> sources, int broadcastMaxRows) {
        String fact = null;
        for (Map.Entry<String, SourceVO> entry : sources.entrySet()) {
            if (entry.getValue() instanceof QuerySourceVO query && !isBroadcastQuery(query, broadcastMaxRows)) {
                if (fact != null) {
                    throw new IllegalArgumentException("Broadcast join requires exactly one fact QuerySource");
                }
                fact = entry.getKey();
            }
        }
        if (fact == null) {
            throw new IllegalArgumentException("Broadcast join requires one fact QuerySource without low maxRows");
        }
        return fact;
    }

    private static String resolveDimSource(Map<String, SourceVO> sources, int broadcastMaxRows) {
        String dim = null;
        for (Map.Entry<String, SourceVO> entry : sources.entrySet()) {
            if (entry.getValue() instanceof QuerySourceVO query && isBroadcastQuery(query, broadcastMaxRows)) {
                if (dim != null) {
                    throw new IllegalArgumentException("Broadcast join requires exactly one broadcast QuerySource");
                }
                dim = entry.getKey();
            }
        }
        if (dim == null) {
            throw new IllegalArgumentException("Broadcast join requires one dimension QuerySource with maxRows");
        }
        return dim;
    }

    private static boolean isBroadcastQuery(QuerySourceVO query, int broadcastMaxRows) {
        Long maxRows = query.getMaxRows();
        return maxRows != null && maxRows > 0 && maxRows <= broadcastMaxRows;
    }

    private static List<BroadcastJoinSpec.OutputColumn> parseProjections(
            SqlNodeList selectList,
            String leftAlias,
            String rightAlias,
            String leftSource,
            String rightSource,
            String factSource,
            String dimSource) {
        List<BroadcastJoinSpec.OutputColumn> columns = new ArrayList<>();
        for (SqlNode node : selectList) {
            SqlNode expression = expression(node);
            if (!(expression instanceof SqlIdentifier identifier)) {
                throw new IllegalArgumentException(
                        "Broadcast join v1 supports only simple column projections in SELECT");
            }
            String outputName = columnName(node);
            QualifiedColumn qualified = qualifiedColumn(identifier);
            BroadcastJoinSpec.ProjectionSide side;
            if (matchesSide(qualified, leftAlias, leftSource, factSource)) {
                side = BroadcastJoinSpec.ProjectionSide.FACT;
            } else if (matchesSide(qualified, rightAlias, rightSource, factSource)) {
                side = BroadcastJoinSpec.ProjectionSide.FACT;
            } else if (matchesSide(qualified, leftAlias, leftSource, dimSource)) {
                side = BroadcastJoinSpec.ProjectionSide.DIM;
            } else if (matchesSide(qualified, rightAlias, rightSource, dimSource)) {
                side = BroadcastJoinSpec.ProjectionSide.DIM;
            } else {
                throw new IllegalArgumentException("SELECT column [" + identifier + "] is not from a join source");
            }
            columns.add(new BroadcastJoinSpec.OutputColumn(outputName, side, qualified.column()));
        }
        return columns;
    }

    private static boolean matchesSide(
            QualifiedColumn qualified, String alias, String sourceName, String expectedSource) {
        if (!sourceName.equals(expectedSource)) {
            return false;
        }
        if (qualified.hasQualifier()) {
            return alias.equalsIgnoreCase(qualified.qualifier());
        }
        return true;
    }

    private static EquiKey parseEquiKey(SqlNode condition, String leftAlias, String rightAlias) {
        List<EquiKey> keys = new ArrayList<>();
        collectEquiKeys(condition, keys);
        if (keys.size() != 1) {
            throw new IllegalArgumentException("Broadcast join v1 requires a single equi-join predicate");
        }
        EquiKey key = keys.getFirst();
        if (!aliasesMatch(key, leftAlias, rightAlias)) {
            throw new IllegalArgumentException("JOIN ON must reference both join aliases");
        }
        return key;
    }

    private static void collectEquiKeys(SqlNode condition, List<EquiKey> keys) {
        if (condition == null) {
            throw new IllegalArgumentException("JOIN ON condition is required");
        }
        if (condition instanceof SqlBasicCall call && call.getKind() == SqlKind.AND) {
            for (SqlNode operand : call.getOperandList()) {
                collectEquiKeys(operand, keys);
            }
            return;
        }
        if (condition instanceof SqlBasicCall call && call.getKind() == SqlKind.EQUALS) {
            QualifiedColumn left = qualifiedColumn(call.operand(0));
            QualifiedColumn right = qualifiedColumn(call.operand(1));
            keys.add(new EquiKey(left, right));
            return;
        }
        throw new IllegalArgumentException("Broadcast join v1 supports only equi-join ON predicates");
    }

    private static boolean aliasesMatch(EquiKey key, String leftAlias, String rightAlias) {
        boolean leftOnLeft = key.left().qualifier().equalsIgnoreCase(leftAlias);
        boolean rightOnRight = key.right().qualifier().equalsIgnoreCase(rightAlias);
        boolean leftOnRight = key.left().qualifier().equalsIgnoreCase(rightAlias);
        boolean rightOnLeft = key.right().qualifier().equalsIgnoreCase(leftAlias);
        return (leftOnLeft && rightOnRight) || (leftOnRight && rightOnLeft);
    }

    private static QualifiedColumn qualifiedColumn(SqlNode node) {
        if (!(node instanceof SqlIdentifier identifier)) {
            throw new IllegalArgumentException("JOIN keys must be column references");
        }
        if (identifier.names.isEmpty()) {
            throw new IllegalArgumentException("Invalid column reference in JOIN");
        }
        if (identifier.names.size() == 1) {
            return new QualifiedColumn(null, simpleName(identifier));
        }
        if (identifier.names.size() == 2) {
            return new QualifiedColumn(identifier.names.get(0), simpleName(identifier));
        }
        throw new IllegalArgumentException("Qualified column depth not supported: " + identifier);
    }

    private static String columnForAlias(EquiKey key, String alias) {
        if (key.left().qualifier() != null && key.left().qualifier().equalsIgnoreCase(alias)) {
            return key.left().column().toLowerCase(Locale.ROOT);
        }
        if (key.right().qualifier() != null && key.right().qualifier().equalsIgnoreCase(alias)) {
            return key.right().column().toLowerCase(Locale.ROOT);
        }
        throw new IllegalArgumentException("JOIN ON must reference alias [" + alias + "]");
    }

    private record EquiKey(QualifiedColumn left, QualifiedColumn right) {
    }

    private record QualifiedColumn(String qualifier, String column) {
        boolean hasQualifier() {
            return qualifier != null && !qualifier.isBlank();
        }
    }

    private static SqlSelect unwrapSelect(SqlNode parsed) {
        SqlNode query = parsed;
        if (parsed instanceof SqlOrderBy orderBy) {
            query = orderBy.query;
        }
        if (!(query instanceof SqlSelect select)) {
            throw new IllegalArgumentException("Broadcast join SQL must be a SELECT");
        }
        return select;
    }

    private static SqlJoin requireJoin(SqlNode from) {
        if (!(from instanceof SqlJoin join)) {
            throw new IllegalArgumentException("Broadcast join SQL must contain a single JOIN in FROM");
        }
        return join;
    }

    private static String tableNameFromFromNode(SqlNode from) {
        if (from instanceof SqlIdentifier identifier) {
            return identifier.getSimple();
        }
        if (from instanceof SqlBasicCall call && call.getKind() == SqlKind.AS
                && call.operand(0) instanceof SqlIdentifier identifier) {
            return identifier.getSimple();
        }
        return null;
    }

    private static String aliasFromFromNode(SqlNode from) {
        if (from instanceof SqlBasicCall call && call.getKind() == SqlKind.AS
                && call.operand(1) instanceof SqlIdentifier alias) {
            return alias.getSimple();
        }
        String table = tableNameFromFromNode(from);
        return table;
    }

    private static String columnName(SqlNode node) {
        if (node.getKind() == SqlKind.AS && node instanceof SqlBasicCall call && call.operand(1) instanceof SqlIdentifier alias) {
            return alias.getSimple();
        }
        if (node instanceof SqlIdentifier identifier) {
            return simpleName(identifier);
        }
        return node.toString();
    }

    private static SqlNode expression(SqlNode node) {
        if (node.getKind() == SqlKind.AS && node instanceof SqlBasicCall call) {
            return call.operand(0);
        }
        return node;
    }

    private static String simpleName(SqlIdentifier identifier) {
        return identifier.names.getLast();
    }

    private static String requireFirstSql(TemplateV2VO template) {
        if (template == null || template.getTransformers() == null) {
            throw new IllegalArgumentException("Template has no SQL transform");
        }
        for (TransformVO transformer : template.getTransformers()) {
            if (transformer instanceof SqlTransformVO sqlTransform) {
                String sql = sqlTransform.getSql();
                if (sql == null || sql.isBlank()) {
                    throw new IllegalArgumentException("Template SQL transform has no SQL");
                }
                return sql;
            }
        }
        throw new IllegalArgumentException("Template has no SQL transform");
    }

    private static SqlNode parseQuery(String sql) {
        try {
            SqlParser parser = SqlParser.create(normalizeSql(sql), SqlParser.config()
                    .withParserFactory(SqlBabelParserImpl.FACTORY)
                    .withQuotedCasing(Casing.UNCHANGED)
                    .withUnquotedCasing(Casing.UNCHANGED)
                    .withCaseSensitive(false)
                    .withConformance(SQL_CONFORMANCE));
            return parser.parseQuery();
        } catch (SqlParseException e) {
            throw new IllegalArgumentException("Failed to parse SQL: " + e.getMessage(), e);
        }
    }

    private static String normalizeSql(String sql) {
        return COUNT_STAR.matcher(sql).replaceAll("COUNT(1)");
    }
}
