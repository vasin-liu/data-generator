package org.gensokyo.data.calcite;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class Pf4jTemplateV2RuntimePluginProviderTests {

    @Test
    void buildsCompositePluginFromPf4jExtensions() {
        Pf4jTemplateV2RuntimePluginProvider provider = new Pf4jTemplateV2RuntimePluginProvider(
                new StubPf4jRuntimeExtensionLocator(List.of(
                        () -> context -> new TemplateV2RuntimeRegistryFactoryTests.TestSpiRuntimePlugin()
                )));

        TemplateV2RuntimePlugin plugin = provider.createPlugin(TemplateV2RuntimeContext.empty());

        Assertions.assertEquals(1, plugin.sourceFactories().size());
        Assertions.assertEquals(1, plugin.transformFactories().size());
        Assertions.assertEquals(1, plugin.sinkFactories().size());
    }

    @Test
    void exposesPluginClassLoadersFromLocatorContract() {
        ClassLoader loader = new ClassLoader() {
        };
        StubPf4jRuntimeExtensionLocator locator = new StubPf4jRuntimeExtensionLocator(List.of(), List.of(loader));

        Assertions.assertEquals(List.of(loader), locator.pluginClassLoaders());
    }

    private record StubPf4jRuntimeExtensionLocator(List<Pf4jTemplateV2RuntimeExtension> extensions,
                                                   List<ClassLoader> pluginClassLoaders)
            implements Pf4jRuntimeExtensionLocator {
        private StubPf4jRuntimeExtensionLocator(List<Pf4jTemplateV2RuntimeExtension> extensions) {
            this(extensions, List.of());
        }

        @Override
        public List<Pf4jTemplateV2RuntimeExtension> loadExtensions() {
            return extensions;
        }

        @Override
        public List<ClassLoader> pluginClassLoaders() {
            return pluginClassLoaders;
        }
    }
}
