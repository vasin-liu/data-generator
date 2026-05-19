/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sql;

import org.apache.calcite.avatica.util.Casing;
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
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;

import java.util.regex.Pattern;

/**
 * Inspects parsed Calcite SQL AST to determine the execution shape for Template V2 transforms.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class ExecutionShapeClassifier {

    private static final SqlConformance SQL_CONFORMANCE = SqlConformanceEnum.BABEL;
    private static final Pattern COUNT_STAR = Pattern.compile("(?i)\\bcount\\s*\\(\\s*\\*\\s*\\)");

    private ExecutionShapeClassifier() {
    }

    /**
     * Classifies execution shape from a SQL SELECT statement.
     *
     * @param sql SQL text (typically the first transform's query)
     * @return classified shape
     * @throws IllegalArgumentException if SQL cannot be parsed
     */
    public static ExecutionShape classify(String sql) {
        SqlNode parsed = parseQuery(sql);
        SqlNodeList orderBy = null;
        SqlNode fetch = null;
        SqlNode query = parsed;

        if (parsed instanceof SqlOrderBy orderByNode) {
            orderBy = orderByNode.orderList;
            fetch = orderByNode.fetch;
            query = orderByNode.query;
        }

        if (!(query instanceof SqlSelect select)) {
            return ExecutionShape.MATERIALIZATION_REQUIRED;
        }

        if (hasGroupBy(select)) {
            return ExecutionShape.MATERIALIZATION_REQUIRED;
        }
        if (select.isDistinct()) {
            return ExecutionShape.MATERIALIZATION_REQUIRED;
        }
        if (hasOrderByWithoutLimit(orderBy, fetch)) {
            return ExecutionShape.MATERIALIZATION_REQUIRED;
        }
        if (containsJoin(select.getFrom())) {
            return ExecutionShape.MATERIALIZATION_REQUIRED;
        }
        if (isSimpleTableFrom(select.getFrom())) {
            return ExecutionShape.ROW_LOCAL;
        }
        return ExecutionShape.MATERIALIZATION_REQUIRED;
    }

    /**
     * Classifies execution shape from the first SQL transform on a template.
     *
     * @param template Template V2 definition
     * @return classified shape
     * @throws IllegalArgumentException if the template has no SQL transform or SQL is blank
     */
    public static ExecutionShape classify(TemplateV2VO template) {
        return classify(requireFirstSql(template));
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
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL must not be blank");
        }
        try {
            SqlParser parser = SqlParser.create(normalizeSql(sql), parserConfig());
            return parser.parseQuery();
        } catch (SqlParseException e) {
            throw new IllegalArgumentException("Failed to parse SQL: " + e.getMessage(), e);
        }
    }

    private static String normalizeSql(String sql) {
        return COUNT_STAR.matcher(sql).replaceAll("COUNT(1)");
    }

    private static SqlParser.Config parserConfig() {
        return SqlParser.config()
                .withParserFactory(SqlBabelParserImpl.FACTORY)
                .withQuotedCasing(Casing.UNCHANGED)
                .withUnquotedCasing(Casing.UNCHANGED)
                .withCaseSensitive(false)
                .withConformance(SQL_CONFORMANCE);
    }

    private static boolean hasGroupBy(SqlSelect select) {
        SqlNodeList groupBy = select.getGroup();
        return groupBy != null && !groupBy.isEmpty();
    }

    private static boolean hasOrderByWithoutLimit(SqlNodeList orderBy, SqlNode fetch) {
        return orderBy != null && !orderBy.isEmpty() && fetch == null;
    }

    private static boolean containsJoin(SqlNode from) {
        if (from == null) {
            return false;
        }
        if (from instanceof SqlJoin) {
            return true;
        }
        if (from instanceof SqlBasicCall call && call.getKind() == SqlKind.AS) {
            return containsJoin(call.operand(0));
        }
        return false;
    }

    private static boolean isSimpleTableFrom(SqlNode from) {
        if (from instanceof SqlIdentifier) {
            return true;
        }
        if (from instanceof SqlBasicCall call && call.getKind() == SqlKind.AS && call.operand(0) instanceof SqlIdentifier) {
            return true;
        }
        return false;
    }
}
