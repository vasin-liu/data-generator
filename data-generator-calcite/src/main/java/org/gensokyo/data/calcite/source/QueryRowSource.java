package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.parser.*;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.gensokyo.data.database.dialect.DialectFactory;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class QueryRowSource implements RowSource {

    private final String name;
    private final RowSchema schema;
    private final List<Row> rows;

    public QueryRowSource(String name, QuerySourceVO source, NamedParameterJdbcTemplate jdbcTemplate) {
        this.name = name;
        Map<String, Object> params = QueryRowSourceSupport.toParams(source.getParams());
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(source.getDataSourceId()));
            String sql = QueryRowSourceSupport.resolveSql(source, jdbcTemplate.getJdbcTemplate().getDataSource());
            List<Map<String, Object>> result = jdbcTemplate.query(
                    sql,
                    params,
                    (ResultSetExtractor<List<Map<String, Object>>>) this::mapRows
            );
            this.rows = result.stream()
                    .map(row -> new Row(new LinkedHashMap<>(row)))
                    .toList();
            this.schema = source.getSchema() != null
                    ? source.getSchema()
                    : QueryRowSourceSupport.inferSchema(result, sql, params, jdbcTemplate);
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
            rows.add(QueryRowSourceSupport.normalizeKeys(mapper.mapRow(rs, rows.size())));
        }
        return rows;
    }
}
