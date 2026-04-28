package org.gensokyo.data.writer;

import org.gensokyo.boot.elasticsearch.MultipleElasticsearchAutoConfiguration;
import org.gensokyo.boot.elasticsearch.config.MultipleElasticsearchClusterAutoConfiguration;
import org.gensokyo.boot.elasticsearch.support.MultipleElasticsearchRestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class EsWriterAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EsWriterConfig.class));

    @Test
    void elasticsearchWriterIsRegisteredWhenClientExists() {
        contextRunner
                .withBean(MultipleElasticsearchRestClient.class, () -> Mockito.mock(MultipleElasticsearchRestClient.class))
                .run(context -> assertThat(context).hasSingleBean(ElasticsearchWriter.class));
    }

    @Test
    void elasticsearchWriterBacksOffWhenCustomBeanExists() {
        contextRunner
                .withBean(MultipleElasticsearchRestClient.class, () -> Mockito.mock(MultipleElasticsearchRestClient.class))
                .withBean("customElasticsearchWriter", ElasticsearchWriter.class, () -> Mockito.mock(ElasticsearchWriter.class))
                .run(context -> assertThat(context).getBeans(ElasticsearchWriter.class).hasSize(1));
    }

    @Test
    void internalElasticsearchStarterLoadsThroughBoot4CompatibilityBridge() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        MultipleElasticsearchAutoConfiguration.class,
                        MultipleElasticsearchClusterAutoConfiguration.class,
                        EsWriterConfig.class
                ))
                .withPropertyValues(
                        "spring.elasticsearch.multiple.primary=test",
                        "spring.elasticsearch.multiple.clusters.test.uris[0]=http://localhost:9200",
                        "spring.elasticsearch.multiple.clusters.test.socket-timeout=5s"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(MultipleElasticsearchRestClient.class);
                    assertThat(context).hasSingleBean(ElasticsearchWriter.class);
                    assertThat(context).hasBean("testRestClient");
                    assertThat(context).hasBean("testRestHighLevelClient");
                });
    }
}
