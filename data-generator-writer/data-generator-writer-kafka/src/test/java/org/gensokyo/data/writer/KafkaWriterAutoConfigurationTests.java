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
    void internalKafkaStarterLoadsThroughBoot4CompatibilityBridge() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MultipleKafkaAutoConfiguration.class, KafkaWriterConfig.class))
                .withPropertyValues(
                        "spring.kafka.multiple.primary=test",
                        "spring.kafka.multiple.clusters.test.bootstrap-servers[0]=localhost:9092",
                        "spring.kafka.multiple.clusters.test.producer.properties.client.dns.lookup=use_all_dns_ips"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(MultipleKafkaTemplate.class);
                    assertThat(context).hasSingleBean(KafkaWriter.class);
                    assertThat(context).hasBean("testKafkaTemplate");
                });
    }
}
