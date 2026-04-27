package org.gensokyo.data.writer;

import org.gensokyo.boot.kafka.MultipleKafkaAutoConfiguration;
import org.gensokyo.boot.kafka.support.MultipleKafkaTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaWriterAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KafkaWriterConfig.class));

    @Test
    void kafkaWriterIsRegisteredWhenKafkaTemplateExists() {
        contextRunner
                .withBean(MultipleKafkaTemplate.class, () -> Mockito.mock(MultipleKafkaTemplate.class))
                .run(context -> assertThat(context).hasSingleBean(KafkaWriter.class));
    }

    @Test
    void kafkaWriterBacksOffWhenCustomBeanExists() {
        contextRunner
                .withBean(MultipleKafkaTemplate.class, () -> Mockito.mock(MultipleKafkaTemplate.class))
                .withBean("customKafkaWriter", KafkaWriter.class, () -> Mockito.mock(KafkaWriter.class))
                .run(context -> assertThat(context).getBeans(KafkaWriter.class).hasSize(1));
    }

    @Test
    void internalKafkaStarterStillTargetsBoot3KafkaProperties() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MultipleKafkaAutoConfiguration.class))
                .withPropertyValues(
                        "spring.kafka.multiple.primary=test",
                        "spring.kafka.multiple.clusters.test.bootstrap-servers[0]=localhost:9092"
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
                            .hasMessageContaining("org.springframework.boot.autoconfigure.kafka.KafkaProperties");
                });
    }
}
