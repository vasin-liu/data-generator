package org.gensokyo.data.config;

import org.gensokyo.data.cache.Templates;
import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.plugin.Pf4jRuntimeExtensionLocator;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeContext;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistry;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeServices;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class TemplateV2RuntimeWatcherTests {

    @Test
    void refreshesRegistryWhenPluginJarChanges() throws Exception {
        Path directory = Files.createTempDirectory("v2-plugin-watch");
        try {
            CountingRegistryProvider registryProvider = new CountingRegistryProvider();
            CountingSubtypeRegistrar subtypeRegistrar = new CountingSubtypeRegistrar();
            CountingTemplates templates = new CountingTemplates();
            TemplateV2RuntimeWatcher watcher = new TemplateV2RuntimeWatcher(
                    new TemplateV2RuntimeContext(new NoopRuntimeJdbcEndpointResolver(), new TemplateV2RuntimeServices(null, null), List.of(directory),
                            getClass().getClassLoader()),
                    registryProvider,
                    new FixedObjectProvider<>(subtypeRegistrar),
                    new FixedObjectProvider<>(templates)
            );

            watcher.start();
            Files.writeString(directory.resolve("demo-plugin.jar"), "changed");

            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while ((registryProvider.refreshCount.get() == 0
                    || subtypeRegistrar.refreshCount.get() == 0
                    || templates.reloadCount.get() == 0)
                    && System.nanoTime() < deadline) {
                Thread.sleep(50);
            }

            watcher.stop();
            Assertions.assertTrue(registryProvider.refreshCount.get() > 0);
            Assertions.assertTrue(subtypeRegistrar.refreshCount.get() > 0);
            Assertions.assertTrue(templates.reloadCount.get() > 0);
        } finally {
            try (var stream = Files.list(directory)) {
                stream.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
            }
            Files.deleteIfExists(directory);
        }
    }

    private static final class CountingSubtypeRegistrar extends TemplateModelSubtypeRegistrar {
        private final AtomicInteger refreshCount = new AtomicInteger();

        private CountingSubtypeRegistrar() {
            super(new EmptyPf4jRuntimeExtensionLocator());
        }

        @Override
        public void refresh() {
            refreshCount.incrementAndGet();
        }
    }

    private static final class CountingTemplates extends Templates {
        private final AtomicInteger reloadCount = new AtomicInteger();

        private CountingTemplates() {
            super(new DataGeneratorProperties(), null, null);
        }

        @Override
        public java.util.List<org.gensokyo.data.model.po.TemplatePO> reloadAll() {
            reloadCount.incrementAndGet();
            return List.of();
        }
    }

    private static final class CountingRegistryProvider implements TemplateV2RuntimeRegistryProvider {
        private final AtomicInteger refreshCount = new AtomicInteger();

        @Override
        public TemplateV2RuntimeRegistry current() {
            return new TemplateV2RuntimeRegistry(List.of(), List.of(), List.of());
        }

        @Override
        public TemplateV2RuntimeRegistry refresh() {
            refreshCount.incrementAndGet();
            return current();
        }
    }

    private record FixedObjectProvider<T>(T value) implements ObjectProvider<T> {
        @Override
        public T getObject(Object... args) {
            return value;
        }

        @Override
        public T getIfAvailable() {
            return value;
        }

        @Override
        public T getIfUnique() {
            return value;
        }

        @Override
        public T getObject() {
            return value;
        }
    }

    private static final class EmptyPf4jRuntimeExtensionLocator implements Pf4jRuntimeExtensionLocator {
        @Override
        public List<org.gensokyo.data.calcite.Pf4jTemplateV2RuntimeExtension> loadExtensions() {
            return List.of();
        }
    }
}
