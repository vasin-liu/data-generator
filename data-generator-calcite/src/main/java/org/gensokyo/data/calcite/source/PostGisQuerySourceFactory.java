/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.V2SourceFactory;
import org.gensokyo.data.calcite.runtime.EffectiveExecutionPolicy;
import org.gensokyo.data.model.v2.PostGisQuerySourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Objects;

/**
 * Factory for Template V2 {@code type: postgis} sources (requires PostGIS-enabled JDBC datasource).
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public class PostGisQuerySourceFactory implements V2SourceFactory {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver;

    /**
     * Creates a factory with JDBC access and endpoint resolution.
     *
     * @param jdbcTemplate                template for the target database
     * @param runtimeJdbcEndpointResolver resolves inline datasource ids
     */
    public PostGisQuerySourceFactory(NamedParameterJdbcTemplate jdbcTemplate,
                                     RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.runtimeJdbcEndpointResolver = Objects.requireNonNull(runtimeJdbcEndpointResolver, "runtimeJdbcEndpointResolver");
    }

    @Override
    public boolean supports(SourceVO source) {
        return source instanceof PostGisQuerySourceVO;
    }

    @Override
    public RowSource create(String name, SourceVO source) {
        return create(name, source, null);
    }

    /**
     * Creates a row source, using {@link ChunkedQueryRowSource} when policy mode is {@code CHUNKED} or {@code STREAMING}.
     *
     * @param name   logical source name
     * @param source PostGIS source configuration
     * @param policy optional effective execution policy
     * @return in-memory or chunked JDBC row source
     */
    public RowSource create(String name, SourceVO source, EffectiveExecutionPolicy policy) {
        PostGisQuerySourceVO postGisSource = resolveEndpoint((PostGisQuerySourceVO) source);
        QuerySourceVO query = PostGisQuerySourceSupport.toQuerySource(postGisSource);
        if (policy != null && usesChunkedRead(policy.mode())) {
            return new ChunkedQueryRowSource(name, query, jdbcTemplate, policy.sourceChunkSize());
        }
        return new QueryRowSource(name, query, jdbcTemplate);
    }

    private PostGisQuerySourceVO resolveEndpoint(PostGisQuerySourceVO source) {
        String effectiveDataSourceId = runtimeJdbcEndpointResolver.resolveSourceDataSourceId(
                PostGisQuerySourceSupport.endpointLookup(source));
        if (effectiveDataSourceId != null && !effectiveDataSourceId.isBlank()) {
            source.setDataSourceId(effectiveDataSourceId);
        }
        return source;
    }

    private static boolean usesChunkedRead(String mode) {
        return "CHUNKED".equals(mode) || "STREAMING".equals(mode);
    }
}
