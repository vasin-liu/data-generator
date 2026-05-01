package org.gensokyo.data.calcite;

import org.gensokyo.data.elasticsearch.support.DynamicElasticsearchClientRegistry;
import org.gensokyo.data.kafka.support.DynamicKafkaTemplateRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Map;

class TemplateV2RuntimeServicesTests {

    @Test
    void exposesJdbcKafkaAndElasticsearchServicesThroughContext() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        DynamicKafkaTemplateRegistry kafkaRegistry = new DynamicKafkaTemplateRegistry("primary", Map.of());
        DynamicElasticsearchClientRegistry elasticsearchRegistry =
                new DynamicElasticsearchClientRegistry("primary", Map.of());

        TemplateV2RuntimeContext context = new TemplateV2RuntimeContext(
                new NoopRuntimeJdbcEndpointResolver(),
                new TemplateV2RuntimeServices(jdbcTemplate, kafkaRegistry, elasticsearchRegistry),
                java.util.List.of(),
                getClass().getClassLoader()
        );

        Assertions.assertSame(jdbcTemplate, context.runtimeServices().jdbcTemplate());
        Assertions.assertSame(kafkaRegistry, context.runtimeServices().kafkaTemplateRegistry());
        Assertions.assertSame(elasticsearchRegistry, context.runtimeServices().elasticsearchClientRegistry());
    }

    private DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:runtime_services;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
