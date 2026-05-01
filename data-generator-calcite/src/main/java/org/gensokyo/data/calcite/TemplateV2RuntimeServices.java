package org.gensokyo.data.calcite;

import org.elasticsearch.client.RestClient;
import org.gensokyo.data.elasticsearch.support.DynamicElasticsearchClientRegistry;
import org.gensokyo.data.kafka.support.DynamicKafkaTemplateRegistry;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

public record TemplateV2RuntimeServices(NamedParameterJdbcTemplate jdbcTemplate,
                                        DynamicKafkaTemplateRegistry kafkaTemplateRegistry,
                                        DynamicElasticsearchClientRegistry elasticsearchClientRegistry) {

    public KafkaTemplate<String, String> kafkaTemplate(String cluster) {
        if (kafkaTemplateRegistry == null) {
            throw new IllegalStateException("Kafka runtime service is not configured");
        }
        return kafkaTemplateRegistry.template(cluster);
    }

    public RestClient elasticsearchClient(String cluster) {
        if (elasticsearchClientRegistry == null) {
            throw new IllegalStateException("Elasticsearch runtime service is not configured");
        }
        return elasticsearchClientRegistry.llc(cluster);
    }
}
