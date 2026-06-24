package org.gensokyo.data.calcite.plugin;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

import java.util.List;

public final class KafkaTemplateTemplateV2RuntimePluginProvider implements TemplateV2RuntimePluginProvider {
    @Override
    public TemplateV2RuntimePlugin createPlugin(TemplateV2RuntimeContext context) {
        if (!context.runtimeServices().hasKafka()) {
            return new TemplateV2RuntimePlugin() {
            };
        }
        return new TemplateV2RuntimePlugin() {
            @Override
            public List<V2SinkFactory> sinkFactories() {
                return List.of(new KafkaSinkFactory(context.runtimeServices()));
            }
        };
    }
}
