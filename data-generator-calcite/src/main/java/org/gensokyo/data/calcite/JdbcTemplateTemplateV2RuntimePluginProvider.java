package org.gensokyo.data.calcite;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

public final class JdbcTemplateTemplateV2RuntimePluginProvider implements TemplateV2RuntimePluginProvider {

    @Override
    public TemplateV2RuntimePlugin createPlugin(TemplateV2RuntimeContext context) {
        NamedParameterJdbcTemplate jdbcTemplate = context.runtimeServices().jdbcTemplate();
        if (jdbcTemplate == null) {
            return new TemplateV2RuntimePlugin() {
            };
        }
        return new TemplateV2RuntimePlugin() {
            @Override
            public List<V2SourceFactory> sourceFactories() {
                return List.of(new QuerySourceFactory(jdbcTemplate, context.runtimeJdbcEndpointResolver()));
            }

            @Override
            public List<V2SinkFactory> sinkFactories() {
                return List.of(new JdbcSinkFactory(jdbcTemplate, context.runtimeJdbcEndpointResolver()));
            }
        };
    }
}
