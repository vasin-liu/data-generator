package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

import org.elasticsearch.client.RestClient;
import org.gensokyo.data.datasource.elasticsearch.DynamicElasticsearchClientRegistry;
import org.gensokyo.data.datasource.kafka.DynamicKafkaTemplateRegistry;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

public record TemplateV2RuntimeServices(NamedParameterJdbcTemplate jdbcTemplate,
                                        DynamicKafkaTemplateRegistry kafkaTemplateRegistry,
                                        DynamicElasticsearchClientRegistry elasticsearchClientRegistry,
                                        AiRuntimeBridge aiRuntimeBridge) {

    public TemplateV2RuntimeServices(NamedParameterJdbcTemplate jdbcTemplate,
                                     DynamicKafkaTemplateRegistry kafkaTemplateRegistry,
                                     DynamicElasticsearchClientRegistry elasticsearchClientRegistry) {
        this(jdbcTemplate, kafkaTemplateRegistry, elasticsearchClientRegistry, null);
    }

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
