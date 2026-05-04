package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

class TemplateV2ExternalPluginIsolationTests {

    @Test
    void serviceLoaderPluginsDoNotProvideTruePluginIsolation() throws Exception {
        Path pluginDirectory = Files.createTempDirectory("v2-service-loader-plugins");
        try {
            createServiceLoaderPluginJar(pluginDirectory, "pluginA", "source_a");
            createServiceLoaderPluginJar(pluginDirectory, "pluginB", "source_b");

            Assertions.assertThrows(TemplateV2RuntimePluginContractException.class,
                    () -> new DirectoryAwareTemplateV2RuntimePluginProvider().createPlugin(runtimeContext(pluginDirectory)));
        } finally {
            deleteRecursively(pluginDirectory);
        }
    }

    @Test
    void pf4jPluginsUseDifferentClassLoaders() throws Exception {
        Path pluginDirectory = Files.createTempDirectory("v2-pf4j-plugins");
        try {
            createPf4jPluginJar(pluginDirectory, "pf4jPluginA", "source_a");
            createPf4jPluginJar(pluginDirectory, "pf4jPluginB", "source_b");

            try (Pf4jTemplateV2RuntimePluginProvider provider =
                         new Pf4jTemplateV2RuntimePluginProvider(new PathBasedPf4jRuntimeExtensionLocator(List.of(pluginDirectory)))) {
                TemplateV2RuntimePlugin plugin = provider.createPlugin(runtimeContext(pluginDirectory));
                List<V2SourceFactory> sourceFactories = plugin.sourceFactories();
                Assertions.assertEquals(2, sourceFactories.size());
                Assertions.assertNotSame(sourceFactories.get(0).getClass().getClassLoader(),
                        sourceFactories.get(1).getClass().getClassLoader());
            }
        } finally {
            deleteRecursively(pluginDirectory);
        }
    }

    @Test
    void pf4jPluginCanContributeSqlUdfToBuiltInSqlTransform() throws Exception {
        Path pluginDirectory = Files.createTempDirectory("v2-pf4j-udf-plugin");
        try {
            createPf4jUdfPluginJar(pluginDirectory, "pf4jUdfPlugin");

            try (Pf4jTemplateV2RuntimePluginProvider provider =
                         new Pf4jTemplateV2RuntimePluginProvider(new PathBasedPf4jRuntimeExtensionLocator(List.of(pluginDirectory)))) {
                TemplateV2RuntimeRegistry registry = new TemplateV2RuntimeRegistryFactory().fromProviders(List.of(
                        new StaticTemplateV2RuntimePluginProvider(new DefaultTemplateV2RuntimePlugin()),
                        provider
                ), runtimeContext(pluginDirectory));

                TemplateV2VO template = new TemplateV2VO();
                template.setName("pf4j-udf-template");
                template.setSources(Map.of("seed", iteratorSource()));
                template.setTransformers(List.of(sql("SELECT V2_PF4J_WRAP('pf4j', 'ok') AS wrapped FROM seed")));
                WriteStageVO sink = new WriteStageVO();
                sink.setWriters(List.of(new ConsoleWriterVO()));
                template.setSinks(List.of(sink));

                TemplateV2RunResult result = new TemplateV2Runner(registry).run(template);

                Assertions.assertFalse(result.getRows().isEmpty());
                Assertions.assertEquals("pf4j-ok", result.getRows().getFirst().getString("wrapped"));
            }
        } finally {
            deleteRecursively(pluginDirectory);
        }
    }

    @Test
    void pf4jDuplicateSqlUdfDiagnosticsIncludePluginIds() throws Exception {
        Path pluginDirectory = Files.createTempDirectory("v2-pf4j-duplicate-udf-plugins");
        try {
            createPf4jUdfPluginJar(pluginDirectory, "pf4jUdfA");
            createPf4jUdfPluginJar(pluginDirectory, "pf4jUdfB");

            try (Pf4jTemplateV2RuntimePluginProvider provider =
                         new Pf4jTemplateV2RuntimePluginProvider(new PathBasedPf4jRuntimeExtensionLocator(List.of(pluginDirectory)))) {
                TemplateV2RuntimeRegistryBuildException failure = Assertions.assertThrows(
                        TemplateV2RuntimeRegistryBuildException.class,
                        () -> new TemplateV2RuntimeRegistryFactory().fromProviders(List.of(provider), runtimeContext(pluginDirectory))
                );

                String diagnostics = diagnostics(failure);
                Assertions.assertTrue(diagnostics.contains("Duplicate Template V2 SQL function [V2_PF4J_WRAP]"));
                Assertions.assertTrue(diagnostics.contains("pf4jUdfA"));
                Assertions.assertTrue(diagnostics.contains("pf4jUdfB"));
            }
        } finally {
            deleteRecursively(pluginDirectory);
        }
    }

