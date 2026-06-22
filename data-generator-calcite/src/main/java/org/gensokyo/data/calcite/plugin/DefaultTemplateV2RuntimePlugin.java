package org.gensokyo.data.calcite.plugin;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.SpelTransformFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.calcite.sql.TemplateV2SqlFunctionRegistry;
import org.gensokyo.data.calcite.transform.JsTransformFactory;
import org.gensokyo.data.calcite.transform.JsonTransformFactory;
import org.gensokyo.data.calcite.transform.LookupTransformFactory;
import org.gensokyo.data.calcite.transform.MaskTransformFactory;

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
                .capability(TemplateV2PluginCapability.source("inline_rows"))
                .capability(TemplateV2PluginCapability.transform("sql"))
                .capability(TemplateV2PluginCapability.transform("spel"))
                .capability(TemplateV2PluginCapability.transform("js"))
                .capability(TemplateV2PluginCapability.transform("json"))
                .capability(TemplateV2PluginCapability.transform("mask"))
                .capability(TemplateV2PluginCapability.transform("lookup"))
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
                new GeoJsonSourceFactory(),
                new InlineRowsSourceFactory()
        );
    }

    @Override
    public List<V2TransformFactory> transformFactories(TemplateV2SqlFunctionRegistry sqlFunctionRegistry) {
        return List.of(new SqlTransformFactory(sqlFunctionRegistry), new SpelTransformFactory(), new JsTransformFactory(),
                new JsonTransformFactory(), new MaskTransformFactory(), new LookupTransformFactory());
    }

    @Override
    public List<V2SinkFactory> sinkFactories() {
        return List.of(new ConsoleSinkFactory(), new CsvSinkFactory(), new ExcelSinkFactory(), new JsonSinkFactory());
    }
}
