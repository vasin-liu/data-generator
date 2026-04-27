package org.gensokyo.data.reader;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class EsReaderAutoConfigurationTests {

    @Test
    void elasticsearchReaderAutoConfigurationLoads() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EsReaderConfig.class))
                .run(context -> assertThat(context).hasNotFailed());
    }
}
