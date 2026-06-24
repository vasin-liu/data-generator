package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.codec.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcTemplateTemplateV2RuntimePluginProviderTests {

    @Test
    void createsJdbcAwareFactoriesFromRuntimeContext() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        RuntimeJdbcEndpointResolver resolver = new RuntimeJdbcEndpointResolver() {
            @Override
            public String resolveSourceDataSourceId(QuerySourceVO source) {
                return "resolved-source";
            }

            @Override
            public String resolveSinkDataSourceId(JdbcWriterVO writer) {
                return "resolved-sink";
            }
        };

        TemplateV2RuntimePlugin plugin = new JdbcTemplateTemplateV2RuntimePluginProvider()
                .createPlugin(new TemplateV2RuntimeContext(
                        resolver,
                        new TemplateV2RuntimeServices(jdbcTemplate, null),
                        java.util.List.of(),
                        getClass().getClassLoader()
                ));

        Assertions.assertEquals(2, plugin.sourceFactories().size());
        Assertions.assertEquals(1, plugin.sinkFactories().size());
        Assertions.assertTrue(
                plugin.sourceFactories().stream().anyMatch(QuerySourceFactory.class::isInstance));
        Assertions.assertTrue(
                plugin.sourceFactories().stream().anyMatch(PostGisQuerySourceFactory.class::isInstance));
        Assertions.assertTrue(plugin.sinkFactories().getFirst() instanceof JdbcSinkFactory);
    }

    private DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:jdbc_plugin_provider;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
