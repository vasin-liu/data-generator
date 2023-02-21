/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClientBuilder;
import org.gensokyo.boot.elasticsearch.config.MultipleElasticsearchProperties;
import org.gensokyo.boot.elasticsearch.config.MultiplePropertiesCredentialsProvider;
import org.gensokyo.boot.elasticsearch.config.MultipleRestClientBuilderCustomizer;

/**
 * @author Gensokyo V.L.
 * @since 2023/1/14 , Version
 */
@Slf4j
public class BasicCertificateMultipleRestClientBuilderCustomizer implements MultipleRestClientBuilderCustomizer {
    private final String cluster;
    private final MultipleElasticsearchProperties props;

    public BasicCertificateMultipleRestClientBuilderCustomizer(String cluster, MultipleElasticsearchProperties props) {
        this.cluster = cluster;
        this.props = props;
    }

    @Override
    public void customize(RestClientBuilder builder) {
        try {
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.disableAuthCaching()
                    .setDefaultCredentialsProvider(new MultiplePropertiesCredentialsProvider(this.props)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Scope scope() {
        return Scope.SINGLE;
    }

    @Override
    public String cluster() {
        return cluster;
    }
}
