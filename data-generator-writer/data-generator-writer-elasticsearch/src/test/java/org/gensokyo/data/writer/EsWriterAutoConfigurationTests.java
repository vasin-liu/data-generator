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
    void internalElasticsearchStarterStillTargetsBoot3ElasticsearchProperties() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        MultipleElasticsearchAutoConfiguration.class,
                        MultipleElasticsearchClusterAutoConfiguration.class
                ))
                .withPropertyValues(
                        "spring.elasticsearch.multiple.primary=test",
                        "spring.elasticsearch.multiple.clusters.test.uris[0]=http://localhost:9200"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable startupFailure = context.getStartupFailure();
                    assertThat(startupFailure)
                            .hasRootCauseInstanceOf(ClassNotFoundException.class)
                            .isNotNull();
                    Throwable rootCause = startupFailure;
                    while (rootCause.getCause() != null) {
                        rootCause = rootCause.getCause();
                    }
                    assertThat(rootCause)
                            .isInstanceOf(ClassNotFoundException.class)
                            .hasMessageContaining("org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties");
                });
    }
}