    @Test
    void pf4jNullProviderDiagnosticsIncludeExtensionClass() throws Exception {
        Path pluginDirectory = Files.createTempDirectory("v2-pf4j-null-provider-plugin");
        try {
            createPf4jNullProviderPluginJar(pluginDirectory, "pf4jNullProvider");

            try (Pf4jTemplateV2RuntimePluginProvider provider =
                         new Pf4jTemplateV2RuntimePluginProvider(new PathBasedPf4jRuntimeExtensionLocator(List.of(pluginDirectory)))) {
                TemplateV2RuntimeRegistryBuildException failure = Assertions.assertThrows(
                        TemplateV2RuntimeRegistryBuildException.class,
                        () -> new TemplateV2RuntimeRegistryFactory().fromProviders(List.of(provider), runtimeContext(pluginDirectory))
                );

                Assertions.assertTrue(failure.getMessage().contains("provider [org.gensokyo.data.calcite.Pf4jTemplateV2RuntimePluginProvider]"));
                Assertions.assertTrue(failure.getCause().getMessage().contains("Pf4jNullProviderExtension"));
                Assertions.assertTrue(failure.getCause().getMessage().contains("must return a plugin provider"));
            }
        } finally {
            deleteRecursively(pluginDirectory);
        }
    }

    private TemplateV2RuntimeContext runtimeContext(Path pluginDirectory) {
        return new TemplateV2RuntimeContext(
                new NoopRuntimeJdbcEndpointResolver(),
                new TemplateV2RuntimeServices(null, null, null),
                List.of(pluginDirectory),
                getClass().getClassLoader()
        );
    }

