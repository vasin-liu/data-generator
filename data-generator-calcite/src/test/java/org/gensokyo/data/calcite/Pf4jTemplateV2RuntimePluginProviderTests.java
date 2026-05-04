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
        Assertions.assertEquals(1, plugin.transformFactories(TemplateV2SqlFunctionRegistry.builtIn()).size());
        Assertions.assertEquals(1, plugin.sinkFactories().size());
    }

    @Test
    void exposesPluginClassLoadersFromLocatorContract() {
        ClassLoader loader = new ClassLoader() {
        };
        StubPf4jRuntimeExtensionLocator locator = new StubPf4jRuntimeExtensionLocator(List.of(), List.of(loader));

        Assertions.assertEquals(List.of(loader), locator.pluginClassLoaders());
    }

    @Test
    void startFailureDiagnosticsIncludeLocatorClassAndPhase() {
        Pf4jTemplateV2RuntimePluginProvider provider = new Pf4jTemplateV2RuntimePluginProvider(
                new FailingStartPf4jRuntimeExtensionLocator());

        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class,
                () -> provider.createPlugin(TemplateV2RuntimeContext.empty()));

        Assertions.assertTrue(failure.getMessage().contains("Failed to start PF4J Template V2 extension locator"));
        Assertions.assertTrue(failure.getMessage().contains(FailingStartPf4jRuntimeExtensionLocator.class.getName()));
        Assertions.assertEquals("pf4j start boom", failure.getCause().getMessage());
    }

    @Test
    void loadFailureDiagnosticsIncludeLocatorClassAndPhase() {
        Pf4jTemplateV2RuntimePluginProvider provider = new Pf4jTemplateV2RuntimePluginProvider(
                new FailingLoadPf4jRuntimeExtensionLocator());

        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class,
                () -> provider.createPlugin(TemplateV2RuntimeContext.empty()));

        Assertions.assertTrue(failure.getMessage().contains("Failed to load PF4J Template V2 extensions from locator"));
        Assertions.assertTrue(failure.getMessage().contains(FailingLoadPf4jRuntimeExtensionLocator.class.getName()));
        Assertions.assertEquals("pf4j load boom", failure.getCause().getMessage());
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

    private static final class FailingStartPf4jRuntimeExtensionLocator implements Pf4jRuntimeExtensionLocator {
        @Override
        public void start() {
            throw new IllegalStateException("pf4j start boom");
        }

        @Override
        public List<Pf4jTemplateV2RuntimeExtension> loadExtensions() {
            return List.of();
        }
    }

    private static final class FailingLoadPf4jRuntimeExtensionLocator implements Pf4jRuntimeExtensionLocator {
        @Override
        public List<Pf4jTemplateV2RuntimeExtension> loadExtensions() {
            throw new IllegalStateException("pf4j load boom");
        }
    }
}
