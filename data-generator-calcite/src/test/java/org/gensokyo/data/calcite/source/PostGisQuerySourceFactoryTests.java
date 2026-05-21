/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.runtime.EffectiveExecutionPolicy;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.PostGisQuerySourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Factory behavior for {@link PostGisQuerySourceFactory} (including CHUNKED policy).
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
class PostGisQuerySourceFactoryTests {

    @Test
    void createsChunkedRowSourceWhenPolicyIsChunked() {
        PostGisQuerySourceVO source = new PostGisQuerySourceVO();
        source.setDataSourceId("ignored");
        source.setTable("sites");

        ExecutionPolicyVO policyVo = new ExecutionPolicyVO();
        policyVo.setMode("CHUNKED");
        policyVo.setSourceChunkSize(500);

        PostGisQuerySourceFactory factory = new PostGisQuerySourceFactory(
                new NamedParameterJdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:postgis_factory;DB_CLOSE_DELAY=-1")),
                new NoopRuntimeJdbcEndpointResolver());

        RowSource rowSource = factory.create("sites_in", source, EffectiveExecutionPolicy.resolve(policyVo));

        Assertions.assertInstanceOf(ChunkedRowSource.class, rowSource);
        Assertions.assertTrue(((ChunkedRowSource) rowSource).supportsChunking());
    }

    @Test
    void supportsPostGisSourceType() {
        PostGisQuerySourceFactory factory = new PostGisQuerySourceFactory(
                new NamedParameterJdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:postgis_factory2;DB_CLOSE_DELAY=-1")),
                new NoopRuntimeJdbcEndpointResolver());

        Assertions.assertTrue(factory.supports(new PostGisQuerySourceVO()));
    }
}
