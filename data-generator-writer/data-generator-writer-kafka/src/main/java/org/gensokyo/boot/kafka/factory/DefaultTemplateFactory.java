package org.gensokyo.boot.kafka.factory;

import org.gensokyo.boot.kafka.config.DefaultMultipleKafkaProducerFactoryCustomizer;
import org.gensokyo.boot.kafka.config.MultipleKafkaProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.LoggingProducerListener;

import java.util.Collections;
import java.util.List;

/**
 * Boot 4 compatible replacement for the upstream Boot 3 starter factory.
 * The original implementation depends on a PropertyMapper method that no
 * longer exists in Boot 4.
 */
public class DefaultTemplateFactory<K, V> implements TemplateFactory<K, V> {

    private final ObjectProvider<List<DefaultMultipleKafkaProducerFactoryCustomizer>> customizers;

    public DefaultTemplateFactory(ObjectProvider<List<DefaultMultipleKafkaProducerFactoryCustomizer>> customizers) {
        this.customizers = customizers;
    }

    @Override
    public KafkaTemplate<K, V> build(String cluster, MultipleKafkaProperties properties) {
        DefaultKafkaProducerFactory<K, V> producerFactory =
                new DefaultKafkaProducerFactory<>(properties.buildProducerProperties());

        for (DefaultMultipleKafkaProducerFactoryCustomizer customizer : this.customizers.getIfAvailable(Collections::emptyList)) {
            customizer.customize(producerFactory);
        }

        KafkaTemplate<K, V> template = new KafkaTemplate<>(producerFactory);
        template.setProducerListener(new LoggingProducerListener<>());

        String defaultTopic = properties.getTemplate().getDefaultTopic();
        if (defaultTopic != null && !defaultTopic.isBlank()) {
            template.setDefaultTopic(defaultTopic);
        }

        String transactionIdPrefix = properties.getTemplate().getTransactionIdPrefix();
        if (transactionIdPrefix != null && !transactionIdPrefix.isBlank()) {
            template.setTransactionIdPrefix(transactionIdPrefix);
        }

        return template;
    }
}
