package org.gensokyo.data.config;

import org.gensokyo.data.kafka.config.MultipleKafkaClusterProperties;
import org.gensokyo.data.datasource.kafka.DynamicKafkaTemplateRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(MultipleKafkaClusterProperties.class)
public class DynamicKafkaConfig {

    @Bean
    @ConditionalOnMissingBean(DynamicKafkaTemplateRegistry.class)
    public DynamicKafkaTemplateRegistry dynamicKafkaTemplateRegistry(MultipleKafkaClusterProperties properties,
                                                                    DefaultListableBeanFactory beanFactory) {
        Map<String, KafkaTemplate<String, String>> templates = new LinkedHashMap<>();
        properties.getClusters().forEach((cluster, clusterProperties) -> {
            KafkaTemplate<String, String> template = new KafkaTemplate<>(
                    new DefaultKafkaProducerFactory<>(clusterProperties.buildProducerProperties()));
            templates.put(cluster, template);
            registerSingleton(beanFactory, cluster + "KafkaTemplate", template);
        });
        return new DynamicKafkaTemplateRegistry(properties.getPrimary(), templates);
    }

    private static void registerSingleton(DefaultListableBeanFactory beanFactory, String beanName, Object bean) {
        if (!beanFactory.containsSingleton(beanName)) {
            beanFactory.registerSingleton(beanName, bean);
        }
    }
}
