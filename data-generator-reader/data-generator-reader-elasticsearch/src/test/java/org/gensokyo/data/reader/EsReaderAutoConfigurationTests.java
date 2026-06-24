package org.gensokyo.data.reader;

import org.gensokyo.data.config.DynamicElasticsearchConfig;
import org.gensokyo.data.datasource.elasticsearch.DynamicElasticsearchClientRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class EsReaderAutoConfigurationTests {

    @Test
    void elasticsearchReaderAutoConfigurationLoads() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DynamicElasticsearchConfig.class, EsReaderConfig.class))
                .withPropertyValues(
                        "spring.elasticsearch.multiple.primary=test",
                        "spring.elasticsearch.multiple.clusters.test.uris[0]=http://localhost:9200"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DynamicElasticsearchClientRegistry.class);
                });
    }
}
