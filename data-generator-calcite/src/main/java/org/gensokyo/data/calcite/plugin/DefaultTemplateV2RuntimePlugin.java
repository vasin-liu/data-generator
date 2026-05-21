package org.gensokyo.data.calcite.plugin;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

import java.util.List;

public class DefaultTemplateV2RuntimePlugin implements TemplateV2RuntimePlugin {
    @Override
    public TemplateV2RuntimePluginDescriptor descriptor() {
        return TemplateV2RuntimePluginDescriptor.builder("builtin-default")
                .version("builtin")
                .hostVersionRange("current")
                .provider("gensokyo")
                .capability(TemplateV2PluginCapability.source("iterator"))
                .capability(TemplateV2PluginCapability.source("ai"))
                .capability(TemplateV2PluginCapability.source("csv"))
                .capability(TemplateV2PluginCapability.source("excel"))
                .capability(TemplateV2PluginCapability.source("json"))
                .capability(TemplateV2PluginCapability.source("geojson"))
                .capability(TemplateV2PluginCapability.transform("sql"))
                .capability(TemplateV2PluginCapability.sink("console"))
                .capability(TemplateV2PluginCapability.sink("csv"))
                .capability(TemplateV2PluginCapability.sink("excel"))
                .capability(TemplateV2PluginCapability.sink("json"))
                .build();
    }

    @Override
    public List<V2SourceFactory> sourceFactories() {
        return List.of(
                new IteratorSourceFactory(),
                new AiSourceFactory(),
                new CsvSourceFactory(),
                new ExcelSourceFactory(),
                new JsonSourceFactory(),
                new GeoJsonSourceFactory()
        );
    }

    @Override
    public List<V2TransformFactory> transformFactories(TemplateV2SqlFunctionRegistry sqlFunctionRegistry) {
        return List.of(new SqlTransformFactory(sqlFunctionRegistry));
    }

    @Override
    public List<V2SinkFactory> sinkFactories() {
        return List.of(new ConsoleSinkFactory(), new CsvSinkFactory(), new ExcelSinkFactory(), new JsonSinkFactory());
    }
}
