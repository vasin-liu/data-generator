/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.gensokyo.data.database.dialect.DialectFactory;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JDBC query row source that streams rows in chunks using a forward-only {@link ResultSet}.
 * <p>
 * Call {@link #hasNextChunk()} and {@link #nextChunk(int)} to read data. {@link #rows()} is intentionally
 * empty; use the chunked API instead of materializing all rows.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public class ChunkedQueryRowSource implements ChunkedRowSource {

    private final String name;
    private final QuerySourceVO source;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final int fetchSize;
    private final Map<String, Object> params;
    private final String sql;
    private final long maxRowsCap;

    private RowSchema schema;
    private long rowsReadSoFar;
    private boolean exhausted;
    private boolean queryOpen;

    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;

    /**
     * Opens a chunked JDBC reader for the given query source.
     *
     * @param name         logical source name
     * @param source       query source configuration
     * @param jdbcTemplate JDBC template for the target datasource
     * @param fetchSize    JDBC {@link PreparedStatement#setFetchSize(int)} hint and default chunk size
     */
    public ChunkedQueryRowSource(String name,
                                 QuerySourceVO source,
                                 NamedParameterJdbcTemplate jdbcTemplate,
                                 int fetchSize) {
        this.name = name;
        this.source = source;
        this.jdbcTemplate = jdbcTemplate;
        this.fetchSize = fetchSize > 0 ? fetchSize : 1;
        this.params = QueryRowSourceSupport.toParams(source.getParams());
        DynamicDataSourceContextHolder.push(Objects.requireNonNull(source.getDataSourceId()));
        try {
            DataSource dataSource = jdbcTemplate.getJdbcTemplate().getDataSource();
            this.sql = QueryRowSourceSupport.resolveSql(source, dataSource);
            this.maxRowsCap = QueryRowSourceSupport.resolveMaxRowsCap(source);
            this.schema = source.getSchema();
        } catch (RuntimeException ex) {
            DynamicDataSourceContextHolder.clear();
            DialectFactory.clearDbType();
            throw ex;
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public RowSchema schema() {
        return schema;
    }

    /**
     * Does not materialize rows. Use {@link #nextChunk(int)} instead.
     *
     * @return empty unmodifiable list
     */
    @Override
    public List<Row> rows() {
        return List.of();
    }

    @Override
    public boolean supportsChunking() {
        return true;
    }

    @Override
    public boolean hasNextChunk() {
        return !exhausted && !maxRowsCapReached();
    }

    @Override
    public List<Row> nextChunk(int maxRows) {
        if (maxRows <= 0) {
            throw new IllegalArgumentException("maxRows must be positive");
        }
        if (exhausted || maxRowsCapReached()) {
            return List.of();
        }
        ensureQueryOpen();
        int chunkLimit = effectiveChunkLimit(maxRows);
        List<Row> chunk = new ArrayList<>(chunkLimit);
        ColumnMapRowMapper mapper = new ColumnMapRowMapper();
        try {
            while (chunk.size() < chunkLimit && !maxRowsCapReached()) {
                if (!resultSet.next()) {
                    exhausted = true;
                    break;
                }
                Map<String, Object> rowMap = QueryRowSourceSupport.normalizeKeys(mapper.mapRow(resultSet, chunk.size()));
                chunk.add(new Row(new LinkedHashMap<>(rowMap)));
                rowsReadSoFar++;
                if (schema == null) {
                    schema = QueryRowSourceSupport.inferSchemaFromRow(rowMap);
                }
                if (maxRowsCapReached()) {
                    exhausted = true;
                    break;
                }
            }
        } catch (SQLException ex) {
            closeQuery();
            throw new IllegalStateException("Failed to read chunk from JDBC query for source [" + name + "]", ex);
        }
        if (exhausted) {
            if (schema == null) {
                schema = QueryRowSourceSupport.inferSchemaWithoutRows(
                        new RowSchema(), sql, params, jdbcTemplate);
            }
            closeQuery();
        }
        return List.copyOf(chunk);
    }

    @Override
    public long rowsReadSoFar() {
        return rowsReadSoFar;
    }

    private void ensureQueryOpen() {
        if (queryOpen) {
            return;
        }
        DataSource dataSource = Objects.requireNonNull(
                jdbcTemplate.getJdbcTemplate().getDataSource(),
                "jdbcTemplate dataSource");
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
            SqlParameterSource paramSource = new MapSqlParameterSource(params);
            String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
            statement = connection.prepareStatement(
                    sqlToUse,
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
            statement.setFetchSize(fetchSize);
            Object[] paramValues = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
            int[] paramTypes = NamedParameterUtils.buildSqlTypeArray(parsedSql, paramSource);
            for (int i = 0; i < paramValues.length; i++) {
                statement.setObject(i + 1, paramValues[i], paramTypes[i]);
            }
            resultSet = statement.executeQuery();
            queryOpen = true;
        } catch (SQLException ex) {
            closeQuery();
            throw new IllegalStateException("Failed to open JDBC query for source [" + name + "]", ex);
        }
    }

    private int effectiveChunkLimit(int maxRows) {
        if (maxRowsCap <= 0) {
            return maxRows;
        }
        long remaining = maxRowsCap - rowsReadSoFar;
        if (remaining <= 0) {
            return 0;
        }
        return (int) Math.min(maxRows, remaining);
    }

    private boolean maxRowsCapReached() {
        return maxRowsCap > 0 && rowsReadSoFar >= maxRowsCap;
    }

    private void closeQuery() {
        SQLException suppressed = null;
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException ex) {
                suppressed = ex;
            }
            resultSet = null;
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ex) {
                if (suppressed == null) {
                    suppressed = ex;
                }
            }
            statement = null;
        }
        if (connection != null) {
            DataSource dataSource = jdbcTemplate.getJdbcTemplate().getDataSource();
            if (dataSource != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
            connection = null;
        }
        queryOpen = false;
        DynamicDataSourceContextHolder.clear();
        DialectFactory.clearDbType();
        if (suppressed != null) {
            throw new IllegalStateException("Failed to close JDBC resources for source [" + name + "]", suppressed);
        }
    }
}
