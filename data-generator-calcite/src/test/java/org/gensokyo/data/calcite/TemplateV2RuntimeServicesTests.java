package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.codec.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;
import org.gensokyo.data.calcite.support.InMemoryCatalog;
import org.gensokyo.data.datasource.api.CatalogEntrySource;
import org.gensokyo.data.datasource.api.ElasticsearchCatalogMetadata;
import org.gensokyo.data.datasource.api.KafkaCatalogMetadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TemplateV2RuntimeServicesTests {

    @Test
    void exposesJdbcKafkaAndElasticsearchServicesThroughContext() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        InMemoryCatalog catalog = new InMemoryCatalog()
                .putKafka("primary", Mockito.mock(org.springframework.kafka.core.KafkaTemplate.class),
                        CatalogEntrySource.BOOTSTRAP, new KafkaCatalogMetadata("embedded"))
                .putElasticsearch("primary", Mockito.mock(org.elasticsearch.client.RestClient.class),
                        CatalogEntrySource.BOOTSTRAP, new ElasticsearchCatalogMetadata("embedded"));

        TemplateV2RuntimeContext context = new TemplateV2RuntimeContext(
                new NoopRuntimeJdbcEndpointResolver(),
                new TemplateV2RuntimeServices(jdbcTemplate, catalog),
                java.util.List.of(),
                getClass().getClassLoader()
        );

        Assertions.assertSame(jdbcTemplate, context.runtimeServices().jdbcTemplate());
        Assertions.assertSame(catalog, context.runtimeServices().connectionCatalog());
        Assertions.assertTrue(context.runtimeServices().hasKafka());
        Assertions.assertTrue(context.runtimeServices().hasElasticsearch());
    }

    @Test
    void exposesAiRuntimeBridgeThroughContext() {
        AiRuntimeBridge bridge = new AiRuntimeBridge() {
            @Override
            public boolean supports(org.gensokyo.data.model.v2.AiProviderVO provider) {
                return true;
            }

            @Override
            public Object generate(org.gensokyo.data.model.v2.AiSourceVO source) {
                return java.util.List.of();
            }
        };
        TemplateV2RuntimeContext context = new TemplateV2RuntimeContext(
                new NoopRuntimeJdbcEndpointResolver(),
                new TemplateV2RuntimeServices(null, null, bridge),
                java.util.List.of(),
                getClass().getClassLoader()
        );

        Assertions.assertSame(bridge, context.runtimeServices().aiRuntimeBridge());
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
