package org.gensokyo.data.config;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.gensokyo.data.elasticsearch.config.MultipleElasticsearchClusterProperties;
import org.gensokyo.data.elasticsearch.config.MultipleElasticsearchClusterProperties.ElasticsearchClusterProperties;
import org.gensokyo.data.datasource.elasticsearch.DynamicElasticsearchClientRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(MultipleElasticsearchClusterProperties.class)
public class DynamicElasticsearchConfig {

    @Bean
    @ConditionalOnMissingBean(DynamicElasticsearchClientRegistry.class)
    public DynamicElasticsearchClientRegistry dynamicElasticsearchClientRegistry(MultipleElasticsearchClusterProperties properties,
                                                                                 DefaultListableBeanFactory beanFactory) {
        Map<String, RestClient> lowLevelClients = new LinkedHashMap<>();

        properties.getClusters().forEach((cluster, clusterProperties) -> {
            RestClient lowLevelClient = buildLowLevelClient(clusterProperties);
            lowLevelClients.put(cluster, lowLevelClient);
            registerSingleton(beanFactory, cluster + "RestClient", lowLevelClient);
        });

        return new DynamicElasticsearchClientRegistry(properties.getPrimary(), lowLevelClients);
    }

    private static RestClient buildLowLevelClient(ElasticsearchClusterProperties properties) {
        return createBuilder(properties).build();
    }

    private static RestClientBuilder createBuilder(ElasticsearchClusterProperties properties) {
        HttpHost[] hosts = properties.getUris().stream()
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);
        RestClientBuilder builder = RestClient.builder(hosts);

        if (StringUtils.hasText(properties.getPathPrefix())) {
            builder.setPathPrefix(properties.getPathPrefix());
        }

        builder.setRequestConfigCallback(requestBuilder -> {
            if (properties.getConnectionTimeout() != null) {
                requestBuilder.setConnectTimeout(Math.toIntExact(properties.getConnectionTimeout().toMillis()));
            }
            if (properties.getSocketTimeout() != null) {
                requestBuilder.setSocketTimeout(Math.toIntExact(properties.getSocketTimeout().toMillis()));
            }
            return requestBuilder;
        });

        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            if (StringUtils.hasText(properties.getUsername())) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            if (StringUtils.hasText(properties.getApiKey())) {
                httpClientBuilder.setDefaultHeaders(List.of(
                        (Header) new BasicHeader(HttpHeaders.AUTHORIZATION, "ApiKey " + encodeApiKey(properties.getApiKey()))
                ));
            }
            return httpClientBuilder;
        });
        return builder;
    }

    private static String encodeApiKey(String apiKey) {
        return Base64.getEncoder().encodeToString(apiKey.getBytes(StandardCharsets.UTF_8));
    }

    private static void registerSingleton(DefaultListableBeanFactory beanFactory, String beanName, Object bean) {
        if (!beanFactory.containsSingleton(beanName)) {
            beanFactory.registerSingleton(beanName, bean);
        }
    }
}
