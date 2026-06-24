package org.gensokyo.data.writer;

import org.gensokyo.data.config.DynamicKafkaConfig;
import org.gensokyo.data.datasource.kafka.DynamicKafkaTemplateRegistry;
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
                .withBean(DynamicKafkaTemplateRegistry.class, () -> Mockito.mock(DynamicKafkaTemplateRegistry.class))
                .run(context -> assertThat(context).hasSingleBean(KafkaWriter.class));
    }

    @Test
    void kafkaWriterBacksOffWhenCustomBeanExists() {
        contextRunner
                .withBean(DynamicKafkaTemplateRegistry.class, () -> Mockito.mock(DynamicKafkaTemplateRegistry.class))
                .withBean("customKafkaWriter", KafkaWriter.class, () -> Mockito.mock(KafkaWriter.class))
                .run(context -> assertThat(context).getBeans(KafkaWriter.class).hasSize(1));
    }

    @Test
    void dynamicKafkaRegistryLoadsWithoutInternalStarter() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DynamicKafkaConfig.class, KafkaWriterConfig.class))
                .withPropertyValues(
                        "spring.kafka.multiple.primary=test",
                        "spring.kafka.multiple.clusters.test.bootstrap-servers[0]=localhost:9092",
                        "spring.kafka.multiple.clusters.test.producer.properties.client.dns.lookup=use_all_dns_ips"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DynamicKafkaTemplateRegistry.class);
                    assertThat(context).hasSingleBean(KafkaWriter.class);
                    assertThat(context).hasBean("testKafkaTemplate");
                    assertThat(context.getBean(DynamicKafkaTemplateRegistry.class).hasTemplate("test")).isTrue();
                });
    }
}
