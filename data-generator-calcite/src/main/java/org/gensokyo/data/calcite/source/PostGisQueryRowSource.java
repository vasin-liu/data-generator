/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.model.v2.PostGisQuerySourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

/**
 * PostGIS table reader implemented by translating configuration into a {@link QuerySourceVO} SQL query.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public class PostGisQueryRowSource implements RowSource {

    private final QueryRowSource delegate;

    /**
     * Builds a finite JDBC row source using generated PostGIS SQL.
     *
     * @param name         logical source name
     * @param source       PostGIS configuration
     * @param jdbcTemplate JDBC access to the PostGIS-enabled database
     */
    public PostGisQueryRowSource(String name, PostGisQuerySourceVO source, NamedParameterJdbcTemplate jdbcTemplate) {
        QuerySourceVO query = new QuerySourceVO();
        query.setDataSourceId(source.getDataSourceId());
        query.setDataSource(source.getDataSource());
        query.setSql(PostGisQuerySqlBuilder.buildSelect(source));
        query.setMaxRows(source.getMaxRows());
        query.setSchema(source.getSchema());
        this.delegate = new QueryRowSource(name, query, jdbcTemplate);
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public RowSchema schema() {
        return delegate.schema();
    }

    @Override
    public List<Row> rows() {
        return delegate.rows();
    }
}
