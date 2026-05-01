package org.example.datagenerator.plugin.sample;

import org.gensokyo.data.calcite.Pf4jTemplateV2RuntimeExtension;
import org.gensokyo.data.calcite.TemplateV2PluginCapability;
import org.gensokyo.data.calcite.TemplateV2RuntimePlugin;
import org.gensokyo.data.calcite.TemplateV2RuntimePluginDescriptor;
import org.gensokyo.data.calcite.TemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.V2SinkFactory;
import org.pf4j.Extension;

import java.util.List;

@Extension
public class SampleTemplateV2Extension implements Pf4jTemplateV2RuntimeExtension {
    @Override
    public TemplateV2RuntimePluginProvider provider() {
        return context -> new TemplateV2RuntimePlugin() {
            @Override
            public TemplateV2RuntimePluginDescriptor descriptor() {
                return TemplateV2RuntimePluginDescriptor.builder("sample-template-v2-plugin")
                        .version("1.0.0-SNAPSHOT")
                        .hostVersionRange("current")
                        .provider("example")
                        .capability(TemplateV2PluginCapability.sink("sample_logging"))
                        .build();
            }

            @Override
            public List<V2SinkFactory> sinkFactories() {
                return List.of(new SampleLoggingSinkFactory());
            }
        };
    }
}