    private String diagnostics(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            builder.append(current.getMessage()).append('\n');
            current = current.getCause();
        }
        return builder.toString();
    }

    private void createServiceLoaderPluginJar(Path pluginDirectory, String pluginId, String sourceKey) throws Exception {
        Path workDirectory = Files.createTempDirectory(pluginId + "-src");
        Path sourceFile = workDirectory.resolve("generated/" + pluginId + "/ServiceLoaderPlugin.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, serviceLoaderPluginSource(pluginId, sourceKey), StandardCharsets.UTF_8);

        Path classesDirectory = Files.createTempDirectory(pluginId + "-classes");
        compile(sourceFile, classesDirectory);

        Path servicesFile = classesDirectory.resolve("META-INF/services/org.gensokyo.data.calcite.TemplateV2RuntimePlugin");
        Files.createDirectories(servicesFile.getParent());
        Files.writeString(servicesFile, "generated." + pluginId + ".ServiceLoaderPlugin", StandardCharsets.UTF_8);

        Path descriptorFile = classesDirectory.resolve("META-INF/data-generator/template-v2-plugin.properties");
        Files.createDirectories(descriptorFile.getParent());
        Files.writeString(descriptorFile,
                """
                plugin.id=%s
                plugin.version=1.0.0
                plugin.provider=test
                plugin.host-version-range=current
                plugin.capabilities=SOURCE:%s
                """.formatted(pluginId, sourceKey), StandardCharsets.UTF_8);

        packageJar(classesDirectory, pluginDirectory.resolve(pluginId + ".jar"));
        deleteRecursively(workDirectory);
        deleteRecursively(classesDirectory);
    }

    private void createPf4jPluginJar(Path pluginDirectory, String pluginId, String sourceKey) throws Exception {
        Path workDirectory = Files.createTempDirectory(pluginId + "-src");
        Path sourceFile = workDirectory.resolve("generated/" + pluginId + "/Pf4jPluginExtension.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, pf4jPluginSource(pluginId, sourceKey), StandardCharsets.UTF_8);

        Path classesDirectory = Files.createTempDirectory(pluginId + "-classes");
        compile(sourceFile, classesDirectory);

        Path extensionsFile = classesDirectory.resolve("META-INF/extensions.idx");
        Files.createDirectories(extensionsFile.getParent());
        Files.writeString(extensionsFile, "generated." + pluginId + ".Pf4jPluginExtension", StandardCharsets.UTF_8);

        Path descriptorFile = classesDirectory.resolve("META-INF/data-generator/template-v2-plugin.properties");
        Files.createDirectories(descriptorFile.getParent());
        Files.writeString(descriptorFile,
                """
                plugin.id=%s
                plugin.version=1.0.0
                plugin.provider=test
                plugin.host-version-range=current
                plugin.capabilities=SOURCE:%s
                """.formatted(pluginId, sourceKey), StandardCharsets.UTF_8);

        Path pf4jPluginProperties = classesDirectory.resolve("plugin.properties");
        Files.writeString(pf4jPluginProperties,
                """
                plugin.id=%s
                plugin.version=1.0.0
                plugin.provider=test
                plugin.requires=1.0.0
                """.formatted(pluginId), StandardCharsets.UTF_8);

        packageJar(classesDirectory, pluginDirectory.resolve(pluginId + ".jar"));
        deleteRecursively(workDirectory);
        deleteRecursively(classesDirectory);
    }

    private void createPf4jUdfPluginJar(Path pluginDirectory, String pluginId) throws Exception {
        Path workDirectory = Files.createTempDirectory(pluginId + "-src");
        Path sourceFile = workDirectory.resolve("generated/" + pluginId + "/Pf4jUdfPluginExtension.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, pf4jUdfPluginSource(pluginId), StandardCharsets.UTF_8);

        Path classesDirectory = Files.createTempDirectory(pluginId + "-classes");
        compile(sourceFile, classesDirectory);

        Path extensionsFile = classesDirectory.resolve("META-INF/extensions.idx");
        Files.createDirectories(extensionsFile.getParent());
        Files.writeString(extensionsFile, "generated." + pluginId + ".Pf4jUdfPluginExtension", StandardCharsets.UTF_8);

        Path descriptorFile = classesDirectory.resolve("META-INF/data-generator/template-v2-plugin.properties");
        Files.createDirectories(descriptorFile.getParent());
        Files.writeString(descriptorFile,
                """
                plugin.id=%s
                plugin.version=1.0.0
                plugin.provider=test
                plugin.host-version-range=current
                plugin.capabilities=
                """.formatted(pluginId), StandardCharsets.UTF_8);

        Path pf4jPluginProperties = classesDirectory.resolve("plugin.properties");
        Files.writeString(pf4jPluginProperties,
                """
                plugin.id=%s
                plugin.version=1.0.0
                plugin.provider=test
                plugin.requires=1.0.0
                """.formatted(pluginId), StandardCharsets.UTF_8);

        packageJar(classesDirectory, pluginDirectory.resolve(pluginId + ".jar"));
        deleteRecursively(workDirectory);
        deleteRecursively(classesDirectory);
    }

    private void createPf4jNullProviderPluginJar(Path pluginDirectory, String pluginId) throws Exception {
        Path workDirectory = Files.createTempDirectory(pluginId + "-src");
        Path sourceFile = workDirectory.resolve("generated/" + pluginId + "/Pf4jNullProviderExtension.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, pf4jNullProviderPluginSource(pluginId), StandardCharsets.UTF_8);

        Path classesDirectory = Files.createTempDirectory(pluginId + "-classes");
        compile(sourceFile, classesDirectory);

        Path extensionsFile = classesDirectory.resolve("META-INF/extensions.idx");
        Files.createDirectories(extensionsFile.getParent());
        Files.writeString(extensionsFile, "generated." + pluginId + ".Pf4jNullProviderExtension", StandardCharsets.UTF_8);

        Path descriptorFile = classesDirectory.resolve("META-INF/data-generator/template-v2-plugin.properties");
        Files.createDirectories(descriptorFile.getParent());
        Files.writeString(descriptorFile,
                """
                plugin.id=%s
                plugin.version=1.0.0
                plugin.provider=test
                plugin.host-version-range=current
                plugin.capabilities=TRANSFORM:sql
                """.formatted(pluginId), StandardCharsets.UTF_8);

        Path pf4jPluginProperties = classesDirectory.resolve("plugin.properties");
        Files.writeString(pf4jPluginProperties,
                """
                plugin.id=%s
                plugin.version=1.0.0
                plugin.provider=test
                plugin.requires=1.0.0
                """.formatted(pluginId), StandardCharsets.UTF_8);

        packageJar(classesDirectory, pluginDirectory.resolve(pluginId + ".jar"));
        deleteRecursively(workDirectory);
        deleteRecursively(classesDirectory);
    }

    private void compile(Path sourceFile, Path classesDirectory) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assertions.assertNotNull(compiler, "JDK compiler is required for plugin isolation tests");
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            Iterable compilationUnits = fileManager.getJavaFileObjects(sourceFile.toFile());
            List<String> options = List.of(
                    "--release", "25",
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classesDirectory.toString()
            );
            Boolean result = compiler.getTask(null, fileManager, null, options, null, compilationUnits).call();
            Assertions.assertEquals(Boolean.TRUE, result, "plugin test source compilation must succeed");
        }
    }

    private void packageJar(Path classesDirectory, Path jarFile) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(jarFile);
             JarOutputStream jarOutputStream = new JarOutputStream(outputStream);
             Stream<Path> stream = Files.walk(classesDirectory)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                String entryName = classesDirectory.relativize(path).toString().replace('\\', '/');
                jarOutputStream.putNextEntry(new JarEntry(entryName));
                jarOutputStream.write(Files.readAllBytes(path));
                jarOutputStream.closeEntry();
            }
        }
    }

    private void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String serviceLoaderPluginSource(String pluginId, String sourceKey) {
        return """
                package generated.%1$s;

                import org.gensokyo.data.calcite.RowSource;
                import org.gensokyo.data.calcite.TemplateV2PluginCapability;
                import org.gensokyo.data.calcite.TemplateV2RuntimePlugin;
                import org.gensokyo.data.calcite.TemplateV2RuntimePluginDescriptor;
                import org.gensokyo.data.calcite.V2SourceFactory;
                import org.gensokyo.data.model.v2.SourceVO;

                import java.util.List;

                public class ServiceLoaderPlugin implements TemplateV2RuntimePlugin {
                    @Override
                    public TemplateV2RuntimePluginDescriptor descriptor() {
                        return TemplateV2RuntimePluginDescriptor.builder("%1$s")
                                .version("1.0.0")
                                .hostVersionRange("current")
                                .provider("test")
                                .capability(TemplateV2PluginCapability.source("%2$s"))
                                .build();
                    }

                    @Override
                    public List<V2SourceFactory> sourceFactories() {
                        return List.of(new PluginSourceFactory());
                    }

                    static final class PluginSourceFactory implements V2SourceFactory {
                        @Override
                        public boolean supports(SourceVO source) {
                            return source != null && "%2$s".equalsIgnoreCase(source.getType());
                        }

                        @Override
                        public RowSource create(String name, SourceVO source) {
                            throw new UnsupportedOperationException("test only");
                        }
                    }
                }
                """.formatted(pluginId, sourceKey);
    }

    private String pf4jPluginSource(String pluginId, String sourceKey) {
        return """
                package generated.%1$s;

                import org.gensokyo.data.calcite.Pf4jTemplateV2RuntimeExtension;
                import org.gensokyo.data.calcite.RowSource;
                import org.gensokyo.data.calcite.TemplateV2PluginCapability;
                import org.gensokyo.data.calcite.TemplateV2RuntimePlugin;
                import org.gensokyo.data.calcite.TemplateV2RuntimePluginDescriptor;
                import org.gensokyo.data.calcite.TemplateV2RuntimePluginProvider;
                import org.gensokyo.data.calcite.V2SourceFactory;
                import org.gensokyo.data.model.v2.SourceVO;
                import org.pf4j.Extension;

                import java.util.List;

                @Extension
                public class Pf4jPluginExtension implements Pf4jTemplateV2RuntimeExtension {
                    @Override
                    public TemplateV2RuntimePluginProvider provider() {
                        return context -> new TemplateV2RuntimePlugin() {
                            @Override
                            public TemplateV2RuntimePluginDescriptor descriptor() {
                                return TemplateV2RuntimePluginDescriptor.builder("%1$s")
                                        .version("1.0.0")
                                        .hostVersionRange("current")
                                        .provider("test")
                                        .capability(TemplateV2PluginCapability.source("%2$s"))
                                        .build();
                            }

                            @Override
                            public List<V2SourceFactory> sourceFactories() {
                                return List.of(new PluginSourceFactory());
                            }
                        };
                    }

                    static final class PluginSourceFactory implements V2SourceFactory {
                        @Override
                        public boolean supports(SourceVO source) {
                            return source != null && "%2$s".equalsIgnoreCase(source.getType());
                        }

                        @Override
                        public RowSource create(String name, SourceVO source) {
                            throw new UnsupportedOperationException("test only");
                        }
                    }
                }
                """.formatted(pluginId, sourceKey);
    }

    private String pf4jUdfPluginSource(String pluginId) {
        return """
                package generated.%1$s;

                import org.apache.calcite.sql.type.OperandTypes;
                import org.apache.calcite.sql.type.ReturnTypes;
                import org.gensokyo.data.calcite.Pf4jTemplateV2RuntimeExtension;
                import org.gensokyo.data.calcite.TemplateV2PluginCapability;
                import org.gensokyo.data.calcite.TemplateV2RuntimePlugin;
                import org.gensokyo.data.calcite.TemplateV2RuntimePluginDescriptor;
                import org.gensokyo.data.calcite.TemplateV2RuntimePluginProvider;
                import org.gensokyo.data.calcite.TemplateV2SqlFunction;
                import org.pf4j.Extension;

                import java.util.List;

                @Extension
                public class Pf4jUdfPluginExtension implements Pf4jTemplateV2RuntimeExtension {
                    @Override
                    public TemplateV2RuntimePluginProvider provider() {
                        return context -> new TemplateV2RuntimePlugin() {
                            @Override
                            public TemplateV2RuntimePluginDescriptor descriptor() {
                                return TemplateV2RuntimePluginDescriptor.builder("%1$s")
                                        .version("1.0.0")
                                        .hostVersionRange("current")
                                        .provider("test")
                                        .build();
                            }

                            @Override
                            public List<TemplateV2SqlFunction> sqlFunctions() {
                                return List.of(new TemplateV2SqlFunction("V2_PF4J_WRAP",
                                        ReturnTypes.VARCHAR_NULLABLE,
                                        OperandTypes.ANY_ANY,
                                        context -> context.stringArgument(0) + "-" + context.stringArgument(1)));
                            }
                        };
                    }
                }
                """.formatted(pluginId);
    }

    private String pf4jNullProviderPluginSource(String pluginId) {
        return """
                package generated.%1$s;

                import org.gensokyo.data.calcite.Pf4jTemplateV2RuntimeExtension;
                import org.gensokyo.data.calcite.TemplateV2RuntimePluginProvider;
                import org.pf4j.Extension;

                @Extension
                public class Pf4jNullProviderExtension implements Pf4jTemplateV2RuntimeExtension {
                    @Override
                    public TemplateV2RuntimePluginProvider provider() {
                        return null;
                    }
                }
                """.formatted(pluginId);
    }

    private org.gensokyo.data.model.v2.IteratorSourceVO iteratorSource() {
        org.gensokyo.data.iterator.NumberIteratorVO iterator = new org.gensokyo.data.iterator.NumberIteratorVO();
        iterator.setFrom(1L);
        iterator.setTo(2L);
        iterator.setStep(1);
        org.gensokyo.data.model.v2.IteratorSourceVO source = new org.gensokyo.data.model.v2.IteratorSourceVO();
        source.setIterator(iterator);
        return source;
    }

    private org.gensokyo.data.model.v2.SqlTransformVO sql(String sql) {
        org.gensokyo.data.model.v2.SqlTransformVO transform = new org.gensokyo.data.model.v2.SqlTransformVO();
        transform.setSql(sql);
        return transform;
    }

}
