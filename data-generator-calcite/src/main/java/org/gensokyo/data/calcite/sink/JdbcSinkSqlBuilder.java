/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds JDBC insert/upsert SQL for V2 sink writers using writer {@code options}.
 *
 * @author Gensokyo
 * @since 2026-06-10
 */
final class JdbcSinkSqlBuilder {

    private JdbcSinkSqlBuilder() {
    }

    /**
     * Builds the SQL statement for a JDBC sink write.
     *
     * @param writer        JDBC writer configuration
     * @param targetColumns target table column names in insert order
     * @return SQL text for {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate#batchUpdate}
     */
    static String buildSql(JdbcWriterVO writer, List<String> targetColumns) {
        String table = Objects.requireNonNull(writer.getTarget(), "JDBC sink target must not be null");
        String columns = String.join(", ", targetColumns);
        String values = targetColumns.stream()
                .map(column -> ":" + column)
                .collect(Collectors.joining(", "));
        String baseInsert = "insert into " + table + " (" + columns + ") values (" + values + ")";
        if (!WriterOptionResolver.booleanOption(writer, "upsert")) {
            return baseInsert;
        }
        return appendUpsertClause(writer, table, columns, values, baseInsert, targetColumns);
    }

    /**
     * Validates {@code upsertKeys} against known target columns when upsert is enabled.
     *
     * @param writer        JDBC writer configuration
     * @param targetColumns target table column names in insert order
     */
    static void validateUpsertKeys(JdbcWriterVO writer, List<String> targetColumns) {
        if (!WriterOptionResolver.booleanOption(writer, "upsert")) {
            return;
        }
        requireUpsertKeys(writer, targetColumns);
    }

    private static String appendUpsertClause(
            JdbcWriterVO writer,
            String table,
            String columns,
            String values,
            String baseInsert,
            List<String> targetColumns) {
        String dialect = resolveDialect(writer);
        return switch (dialect) {
            case "postgres", "postgresql" -> appendPostgresUpsert(writer, baseInsert, targetColumns);
            case "kingbase", "highgo" -> appendPostgresUpsert(writer, baseInsert, targetColumns);
            case "mysql" -> appendMysqlUpsert(writer, table, columns, values, targetColumns);
            case "dameng" -> appendDamengMerge(writer, table, targetColumns);
            case "clickhouse", "click_house" -> throw unsupportedUpsertDialect("clickhouse");
            case "generic" -> throw unsupportedUpsertDialect("generic");
            default -> throw unsupportedUpsertDialect(dialect);
        };
    }

    private static IllegalArgumentException unsupportedUpsertDialect(String dialect) {
        return new IllegalArgumentException(
                "JDBC sink upsert=true is not supported for dialect " + dialect);
    }

    private static String appendPostgresUpsert(
            JdbcWriterVO writer,
            String baseInsert,
            List<String> targetColumns) {
        List<String> upsertKeys = requireUpsertKeys(writer, targetColumns);
        String conflict = String.join(", ", upsertKeys);
        List<String> updateColumns = targetColumns.stream()
                .filter(column -> !upsertKeys.contains(column))
                .toList();
        if (updateColumns.isEmpty()) {
            return baseInsert + " on conflict (" + conflict + ") do nothing";
        }
        String updates = updateColumns.stream()
                .map(column -> column + " = excluded." + column)
                .collect(Collectors.joining(", "));
        return baseInsert + " on conflict (" + conflict + ") do update set " + updates;
    }

    /**
     * Builds MySQL {@code INSERT ... ON DUPLICATE KEY UPDATE} SQL.
     * Legacy {@code options.conflictColumns} is not honored for MySQL — use {@code upsertKeys}.
     */
    private static String appendMysqlUpsert(
            JdbcWriterVO writer,
            String table,
            String columns,
            String values,
            List<String> targetColumns) {
        List<String> upsertKeys = requireUpsertKeys(writer, targetColumns);
        String baseInsert = "insert into " + table + " (" + columns + ") values (" + values + ")";
        List<String> updateColumns = targetColumns.stream()
                .filter(column -> !upsertKeys.contains(column))
                .toList();
        if (updateColumns.isEmpty()) {
            return baseInsert;
        }
        String updates = updateColumns.stream()
                .map(column -> column + " = values(" + column + ")")
                .collect(Collectors.joining(", "));
        return baseInsert + " on duplicate key update " + updates;
    }

    /**
     * Builds Dameng {@code MERGE INTO} upsert SQL using {@code options.upsertKeys} for the ON match.
     * Source row binds reuse the same named parameters as the base INSERT ({@code :column}).
     *
     * @param writer         JDBC writer configuration
     * @param table          target table name
     * @param targetColumns  target table column names in insert order
     * @return MERGE statement for batch upsert
     */
    private static String appendDamengMerge(
            JdbcWriterVO writer,
            String table,
            List<String> targetColumns) {
        List<String> upsertKeys = requireUpsertKeys(writer, targetColumns);
        String columns = String.join(", ", targetColumns);
        // Dameng MERGE binds one row per batch entry via named parameters on the USING subquery.
        String selectList = targetColumns.stream()
                .map(column -> ":" + column + " AS " + column)
                .collect(Collectors.joining(", "));
        String onClause = upsertKeys.stream()
                .map(key -> "t." + key + " = s." + key)
                .collect(Collectors.joining(" AND "));
        List<String> updateColumns = targetColumns.stream()
                .filter(column -> !upsertKeys.contains(column))
                .toList();
        StringBuilder sql = new StringBuilder();
        sql.append("merge into ").append(table).append(" t using (select ")
                .append(selectList)
                .append(" from dual) s on (")
                .append(onClause)
                .append(")");
        if (!updateColumns.isEmpty()) {
            String updates = updateColumns.stream()
                    .map(column -> "t." + column + " = s." + column)
                    .collect(Collectors.joining(", "));
            sql.append(" when matched then update set ").append(updates);
        }
        String insertValues = targetColumns.stream()
                .map(column -> "s." + column)
                .collect(Collectors.joining(", "));
        sql.append(" when not matched then insert (")
                .append(columns)
                .append(") values (")
                .append(insertValues)
                .append(")");
        return sql.toString();
    }

    /**
     * Resolves upsert key columns from {@code options.upsertKeys} or legacy {@code conflictColumns}.
     *
     * @param writer        JDBC writer configuration
     * @param targetColumns known target column names
     * @return non-empty upsert key list
     */
    private static List<String> requireUpsertKeys(JdbcWriterVO writer, List<String> targetColumns) {
        List<String> upsertKeys = WriterOptionResolver.upsertKeysOption(writer);
        String writerName = writer.getTarget() == null ? "jdbc" : writer.getTarget();
        if (upsertKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "JDBC sink writer '" + writerName + "' upsert=true requires non-empty options.upsertKeys; "
                            + "known columns: " + targetColumns);
        }
        for (String key : upsertKeys) {
            if (!targetColumns.contains(key)) {
                throw new IllegalArgumentException(
                        "JDBC sink writer '" + writerName + "' upsert key '" + key + "' is not a known column; "
                                + "known columns: " + targetColumns);
            }
        }
        return upsertKeys;
    }

    private static String resolveDialect(JdbcWriterVO writer) {
        String dialect = WriterOptionResolver.stringOption(writer, "dialect", null);
        if (!StringUtils.hasText(dialect)) {
            return "generic";
        }
        return dialect.trim().toLowerCase(Locale.ROOT);
    }
}
