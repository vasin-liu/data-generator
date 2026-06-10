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
        return appendUpsertClause(writer, table, columns, values, baseInsert);
    }

    private static String appendUpsertClause(
            JdbcWriterVO writer,
            String table,
            String columns,
            String values,
            String baseInsert) {
        String dialect = resolveDialect(writer);
        return switch (dialect) {
            case "postgres" -> appendPostgresUpsert(writer, baseInsert);
            case "mysql" -> "insert ignore into " + table + " (" + columns + ") values (" + values + ")";
            default -> baseInsert;
        };
    }

    private static String appendPostgresUpsert(JdbcWriterVO writer, String baseInsert) {
        String conflictColumns = WriterOptionResolver.stringOption(writer, "conflictColumns", null);
        if (!StringUtils.hasText(conflictColumns)) {
            throw new IllegalArgumentException(
                    "JDBC sink upsert with dialect postgres requires options.conflictColumns");
        }
        return baseInsert + " on conflict (" + conflictColumns.trim() + ") do nothing";
    }

    private static String resolveDialect(JdbcWriterVO writer) {
        String dialect = WriterOptionResolver.stringOption(writer, "dialect", null);
        if (!StringUtils.hasText(dialect)) {
            return "generic";
        }
        return dialect.trim().toLowerCase(Locale.ROOT);
    }
}
