package org.gensokyo.boot.elasticsearch.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClientBuilder;

/**
 * Boot 4 compatible replacement for the upstream Boot 3 starter customizer.
 * The original implementation depends on a PropertyMapper API removed in Boot 4.
 */
public class DefaultMultipleRestClientBuilderCustomizer implements MultipleRestClientBuilderCustomizer {

    private final String cluster;
    private final MultipleElasticsearchProperties properties;

    public DefaultMultipleRestClientBuilderCustomizer(String cluster, MultipleElasticsearchProperties properties) {
        this.cluster = cluster;
        this.properties = properties;
    }

    @Override
    public void customize(RestClientBuilder builder) {
    }

    @Override
    public void customize(HttpAsyncClientBuilder builder) {
        builder.setDefaultCredentialsProvider(new MultiplePropertiesCredentialsProvider(this.properties));
    }

    @Override
    public void customize(RequestConfig.Builder builder) {
        if (this.properties.getConnectionTimeout() != null) {
            builder.setConnectTimeout(Math.toIntExact(this.properties.getConnectionTimeout().toMillis()));
        }
        if (this.properties.getSocketTimeout() != null) {
            builder.setSocketTimeout(Math.toIntExact(this.properties.getSocketTimeout().toMillis()));
        }
    }

    @Override
    public Scope scope() {
        return Scope.SINGLE;
    }

    @Override
    public String cluster() {
        return this.cluster;
    }
}
