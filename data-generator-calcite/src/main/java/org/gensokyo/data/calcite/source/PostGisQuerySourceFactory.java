/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.V2SourceFactory;
import org.gensokyo.data.model.v2.PostGisQuerySourceVO;
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
        PostGisQuerySourceVO postGisSource = (PostGisQuerySourceVO) source;
        String effectiveDataSourceId = runtimeJdbcEndpointResolver.resolveSourceDataSourceId(wrapAsQuery(postGisSource));
        if (effectiveDataSourceId != null && !effectiveDataSourceId.isBlank()) {
            postGisSource.setDataSourceId(effectiveDataSourceId);
        }
        return new PostGisQueryRowSource(name, postGisSource, jdbcTemplate);
    }

    private static org.gensokyo.data.model.v2.QuerySourceVO wrapAsQuery(PostGisQuerySourceVO source) {
        org.gensokyo.data.model.v2.QuerySourceVO query = new org.gensokyo.data.model.v2.QuerySourceVO();
        query.setDataSourceId(source.getDataSourceId());
        query.setDataSource(source.getDataSource());
        return query;
    }
}
