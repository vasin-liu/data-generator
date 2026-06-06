/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.calcite.runtime.EffectiveExecutionPolicy;
import org.gensokyo.data.calcite.sql.CalciteExecutionContext;
import org.gensokyo.data.calcite.sql.CalciteSqlValidationResult;
import org.gensokyo.data.calcite.sql.CalciteSqlValidator;
import org.gensokyo.data.calcite.sql.ExecutionShape;
import org.gensokyo.data.calcite.sql.ExecutionShapeClassifier;
import org.gensokyo.data.iterator.DatabaseIteratorVO;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.GeoJsonSourceVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.PostGisQuerySourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.iterator.IteratorVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Builds a bounded V2 Calcite plan summary for control-plane explain responses.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public class TemplateV2PlanExplainService {

    private static final int SQL_SNIPPET_MAX = 500;
    private static final Pattern WORD_GROUP_BY = Pattern.compile("(?i)\\bgroup\\s+by\\b");
    private static final Pattern WORD_JOIN = Pattern.compile("(?i)\\bjoin\\b");
    private static final Pattern WORD_DISTINCT = Pattern.compile("(?i)\\bselect\\s+distinct\\b");
    private static final Pattern WORD_ORDER_BY = Pattern.compile("(?i)\\border\\s+by\\b");
    private static final Pattern WORD_LIMIT = Pattern.compile("(?i)\\b(limit|fetch)\\b");

    private final CalciteSqlValidator sqlValidator = new CalciteSqlValidator();

    /**
     * Explains the normalized V2 template for operators.
     *
     * @param v1Probe optional V1-shaped YAML probe for legacy diff hints; may be {@code null}
     * @param v2      normalized V2 template
     * @return plan explain block; never {@code null}
     */
    public TemplateV2PlanExplain explain(TemplateVO v1Probe, TemplateV2VO v2) {
        TemplateV2PlanExplain explain = new TemplateV2PlanExplain();
        Objects.requireNonNull(v2, "v2");

        appendLegacyHints(explain, v1Probe);
        appendSourceSummaries(explain, v2);

        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(v2.getExecutionPolicy());
        explain.setEffectiveExecutionMode(policy.mode());

        String sql = firstSql(v2);
        if (sql == null) {
            explain.setCalciteValidation("No SQL transform on V2 template");
            explain.getDiffNotes().add("V2 has no SQL transform — preview used pipeline output only");
            return explain;
        }

        explain.setV2Sql(truncate(sql));
        validateAndClassify(explain, v2, sql, policy);
        explain.getPlanFeatures().addAll(describePlanFeatures(sql));
        appendDiffNotes(explain, v1Probe, sql);
        return explain;
    }

    private void validateAndClassify(
            TemplateV2PlanExplain explain,
            TemplateV2VO v2,
            String sql,
            EffectiveExecutionPolicy policy) {
        CalciteExecutionContext context = buildValidationContext(v2);
        CalciteSqlValidationResult validation = sqlValidator.validate(sql, context);
        if (validation.isValid()) {
            explain.setCalciteValidation("Calcite parse/validate OK");
        }
        else {
            explain.setCalciteValidation("Calcite validation failed: " + validation.getMessage());
        }

        try {
            ExecutionShape shape = ExecutionShapeClassifier.classify(v2);
            explain.setExecutionShape(shape.name());
            if ("CHUNKED".equalsIgnoreCase(policy.mode()) && shape == ExecutionShape.MATERIALIZATION_REQUIRED) {
                explain.getDiffNotes().add(
                        "CHUNKED mode requested but SQL shape is MATERIALIZATION_REQUIRED — runtime may fall back or approximate");
            }
            if ("CHUNKED".equalsIgnoreCase(policy.mode()) && shape == ExecutionShape.ROW_LOCAL) {
                explain.getDiffNotes().add("CHUNKED row-local shape — suitable for large JDBC export path");
            }
            if (shape == ExecutionShape.BROADCAST_JOIN) {
                explain.getDiffNotes().add("Broadcast join shape — verify dimension source maxRows/broadcastMaxRows");
            }
        }
        catch (RuntimeException e) {
            explain.setExecutionShape("UNKNOWN");
            explain.getDiffNotes().add("Execution shape classification failed: " + e.getMessage());
        }
    }

    private static void appendLegacyHints(TemplateV2PlanExplain explain, TemplateVO v1Probe) {
        if (v1Probe == null) {
            return;
        }
        IteratorVO iterator = v1Probe.getIterator();
        if (iterator instanceof DatabaseIteratorVO database) {
            if (database.getDataSourceId() != null) {
                explain.getV1Hints().add("Legacy JDBC datasource: " + database.getDataSourceId());
            }
            if (database.getSql() != null && !database.getSql().isBlank()) {
                explain.getV1Hints().add("Legacy JDBC SQL: " + truncate(database.getSql()));
            }
        }
        else if (iterator != null && iterator.getType() != null) {
            explain.getV1Hints().add("Legacy iterator type: " + iterator.getType());
        }
    }

    private static void appendSourceSummaries(TemplateV2PlanExplain explain, TemplateV2VO v2) {
        if (v2.getSources() == null) {
            return;
        }
        for (Map.Entry<String, SourceVO> entry : v2.getSources().entrySet()) {
            SourceVO source = entry.getValue();
            String kind = source == null ? "unknown" : source.getClass().getSimpleName();
            if (source instanceof QuerySourceVO query) {
                String ds = query.getDataSourceId() != null ? query.getDataSourceId() : "?";
                explain.getSourceSummaries().add(
                        entry.getKey() + ": QuerySourceVO ds=" + ds
                                + (query.getMaxRows() != null ? " maxRows=" + query.getMaxRows() : ""));
            }
            else if (source instanceof GeoJsonSourceVO geoJson) {
                explain.getSourceSummaries().add(entry.getKey() + ": GeoJsonSourceVO path=" + geoJson.getPath());
            }
            else if (source instanceof PostGisQuerySourceVO postGis) {
                explain.getSourceSummaries().add(entry.getKey() + ": PostGisQuerySourceVO table=" + postGis.getTable()
                        + " geom=" + postGis.getGeometryColumn());
            }
            else if (source instanceof IteratorSourceVO iteratorSource) {
                String iteratorType = iteratorSource.getIterator() != null
                        ? iteratorSource.getIterator().getType()
                        : "?";
                explain.getSourceSummaries().add(entry.getKey() + ": IteratorSourceVO type=" + iteratorType);
            }
            else {
                explain.getSourceSummaries().add(entry.getKey() + ": " + kind);
            }
        }
    }

    private static void appendDiffNotes(TemplateV2PlanExplain explain, TemplateVO v1Probe, String v2Sql) {
        String v1Sql = extractV1JdbcSql(v1Probe);
        if (v1Sql != null && !normalizeForCompare(v1Sql).equals(normalizeForCompare(v2Sql))) {
            explain.getDiffNotes().add("Legacy JDBC SQL text differs from V2 transform SQL — review join/projection rewrite");
        }
    }

    private static String extractV1JdbcSql(TemplateVO v1) {
        if (v1 == null || v1.getIterator() == null) {
            return null;
        }
        if (v1.getIterator() instanceof DatabaseIteratorVO database) {
            return database.getSql();
        }
        return null;
    }

    private static String normalizeForCompare(String sql) {
        return sql == null ? "" : sql.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static CalciteExecutionContext buildValidationContext(TemplateV2VO v2) {
        CalciteExecutionContext context = new CalciteExecutionContext();
        if (v2.getSources() == null) {
            return context;
        }
        RowSchema placeholder = placeholderSchema();
        for (String name : v2.getSources().keySet()) {
            context.addSchema(name, placeholder);
        }
        return context;
    }

    private static RowSchema placeholderSchema() {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(
                new ColumnDef("id", "ANY", true),
                new ColumnDef("name", "ANY", true),
                new ColumnDef("value", "ANY", true)));
        return schema;
    }

    private static String firstSql(TemplateV2VO v2) {
        if (v2.getTransformers() == null) {
            return null;
        }
        for (TransformVO transformer : v2.getTransformers()) {
            if (transformer instanceof SqlTransformVO sqlTransform) {
                return sqlTransform.getSql();
            }
        }
        return null;
    }

    private static List<String> describePlanFeatures(String sql) {
        List<String> features = new ArrayList<>();
        if (WORD_GROUP_BY.matcher(sql).find()) {
            features.add("GROUP BY");
        }
        if (WORD_DISTINCT.matcher(sql).find()) {
            features.add("DISTINCT");
        }
        if (WORD_JOIN.matcher(sql).find()) {
            features.add("JOIN");
        }
        if (WORD_ORDER_BY.matcher(sql).find()) {
            features.add("ORDER BY");
        }
        if (WORD_LIMIT.matcher(sql).find()) {
            features.add("LIMIT/FETCH");
        }
        if (sql.toLowerCase(Locale.ROOT).contains(" where ")) {
            features.add("WHERE");
        }
        return features;
    }

    private static String truncate(String sql) {
        if (sql == null) {
            return null;
        }
        String trimmed = sql.strip();
        if (trimmed.length() <= SQL_SNIPPET_MAX) {
            return trimmed;
        }
        return trimmed.substring(0, SQL_SNIPPET_MAX) + "...";
    }
}
