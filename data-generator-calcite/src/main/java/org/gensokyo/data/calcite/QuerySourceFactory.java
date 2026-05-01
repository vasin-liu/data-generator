package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class QuerySourceFactory implements V2SourceFactory {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver;

    public QuerySourceFactory(NamedParameterJdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, new NoopRuntimeJdbcEndpointResolver());
    }

    public QuerySourceFactory(NamedParameterJdbcTemplate jdbcTemplate,
                              RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver) {
        this.jdbcTemplate = jdbcTemplate;
        this.runtimeJdbcEndpointResolver = runtimeJdbcEndpointResolver;
    }

    @Override
    public boolean supports(SourceVO source) {
        return source instanceof QuerySourceVO;
    }

    @Override
    public RowSource create(String name, SourceVO source) {
        QuerySourceVO querySource = (QuerySourceVO) source;
        String effectiveDataSourceId = runtimeJdbcEndpointResolver.resolveSourceDataSourceId(querySource);
        if (effectiveDataSourceId != null && !effectiveDataSourceId.isBlank()) {
            querySource.setDataSourceId(effectiveDataSourceId);
        }
        return new QueryRowSource(name, querySource, jdbcTemplate);
    }
}
