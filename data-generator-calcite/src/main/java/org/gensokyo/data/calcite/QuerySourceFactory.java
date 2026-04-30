package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class QuerySourceFactory implements V2SourceFactory {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public QuerySourceFactory(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean supports(SourceVO source) {
        return source instanceof QuerySourceVO;
    }

    @Override
    public RowSource create(String name, SourceVO source) {
        return new QueryRowSource(name, (QuerySourceVO) source, jdbcTemplate);
    }
}
