package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.runtime.EffectiveExecutionPolicy;

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
        return create(name, source, null);
    }

    /**
     * Creates a row source, using {@link ChunkedQueryRowSource} when policy mode is {@code CHUNKED} or {@code STREAMING}.
     *
     * @param name   logical source name
     * @param source source configuration
     * @param policy optional effective execution policy; when {@code null}, uses in-memory {@link QueryRowSource}
     * @return row source implementation
     */
    public RowSource create(String name, SourceVO source, EffectiveExecutionPolicy policy) {
        QuerySourceVO querySource = (QuerySourceVO) source;
        String effectiveDataSourceId = runtimeJdbcEndpointResolver.resolveSourceDataSourceId(querySource);
        if (effectiveDataSourceId != null && !effectiveDataSourceId.isBlank()) {
            querySource.setDataSourceId(effectiveDataSourceId);
        }
        if (policy != null && usesChunkedRead(policy.mode())) {
            return new ChunkedQueryRowSource(name, querySource, jdbcTemplate, policy.sourceChunkSize());
        }
        return new QueryRowSource(name, querySource, jdbcTemplate);
    }

    private static boolean usesChunkedRead(String mode) {
        return "CHUNKED".equals(mode) || "STREAMING".equals(mode);
    }
}
