package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.sql.TemplateV2SqlFunctionRegistry;

import java.util.List;

public interface TemplateV2RuntimePlugin {
    default TemplateV2RuntimePluginDescriptor descriptor() {
        return TemplateV2RuntimePluginDescriptors.builtIn(getClass().getName());
    }

    default List<V2SourceFactory> sourceFactories() {
        return List.of();
    }

    default List<V2TransformFactory> transformFactories() {
        return List.of();
    }

    default List<V2TransformFactory> transformFactories(TemplateV2SqlFunctionRegistry sqlFunctionRegistry) {
        return transformFactories();
    }

    default List<V2SinkFactory> sinkFactories() {
        return List.of();
    }

    default List<TemplateV2SqlFunction> sqlFunctions() {
        return List.of();
    }
}
