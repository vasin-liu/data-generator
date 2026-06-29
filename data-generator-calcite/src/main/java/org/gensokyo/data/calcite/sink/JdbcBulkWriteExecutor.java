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
import java.sql.Statement;
import java.util.ArrayList;
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
     * @param writeStats   optional collector for upsert counters (may be null)
     */
    static void writeSlice(
            NamedParameterJdbcTemplate jdbcTemplate,
            JdbcWriterVO writer,
            List<JdbcSinkColumnMappings.ColumnMapping> mappings,
            List<Row> rows,
            JdbcSinkWriteStats writeStats) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        JdbcSinkBulkMode bulkMode = JdbcSinkBulkMode.resolve(writer);
        switch (bulkMode) {
            case POSTGRES_COPY -> writePostgresCopy(jdbcTemplate, writer, mappings, rows);
            case CLICKHOUSE_INSERT -> writeClickHouseInsert(jdbcTemplate, writer, mappings, rows);
            case JDBC_BATCH -> writeJdbcBatch(jdbcTemplate, writer, mappings, rows, writeStats);
        }
    }

    private static void writeJdbcBatch(
            NamedParameterJdbcTemplate jdbcTemplate,
            JdbcWriterVO writer,
            List<JdbcSinkColumnMappings.ColumnMapping> mappings,
            List<Row> rows,
            JdbcSinkWriteStats writeStats) {
        List<String> targetColumns = mappings.stream().map(JdbcSinkColumnMappings.ColumnMapping::target).toList();
        // Fail-fast upsert key validation before JDBC execute (D-14).
        JdbcSinkSqlBuilder.validateUpsertKeys(writer, targetColumns);
        String sql = JdbcSinkSqlBuilder.buildSql(writer, targetColumns);
        List<Row> writableRows = filterWritableRows(writer, mappings, rows, writeStats);
        if (writableRows.isEmpty()) {
            return;
        }
        Map<String, ?>[] batch = writableRows.stream()
                .map(row -> JdbcSinkColumnMappings.toSqlParams(row, mappings))
                .toArray(Map[]::new);
        int[] updateCounts = jdbcTemplate.batchUpdate(sql, batch);
        if (writeStats != null && WriterOptionResolver.booleanOption(writer, "upsert")) {
            String dialect = resolveDialect(writer);
            long upserted = countUpsertedRows(updateCounts, dialect);
            writeStats.addRowsUpserted(upserted);
        }
    }

    /**
     * Interprets JDBC batch update counts for upsert metrics.
     * <p>
     * MySQL returns {@code 2} when a duplicate-key row was updated and {@code 1} for a fresh insert.
     * PostgreSQL returns {@code 1} for both insert and update on {@code ON CONFLICT DO UPDATE}.
     * {@link Statement#SUCCESS_NO_INFO} is treated as one successful row when drivers omit exact counts.
     * </p>
     *
     * @param updateCounts per-row batch update counts from the driver
     * @param dialect      normalized JDBC sink dialect
     * @return number of rows counted as upsert updates
     */
    static long countUpsertedRows(int[] updateCounts, String dialect) {
        long upserted = 0;
        for (int count : updateCounts) {
            upserted += upsertCountAsRows(count, dialect);
        }
        return upserted;
    }

    private static int upsertCountAsRows(int updateCount, String dialect) {
        if (updateCount == Statement.SUCCESS_NO_INFO) {
            return 1;
        }
        if (updateCount < 0) {
            return 0;
        }
        if ("mysql".equals(dialect)) {
            return updateCount == 2 ? 1 : 0;
        }
        // PostgreSQL upsert path: count successful row operations (insert or update).
        return updateCount > 0 ? 1 : 0;
    }

    /**
     * Filters rows with null upsert-key values when upsert mode is active.
     * Skipped rows are counted separately from JDBC failures (D-16, W-03).
     */
    private static List<Row> filterWritableRows(
            JdbcWriterVO writer,
            List<JdbcSinkColumnMappings.ColumnMapping> mappings,
            List<Row> rows,
            JdbcSinkWriteStats writeStats) {
        if (!WriterOptionResolver.booleanOption(writer, "upsert")) {
            return rows;
        }
        List<String> upsertKeys = WriterOptionResolver.upsertKeysOption(writer);
        if (upsertKeys.isEmpty()) {
            return rows;
        }
        List<Row> writable = new ArrayList<>(rows.size());
        long skipped = 0;
        for (Row row : rows) {
            if (hasNullUpsertKey(row, upsertKeys, mappings)) {
                skipped++;
            } else {
                writable.add(row);
            }
        }
        if (writeStats != null && skipped > 0) {
            writeStats.addRowsSkipped(skipped);
        }
        return writable;
    }

    private static boolean hasNullUpsertKey(
            Row row,
            List<String> upsertKeys,
            List<JdbcSinkColumnMappings.ColumnMapping> mappings) {
        for (String upsertKey : upsertKeys) {
            String sourceColumn = resolveSourceColumn(upsertKey, mappings);
            if (row.get(sourceColumn) == null) {
                return true;
            }
        }
        return false;
    }

    private static String resolveSourceColumn(
            String upsertKey,
            List<JdbcSinkColumnMappings.ColumnMapping> mappings) {
        for (JdbcSinkColumnMappings.ColumnMapping mapping : mappings) {
            if (upsertKey.equalsIgnoreCase(mapping.target()) || upsertKey.equalsIgnoreCase(mapping.source())) {
                return mapping.source();
            }
        }
        return upsertKey;
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

    private static String resolveDialect(JdbcWriterVO writer) {
        String dialect = WriterOptionResolver.stringOption(writer, "dialect", null);
        if (!StringUtils.hasText(dialect)) {
            return "generic";
        }
        return dialect.trim().toLowerCase(Locale.ROOT);
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
