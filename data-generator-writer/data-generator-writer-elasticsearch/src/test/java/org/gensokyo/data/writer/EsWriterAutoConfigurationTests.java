package org.gensokyo.data.writer;

import org.gensokyo.data.config.DynamicElasticsearchConfig;
import org.gensokyo.data.elasticsearch.support.DynamicElasticsearchClientRegistry;
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
                .withBean(DynamicElasticsearchClientRegistry.class, () -> Mockito.mock(DynamicElasticsearchClientRegistry.class))
                .run(context -> assertThat(context).hasSingleBean(ElasticsearchWriter.class));
    }

    @Test
    void elasticsearchWriterBacksOffWhenCustomBeanExists() {
        contextRunner
                .withBean(DynamicElasticsearchClientRegistry.class, () -> Mockito.mock(DynamicElasticsearchClientRegistry.class))
                .withBean("customElasticsearchWriter", ElasticsearchWriter.class, () -> Mockito.mock(ElasticsearchWriter.class))
                .run(context -> assertThat(context).getBeans(ElasticsearchWriter.class).hasSize(1));
    }

    @Test
    void dynamicElasticsearchRegistryLoadsWithoutInternalStarter() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DynamicElasticsearchConfig.class,
                        EsWriterConfig.class
                ))
                .withPropertyValues(
                        "spring.elasticsearch.multiple.primary=test",
                        "spring.elasticsearch.multiple.clusters.test.uris[0]=http://localhost:9200",
                        "spring.elasticsearch.multiple.clusters.test.socket-timeout=5s"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DynamicElasticsearchClientRegistry.class);
                    assertThat(context).hasSingleBean(ElasticsearchWriter.class);
                    assertThat(context).hasBean("testRestClient");
                    assertThat(context).doesNotHaveBean("testRestHighLevelClient");
                    assertThat(context.getBean(DynamicElasticsearchClientRegistry.class).hasCluster("test")).isTrue();
                });
    }
}
