/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.model.v2.PostGisQuerySourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;

/**
 * Maps {@link PostGisQuerySourceVO} to a JDBC {@link QuerySourceVO} with generated PostGIS SQL.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public final class PostGisQuerySourceSupport {

    private PostGisQuerySourceSupport() {
    }

    /**
     * Builds the query-source view of a PostGIS table read (SQL from {@link PostGisQuerySqlBuilder}).
     *
     * @param source PostGIS configuration
     * @return query source used by {@link QueryRowSource} / {@link ChunkedQueryRowSource}
     */
    public static QuerySourceVO toQuerySource(PostGisQuerySourceVO source) {
        QuerySourceVO query = new QuerySourceVO();
        query.setDataSourceId(source.getDataSourceId());
        query.setDataSource(source.getDataSource());
        query.setSql(PostGisQuerySqlBuilder.buildSelect(source));
        query.setMaxRows(source.getMaxRows());
        query.setSchema(source.getSchema());
        return query;
    }

    /**
     * Minimal query wrapper for {@link org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver}.
     *
     * @param source PostGIS configuration
     * @return query source with only datasource fields set
     */
    public static QuerySourceVO endpointLookup(PostGisQuerySourceVO source) {
        QuerySourceVO query = new QuerySourceVO();
        query.setDataSourceId(source.getDataSourceId());
        query.setDataSource(source.getDataSource());
        return query;
    }
}
