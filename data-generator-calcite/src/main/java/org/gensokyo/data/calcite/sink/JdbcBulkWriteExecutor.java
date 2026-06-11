/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Executes JDBC sink batches using optional bulk loaders (COPY / multi-row INSERT).
 *
 * @author Gensokyo
 * @since 2026-06-11
 */
final class JdbcBulkWriteExecutor {

    private JdbcBulkWriteExecutor() {
    }

    /**
     * Writes one batch slice using the configured bulk mode.
     *
     * @param jdbcTemplate JDBC template bound to the sink datasource
     * @param writer       JDBC writer configuration
     * @param mappings     column mappings for the batch
     * @param rows         rows in this batch slice
     */
    static void writeSlice(
            NamedParameterJdbcTemplate jdbcTemplate,
            JdbcWriterVO writer,
            List<JdbcSinkColumnMappings.ColumnMapping> mappings,
            List<Row> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        JdbcSinkBulkMode bulkMode = JdbcSinkBulkMode.resolve(writer);
        switch (bulkMode) {
            case POSTGRES_COPY -> writePostgresCopy(jdbcTemplate, writer, mappings, rows);
            case CLICKHOUSE_INSERT -> writeClickHouseInsert(jdbcTemplate, writer, mappings, rows);
            case JDBC_BATCH -> writeJdbcBatch(jdbcTemplate, writer, mappings, rows);
        }
    }

    private static void writeJdbcBatch(
            NamedParameterJdbcTemplate jdbcTemplate,
            JdbcWriterVO writer,
            List<JdbcSinkColumnMappings.ColumnMapping> mappings,
            List<Row> rows) {
        List<String> targetColumns = mappings.stream().map(JdbcSinkColumnMappings.ColumnMapping::target).toList();
        String sql = JdbcSinkSqlBuilder.buildSql(writer, targetColumns);
        Map<String, ?>[] batch = rows.stream()
                .map(row -> JdbcSinkColumnMappings.toSqlParams(row, mappings))
                .toArray(Map[]::new);
        jdbcTemplate.batchUpdate(sql, batch);
    }

    private static void writePostgresCopy(
            NamedParameterJdbcTemplate jdbcTemplate,
            JdbcWriterVO writer,
            List<JdbcSinkColumnMappings.ColumnMapping> mappings,
            List<Row> rows) {
        if (WriterOptionResolver.booleanOption(writer, "upsert")) {
            throw new IllegalArgumentException("JDBC sink bulkMode postgres_copy does not support upsert=true");
        }
        String dialect = WriterOptionResolver.stringOption(writer, "dialect", null);
        if (StringUtils.hasText(dialect)
                && !"postgres".equalsIgnoreCase(dialect.trim())
                && !"postgresql".equalsIgnoreCase(dialect.trim())) {
            throw new IllegalArgumentException("JDBC sink bulkMode postgres_copy requires dialect postgres");
        }
        String table = Objects.requireNonNull(writer.getTarget(), "JDBC sink target must not be null");
        String columns = mappings.stream()
                .map(JdbcSinkColumnMappings.ColumnMapping::target)
                .collect(Collectors.joining(", "));
        String copySql = "COPY " + table + " (" + columns + ") FROM STDIN WITH (FORMAT csv)";
        String csv = buildCsvPayload(rows, mappings);
        DataSource dataSource = Objects.requireNonNull(
                jdbcTemplate.getJdbcTemplate().getDataSource(),
                "JDBC sink datasource must not be null");
        try (Connection connection = dataSource.getConnection()) {
            CopyManager copyManager = new CopyManager(connection.unwrap(BaseConnection.class));
            long copied = copyManager.copyIn(copySql, new StringReader(csv));
            if (copied <= 0) {
                throw new IllegalStateException("PostgreSQL COPY wrote zero bytes");
            }
        } catch (SQLException | IOException ex) {
            throw new IllegalStateException("PostgreSQL COPY bulk write failed for table " + table, ex);
        }
    }

    private static void writeClickHouseInsert(
            NamedParameterJdbcTemplate jdbcTemplate,
            JdbcWriterVO writer,
            List<JdbcSinkColumnMappings.ColumnMapping> mappings,
            List<Row> rows) {
        if (WriterOptionResolver.booleanOption(writer, "upsert")) {
            throw new IllegalArgumentException("JDBC sink bulkMode clickhouse_insert does not support upsert=true");
        }
        String dialect = WriterOptionResolver.stringOption(writer, "dialect", null);
        if (StringUtils.hasText(dialect)) {
            String normalized = dialect.trim().toLowerCase(Locale.ROOT);
            if (!"clickhouse".equals(normalized) && !"click_house".equals(normalized)) {
                throw new IllegalArgumentException("JDBC sink bulkMode clickhouse_insert requires dialect clickhouse");
            }
        }
        String table = Objects.requireNonNull(writer.getTarget(), "JDBC sink target must not be null");
        String columns = mappings.stream()
                .map(JdbcSinkColumnMappings.ColumnMapping::target)
                .collect(Collectors.joining(", "));
        String valuesClause = rows.stream()
                .map(row -> "(" + formatClickHouseTuple(JdbcSinkColumnMappings.orderedValues(row, mappings)) + ")")
                .collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + table + " (" + columns + ") VALUES " + valuesClause;
        jdbcTemplate.getJdbcTemplate().update(sql);
    }

    private static String buildCsvPayload(List<Row> rows, List<JdbcSinkColumnMappings.ColumnMapping> mappings) {
        StringBuilder payload = new StringBuilder();
        for (Row row : rows) {
            List<Object> values = JdbcSinkColumnMappings.orderedValues(row, mappings);
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    payload.append(',');
                }
                payload.append(csvCell(values.get(i)));
            }
            payload.append('\n');
        }
        return payload.toString();
    }

    private static String csvCell(Object value) {
        if (value == null) {
            // COPY CSV treats an unquoted empty field as NULL.
            return "";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private static String formatClickHouseTuple(List<Object> values) {
        return values.stream().map(JdbcBulkWriteExecutor::formatClickHouseLiteral).collect(Collectors.joining(", "));
    }

    private static String formatClickHouseLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        String text = value.toString().replace("'", "''");
        return "'" + text + "'";
    }
}
