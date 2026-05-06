package org.gensokyo.data.calcite;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.database.DbTypeKit;
import org.gensokyo.data.database.dialect.DialectFactory;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.scripter.ScriptVO;
import org.gensokyo.data.model.vo.stage.ParamVO;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class QueryRowSource implements RowSource {
    private static final ExpressionParser SPEL = new SpelExpressionParser();

    private final String name;
    private final RowSchema schema;
    private final List<Row> rows;

    public QueryRowSource(String name, QuerySourceVO source, NamedParameterJdbcTemplate jdbcTemplate) {
        this.name = name;
        Map<String, Object> params = toParams(source.getParams());
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(source.getDataSourceId()));
            String sql = resolveSql(source, jdbcTemplate.getJdbcTemplate().getDataSource());
            List<Map<String, Object>> result = jdbcTemplate.query(
                    sql,
                    params,
                    (ResultSetExtractor<List<Map<String, Object>>>) this::mapRows
            );
            this.rows = result.stream()
                    .map(row -> new Row(new LinkedHashMap<>(row)))
                    .toList();
            this.schema = source.getSchema() != null ? source.getSchema() : inferSchema(result, sql, params, jdbcTemplate);
        } finally {
            DynamicDataSourceContextHolder.clear();
            DialectFactory.clearDbType();
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

    @Override
    public List<Row> rows() {
        return rows;
    }

    private List<Map<String, Object>> mapRows(java.sql.ResultSet rs) throws java.sql.SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ColumnMapRowMapper mapper = new ColumnMapRowMapper();
        while (rs.next()) {
            rows.add(normalizeKeys(mapper.mapRow(rs, rows.size())));
        }
        return rows;
    }

    private Map<String, Object> normalizeKeys(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach((key, value) -> normalized.put(key == null ? null : key.toLowerCase(Locale.ROOT), value));
        return normalized;
    }

    private RowSchema inferSchema(List<Map<String, Object>> result,
                                  String sql,
                                  Map<String, Object> params,
                                  NamedParameterJdbcTemplate jdbcTemplate) {
        RowSchema schema = new RowSchema();
        if (!result.isEmpty()) {
            List<ColumnDef> columns = new ArrayList<>();
            result.get(0).forEach((key, value) -> columns.add(new ColumnDef(key, logicalType(value), true)));
            schema.setColumns(columns);
            return schema;
        }
        return inferSchemaWithoutRows(schema, sql, params, jdbcTemplate);
    }

    private RowSchema inferSchemaWithoutRows(RowSchema schema,
                                             String sql,
                                             Map<String, Object> params,
                                             NamedParameterJdbcTemplate jdbcTemplate) {
        jdbcTemplate.query(sql, params, (ResultSetExtractor<Void>) rs -> {
            ResultSetMetaData metaData = rs.getMetaData();
            List<ColumnDef> columns = new ArrayList<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                columns.add(new ColumnDef(
                        metaData.getColumnLabel(i).toLowerCase(Locale.ROOT),
                        normalizeJdbcType(metaData.getColumnTypeName(i)),
                        true
                ));
            }
            schema.setColumns(columns);
            return null;
        });
        return schema;
    }

    private String logicalType(Object value) {
        if (value instanceof Integer || value instanceof Long) {
            return "BIGINT";
        }
        if (value instanceof Number) {
            return "DECIMAL";
        }
        if (value instanceof Boolean) {
            return "BOOLEAN";
        }
        return "VARCHAR";
    }

    private String normalizeJdbcType(String jdbcType) {
        if (jdbcType == null || jdbcType.isBlank()) {
            return "VARCHAR";
        }
        String normalized = jdbcType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "INTEGER", "INT", "BIGINT", "SMALLINT", "TINYINT" -> "BIGINT";
            case "DECIMAL", "NUMERIC", "DOUBLE", "FLOAT", "REAL" -> "DECIMAL";
            case "BOOLEAN", "BIT" -> "BOOLEAN";
            case "DATE" -> "DATE";
            case "TIMESTAMP", "TIMESTAMP WITH TIME ZONE", "DATETIME" -> "TIMESTAMP";
            default -> "VARCHAR";
        };
    }

    private String resolveSql(QuerySourceVO source, DataSource dataSource) {
        String sql = Objects.requireNonNull(source.getSql());
        long limit = resolveLimit(source);
        long offset = resolveOffset(source);
        if (limit <= 0 || dataSource == null) {
            return sql;
        }
        DialectFactory.setDbType(DbTypeKit.getDbType(dataSource));
        return DialectFactory.getDialect().forPagination(new StringBuilder(sql), limit, offset);
    }

    private long resolveOffset(QuerySourceVO source) {
        Integer pageIndex = source.getPageIndex();
        Integer pageSize = source.getPageSize();
        if (pageIndex == null || pageSize == null || pageIndex <= 1 || pageSize <= 0) {
            return 0L;
        }
        return (long) (pageIndex - 1) * pageSize;
    }

    private long resolveLimit(QuerySourceVO source) {
        long limit = Long.MAX_VALUE;
        if (source.getPageSize() != null && source.getPageSize() > 0) {
            limit = Math.min(limit, source.getPageSize().longValue());
        }
        if (source.getMaxRows() != null && source.getMaxRows() > 0) {
            limit = Math.min(limit, source.getMaxRows());
        }
        return limit == Long.MAX_VALUE ? 0L : limit;
    }

    private Map<String, Object> toParams(List<ParamVO> params) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (params == null) {
            return values;
        }
        for (ParamVO param : params) {
            if (param != null && param.getName() != null) {
                values.put(param.getName(), evaluateParam(param));
            }
        }
        return values;
    }

    private Object evaluateParam(ParamVO param) {
        ScriptVO script = param.getLanguage();
        if (script == null) {
            return null;
        }
        String type = script.getType();
        if (type == null || Const.ScriptType.PLAIN.equalsIgnoreCase(type)) {
            return script.getContent();
        }
        return SPEL.parseExpression(script.getContent()).getValue(buildEvaluationContext());
    }

    private StandardEvaluationContext buildEvaluationContext() {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable(Const.SCRIPT_VAR_DATASET, Map.of());
        context.setVariable(Const.SCRIPT_VAR_ARGS, List.of());
        return context;
    }
}
