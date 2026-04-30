package org.gensokyo.data.calcite;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.gensokyo.data.constant.Const;
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
            List<Map<String, Object>> result = jdbcTemplate.query(
                    Objects.requireNonNull(source.getSql()),
                    params,
                    (ResultSetExtractor<List<Map<String, Object>>>) this::mapRows
            );
            this.rows = result.stream()
                    .map(row -> new Row(new LinkedHashMap<>(row)))
                    .toList();
            this.schema = source.getSchema() != null ? source.getSchema() : inferSchema(result);
        } finally {
            DynamicDataSourceContextHolder.clear();
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

    private RowSchema inferSchema(List<Map<String, Object>> result) {
        RowSchema schema = new RowSchema();
        if (result.isEmpty()) {
            return schema;
        }
        List<ColumnDef> columns = new ArrayList<>();
        result.get(0).forEach((key, value) -> columns.add(new ColumnDef(key, logicalType(value), true)));
        schema.setColumns(columns);
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
