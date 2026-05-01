package org.gensokyo.data.config;

import org.gensokyo.data.calcite.PathBasedPf4jRuntimeExtensionLocator;
import org.gensokyo.data.calcite.Pf4jTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.RefreshableTemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.calcite.StaticTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.TemplateV2RunResult;
import org.gensokyo.data.calcite.TemplateV2Runner;
import org.gensokyo.data.calcite.TemplateV2RuntimeContext;
import org.gensokyo.data.calcite.TemplateV2RuntimeRegistryFactory;
import org.gensokyo.data.calcite.DefaultTemplateV2RuntimePlugin;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.gensokyo.data.yaml.JacksonParser;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

class Pf4jTemplateSubtypeIntegrationTests {

    @Test
    void pf4jPluginModelSubtypesCanBeParsedAfterRegistration() throws Exception {
        Path pluginDirectory = Files.createTempDirectory("pf4j-template-subtype");
        try {
            createPf4jPluginJar(pluginDirectory, "pf4jSubtypePlugin",
                    "plugin_query", "plugin_transform", "plugin_writer");

            try (PathBasedPf4jRuntimeExtensionLocator locator = new PathBasedPf4jRuntimeExtensionLocator(List.of(pluginDirectory))) {
                TemplateModelSubtypeRegistrar registrar = new TemplateModelSubtypeRegistrar(locator);
                registrar.refresh();

                TemplateV2DraftVO draft = new JacksonParser().parse("""
                        name: pf4j-subtype-demo
                        sources:
                          input:
                            type: plugin_query
                            sql: SELECT 1 AS id
                        transform:
                          type: plugin_transform
                          expression: SELECT id AS value FROM input
                        sink:
                          writers:
                            - type: plugin_writer
                              target: demo_target
                        """, TemplateV2DraftVO.class);

                Assertions.assertNotNull(draft);
                Assertions.assertNotNull(draft.getSources());
                SourceVO source = draft.getSources().get("input");
                Assertions.assertNotNull(source);
                Assertions.assertEquals("generated.pf4jSubtypePlugin.PluginQuerySourceVO", source.getClass().getName());
                Assertions.assertEquals("SELECT 1 AS id", source.getClass().getMethod("getSql").invoke(source));

                TransformVO transform = draft.getTransform();
                Assertions.assertNotNull(transform);
                Assertions.assertEquals("generated.pf4jSubtypePlugin.PluginTransformVO", transform.getClass().getName());
                Assertions.assertEquals("SELECT id AS value FROM input", transform.getClass().getMethod("getExpression").invoke(transform));

                Assertions.assertNotNull(draft.getSink());
                Assertions.assertNotNull(draft.getSink().getWriters());
                Assertions.assertEquals(1, draft.getSink().getWriters().size());
                WriterVO writer = draft.getSink().getWriters().getFirst();
                Assertions.assertEquals("generated.pf4jSubtypePlugin.PluginWriterVO", writer.getClass().getName());
                Assertions.assertEquals("demo_target", writer.getTarget());
            }
        } finally {
            deleteRecursively(pluginDirectory);
        }
    }

    @Test
    void pf4jPluginFactoriesCanParticipateInTemplateV2Execution() throws Exception {
        Path pluginDirectory = Files.createTempDirectory("pf4j-template-runtime");
        try {
            createPf4jPluginJar(pluginDirectory, "pf4jRuntimePlugin",
                    "plugin_query_runtime", "plugin_transform_runtime", "plugin_writer_runtime");

            try (PathBasedPf4jRuntimeExtensionLocator locator = new PathBasedPf4jRuntimeExtensionLocator(List.of(pluginDirectory));
                 Pf4jTemplateV2RuntimePluginProvider pluginProvider = new Pf4jTemplateV2RuntimePluginProvider(locator)) {
                TemplateModelSubtypeRegistrar registrar = new TemplateModelSubtypeRegistrar(locator);
                registrar.refresh();

                TemplateV2DraftVO draft = new JacksonParser().parse("""
                        name: pf4j-runtime-demo
                        sources:
                          input:
                            type: plugin_query_runtime
                            sql: SELECT 1 AS ignored
                        transform:
                          type: plugin_transform_runtime
                          expression: plugin-transform
                        sink:
                          writers:
                            - type: plugin_writer_runtime
                              target: runtime_target
                        """, TemplateV2DraftVO.class);

                TemplateV2VO template = TemplateV2Normalizer.normalize(draft);
                TemplateV2Runner runner = new TemplateV2Runner(
                        new RefreshableTemplateV2RuntimeRegistryProvider(
                                List.of(pluginProvider),
                                new TemplateV2RuntimeRegistryFactory(),
                                TemplateV2RuntimeContext.empty()
                        )
                );

                TemplateV2RunResult result = runner.run(template);

                Assertions.assertEquals(2, result.getRows().size());
                Assertions.assertEquals("alpha-plugin", result.getRows().get(0).getString("value"));
                Assertions.assertEquals("beta-plugin", result.getRows().get(1).getString("value"));

                ClassLoader pluginClassLoader = locator.pluginClassLoaders().getFirst();
                Class<?> sinkCapture = Class.forName("generated.pf4jRuntimePlugin.PluginSinkCapture", true, pluginClassLoader);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> capturedRows = (List<Map<String, Object>>) sinkCapture.getMethod("capturedRows").invoke(null);
                Assertions.assertEquals(2, capturedRows.size());
                Assertions.assertEquals("runtime_target", sinkCapture.getMethod("capturedTarget").invoke(null));
                Assertions.assertEquals("alpha-plugin", capturedRows.get(0).get("value"));
                Assertions.assertEquals("plugin-transform", capturedRows.get(0).get("pipeline"));
                Assertions.assertEquals("beta-plugin", capturedRows.get(1).get("value"));
            }
        } finally {
            deleteRecursively(pluginDirectory);
        }
    }

    @Test
    void builtinAndPf4jFactoriesCanExecuteInOneTemplate() throws Exception {
        Path pluginDirectory = Files.createTempDirectory("pf4j-template-mixed-runtime");
        try {
            createPf4jPluginJar(pluginDirectory, "pf4jMixedRuntimePlugin",
                    "plugin_query_mixed", "plugin_transform_mixed", "plugin_writer_mixed");

            try (PathBasedPf4jRuntimeExtensionLocator locator = new PathBasedPf4jRuntimeExtensionLocator(List.of(pluginDirectory));
                 Pf4jTemplateV2RuntimePluginProvider pluginProvider = new Pf4jTemplateV2RuntimePluginProvider(locator)) {
                TemplateModelSubtypeRegistrar registrar = new TemplateModelSubtypeRegistrar(locator);
                registrar.refresh();

                TemplateV2DraftVO draft = new JacksonParser().parse("""
                        name: pf4j-mixed-runtime-demo
                        sources:
                          input:
                            type: iterator
                            iterator:
                              type: number
                              from: 1
                              to: 2
                              step: 1
                        transform:
                          type: plugin_transform_mixed
                          expression: mixed-transform
                        sink:
                          writers:
                            - type: plugin_writer_mixed
                              target: mixed_target
                        """, TemplateV2DraftVO.class);
                TemplateV2VO template = TemplateV2Normalizer.normalize(draft);

                TemplateV2Runner runner = new TemplateV2Runner(
                        new RefreshableTemplateV2RuntimeRegistryProvider(
                                List.of(
                                        new StaticTemplateV2RuntimePluginProvider(new DefaultTemplateV2RuntimePlugin()),
                                        pluginProvider
                                ),
                                new TemplateV2RuntimeRegistryFactory(),
                                TemplateV2RuntimeContext.empty()
                        )
                );

                TemplateV2RunResult result = runner.run(template);

                Assertions.assertEquals(2, result.getRows().size());
                Assertions.assertEquals("1-plugin", result.getRows().get(0).getString("value"));
                Assertions.assertEquals("2-plugin", result.getRows().get(1).getString("value"));
                Assertions.assertEquals("mixed-transform", result.getRows().get(0).getString("pipeline"));

                ClassLoader pluginClassLoader = locator.pluginClassLoaders().getFirst();
                Class<?> sinkCapture = Class.forName("generated.pf4jMixedRuntimePlugin.PluginSinkCapture", true, pluginClassLoader);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> capturedRows = (List<Map<String, Object>>) sinkCapture.getMethod("capturedRows").invoke(null);
                Assertions.assertEquals(2, capturedRows.size());
                Assertions.assertEquals("mixed_target", sinkCapture.getMethod("capturedTarget").invoke(null));
                Assertions.assertEquals("1-plugin", capturedRows.get(0).get("value"));
                Assertions.assertEquals("2-plugin", capturedRows.get(1).get("value"));
            }
        } finally {
            deleteRecursively(pluginDirectory);
        }
    }

    @Test
    void pf4jRefreshMakesNewPluginFactoriesExecutable() throws Exception {
        Path pluginDirectory = Files.createTempDirectory("pf4j-template-refresh-runtime");
        try {
            try (PathBasedPf4jRuntimeExtensionLocator locator = new PathBasedPf4jRuntimeExtensionLocator(List.of(pluginDirectory));
                 Pf4jTemplateV2RuntimePluginProvider pluginProvider = new Pf4jTemplateV2RuntimePluginProvider(locator)) {
                TemplateModelSubtypeRegistrar registrar = new TemplateModelSubtypeRegistrar(locator);
                RefreshableTemplateV2RuntimeRegistryProvider registryProvider =
                        new RefreshableTemplateV2RuntimeRegistryProvider(
                                List.of(
                                        new StaticTemplateV2RuntimePluginProvider(new DefaultTemplateV2RuntimePlugin()),
                                        pluginProvider
                                ),
                                new TemplateV2RuntimeRegistryFactory(),
                                TemplateV2RuntimeContext.empty()
                        );
                TemplateV2Runner runner = new TemplateV2Runner(registryProvider);

                createPf4jPluginJar(pluginDirectory, "pf4jRefreshRuntimePlugin",
                        "plugin_query_refresh", "plugin_transform_refresh", "plugin_writer_refresh");
                registrar.refresh();
                registryProvider.refresh();

                TemplateV2DraftVO draft = new JacksonParser().parse("""
                        name: pf4j-refresh-runtime-demo
                        sources:
                          input:
                            type: iterator
                            iterator:
                              type: number
                              from: 3
                              to: 4
                              step: 1
                        transform:
                          type: plugin_transform_refresh
                          expression: refreshed-transform
                        sink:
                          writers:
                            - type: plugin_writer_refresh
                              target: refreshed_target
                        """, TemplateV2DraftVO.class);

                TemplateV2RunResult result = runner.run(TemplateV2Normalizer.normalize(draft));

                Assertions.assertEquals(2, result.getRows().size());
                Assertions.assertEquals("3-plugin", result.getRows().get(0).getString("value"));
                Assertions.assertEquals("4-plugin", result.getRows().get(1).getString("value"));
                Assertions.assertEquals("refreshed-transform", result.getRows().get(0).getString("pipeline"));

                ClassLoader pluginClassLoader = locator.pluginClassLoaders().getFirst();
                Class<?> sinkCapture = Class.forName("generated.pf4jRefreshRuntimePlugin.PluginSinkCapture", true, pluginClassLoader);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> capturedRows = (List<Map<String, Object>>) sinkCapture.getMethod("capturedRows").invoke(null);
                Assertions.assertEquals(2, capturedRows.size());
                Assertions.assertEquals("refreshed_target", sinkCapture.getMethod("capturedTarget").invoke(null));
                Assertions.assertEquals("3-plugin", capturedRows.get(0).get("value"));
                Assertions.assertEquals("4-plugin", capturedRows.get(1).get("value"));
            }
        } finally {
            deleteRecursively(pluginDirectory);
        }
    }

    private void createPf4jPluginJar(Path pluginDirectory,
                                     String pluginId,
                                     String sourceType,
                                     String transformType,
                                     String writerType) throws Exception {
        Path workDirectory = Files.createTempDirectory(pluginId + "-src");
        Path packageDirectory = workDirectory.resolve("generated/" + pluginId);
        Path extensionSourceFile = packageDirectory.resolve("Pf4jPluginExtension.java");
        Path subtypeSourceFile = packageDirectory.resolve("PluginQuerySourceVO.java");
        Path transformSourceFile = packageDirectory.resolve("PluginTransformVO.java");
        Path writerSourceFile = packageDirectory.resolve("PluginWriterVO.java");
        Path sinkCaptureSourceFile = packageDirectory.resolve("PluginSinkCapture.java");
        Files.createDirectories(packageDirectory);
        Files.writeString(extensionSourceFile,
                pf4jPluginExtensionSource(pluginId, sourceType, transformType, writerType), StandardCharsets.UTF_8);
        Files.writeString(subtypeSourceFile, pf4jPluginSourceSubtypeSource(pluginId, sourceType), StandardCharsets.UTF_8);
        Files.writeString(transformSourceFile, pf4jPluginTransformSubtypeSource(pluginId, transformType), StandardCharsets.UTF_8);
        Files.writeString(writerSourceFile, pf4jPluginWriterSubtypeSource(pluginId, writerType), StandardCharsets.UTF_8);
        Files.writeString(sinkCaptureSourceFile, pf4jPluginSinkCaptureSource(pluginId), StandardCharsets.UTF_8);

        Path classesDirectory = Files.createTempDirectory(pluginId + "-classes");
        compile(List.of(extensionSourceFile, subtypeSourceFile, transformSourceFile, writerSourceFile, sinkCaptureSourceFile), classesDirectory);

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
                plugin.capabilities=SOURCE:%s,TRANSFORM:%s,SINK:%s
                """.formatted(pluginId, sourceType, transformType, writerType), StandardCharsets.UTF_8);

        Path spiSource = classesDirectory.resolve("META-INF/services/org.gensokyo.data.model.v2.SourceVO");
        Files.createDirectories(spiSource.getParent());
        Files.writeString(spiSource, "generated." + pluginId + ".PluginQuerySourceVO", StandardCharsets.UTF_8);

        Path spiTransform = classesDirectory.resolve("META-INF/services/org.gensokyo.data.model.v2.TransformVO");
        Files.writeString(spiTransform, "generated." + pluginId + ".PluginTransformVO", StandardCharsets.UTF_8);

        Path spiWriter = classesDirectory.resolve("META-INF/services/org.gensokyo.data.model.vo.writer.WriterVO");
        Files.writeString(spiWriter, "generated." + pluginId + ".PluginWriterVO", StandardCharsets.UTF_8);

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

    private void compile(List<Path> sourceFiles, Path classesDirectory) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assertions.assertNotNull(compiler, "JDK compiler is required for PF4J subtype integration tests");
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            Iterable<? extends javax.tools.JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(
                    sourceFiles.stream().map(Path::toFile).toList()
            );
            List<String> options = List.of(
                    "--release", "25",
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classesDirectory.toString()
            );
            Boolean result = compiler.getTask(null, fileManager, null, options, null, compilationUnits).call();
            Assertions.assertEquals(Boolean.TRUE, result, "plugin integration test source compilation must succeed");
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

    private String pf4jPluginExtensionSource(String pluginId,
                                             String sourceType,
                                             String transformType,
                                             String writerType) {
        return """
                package generated.%1$s;

                import org.gensokyo.data.calcite.Pf4jTemplateV2RuntimeExtension;
                import org.gensokyo.data.calcite.RowSource;
                import org.gensokyo.data.calcite.RowSink;
                import org.gensokyo.data.calcite.TemplateV2PluginCapability;
                import org.gensokyo.data.calcite.TemplateV2RuntimePlugin;
                import org.gensokyo.data.calcite.TemplateV2RuntimePluginDescriptor;
                import org.gensokyo.data.calcite.TemplateV2RuntimePluginProvider;
                import org.gensokyo.data.calcite.V2SinkFactory;
                import org.gensokyo.data.calcite.V2SourceFactory;
                import org.gensokyo.data.calcite.V2TransformFactory;
                import org.gensokyo.data.calcite.CalciteExecutionContext;
                import org.gensokyo.data.calcite.CalciteRowTransformer;
                import org.gensokyo.data.model.v2.SourceVO;
                import org.gensokyo.data.model.v2.TransformVO;
                import org.gensokyo.data.model.vo.writer.WriterVO;
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
                                        .capability(TemplateV2PluginCapability.transform("%3$s"))
                                        .capability(TemplateV2PluginCapability.sink("%4$s"))
                                        .build();
                            }

                            @Override
                            public List<V2SourceFactory> sourceFactories() {
                                return List.of(new PluginSourceFactory());
                            }

                            @Override
                            public List<V2TransformFactory> transformFactories() {
                                return List.of(new PluginTransformFactory());
                            }

                            @Override
                            public List<V2SinkFactory> sinkFactories() {
                                return List.of(new PluginSinkFactory());
                            }
                        };
                    }

                    static final class PluginSourceFactory implements V2SourceFactory {
                        @Override
                        public boolean supports(SourceVO source) {
                            return source instanceof PluginQuerySourceVO;
                        }

                        @Override
                        public RowSource create(String name, SourceVO source) {
                            return new RowSource() {
                                @Override
                                public String name() {
                                    return name;
                                }

                                @Override
                                public org.gensokyo.data.model.v2.RowSchema schema() {
                                    org.gensokyo.data.model.v2.RowSchema schema = new org.gensokyo.data.model.v2.RowSchema();
                                    schema.setColumns(List.of(
                                            new org.gensokyo.data.model.v2.ColumnDef("value", "VARCHAR", false)
                                    ));
                                    return schema;
                                }

                                @Override
                                public List<org.gensokyo.data.model.v2.Row> rows() {
                                    return List.of(
                                            new org.gensokyo.data.model.v2.Row(java.util.Map.of("value", "alpha")),
                                            new org.gensokyo.data.model.v2.Row(java.util.Map.of("value", "beta"))
                                    );
                                }
                            };
                        }
                    }

                    static final class PluginTransformFactory implements V2TransformFactory {
                        @Override
                        public boolean supports(TransformVO transform) {
                            return transform instanceof PluginTransformVO;
                        }

                        @Override
                        public CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context) {
                            List<org.gensokyo.data.model.v2.Row> input = context.getData().get("input");
                            org.gensokyo.data.model.v2.RowSchema schema = new org.gensokyo.data.model.v2.RowSchema();
                            schema.setColumns(List.of(
                                    new org.gensokyo.data.model.v2.ColumnDef("value", "VARCHAR", false),
                                    new org.gensokyo.data.model.v2.ColumnDef("pipeline", "VARCHAR", false)
                            ));
                            List<org.gensokyo.data.model.v2.Row> output = input.stream()
                                    .map(row -> new org.gensokyo.data.model.v2.Row(java.util.Map.of(
                                            "value", row.getString("value") + "-plugin",
                                            "pipeline", ((PluginTransformVO) transform).getExpression()
                                    )))
                                    .toList();
                            return new CalciteRowTransformer.TransformResult(schema, output);
                        }
                    }

                    static final class PluginSinkFactory implements V2SinkFactory {
                        @Override
                        public boolean supports(WriterVO writer) {
                            return writer instanceof PluginWriterVO;
                        }

                        @Override
                        public RowSink create(WriterVO writer) {
                            return (schema, rows) -> PluginSinkCapture.capture((PluginWriterVO) writer, rows);
                        }
                    }
                }
                """.formatted(pluginId, sourceType, transformType, writerType);
    }

    private String pf4jPluginSourceSubtypeSource(String pluginId, String sourceType) {
        return """
                package generated.%1$s;

                import org.gensokyo.data.json.JsonSubType;
                import org.gensokyo.data.model.v2.SourceVO;

                @JsonSubType("%2$s")
                public class PluginQuerySourceVO extends SourceVO {
                    public PluginQuerySourceVO() {
                        setType("%2$s");
                    }

                    private String sql;

                    public String getSql() {
                        return sql;
                    }

                    public void setSql(String sql) {
                        this.sql = sql;
                    }
                }
                """.formatted(pluginId, sourceType);
    }

    private String pf4jPluginTransformSubtypeSource(String pluginId, String transformType) {
        return """
                package generated.%1$s;

                import org.gensokyo.data.json.JsonSubType;
                import org.gensokyo.data.model.v2.TransformVO;

                @JsonSubType("%2$s")
                public class PluginTransformVO extends TransformVO {
                    public PluginTransformVO() {
                        setType("%2$s");
                    }

                    private String expression;

                    public String getExpression() {
                        return expression;
                    }

                    public void setExpression(String expression) {
                        this.expression = expression;
                    }
                }
                """.formatted(pluginId, transformType);
    }

    private String pf4jPluginWriterSubtypeSource(String pluginId, String writerType) {
        return """
                package generated.%1$s;

                import org.gensokyo.data.json.JsonSubType;
                import org.gensokyo.data.model.vo.writer.WriterVO;

                @JsonSubType("%2$s")
                public class PluginWriterVO extends WriterVO {
                    public PluginWriterVO() {
                        setType("%2$s");
                    }
                }
                """.formatted(pluginId, writerType);
    }

    private String pf4jPluginSinkCaptureSource(String pluginId) {
        return """
                package generated.%1$s;

                import org.gensokyo.data.model.v2.Row;

                import java.util.ArrayList;
                import java.util.LinkedHashMap;
                import java.util.List;
                import java.util.Map;

                public final class PluginSinkCapture {
                    private static final List<Map<String, Object>> CAPTURED_ROWS = new ArrayList<>();
                    private static String capturedTarget;

                    private PluginSinkCapture() {
                    }

                    public static synchronized void capture(PluginWriterVO writer, List<Row> rows) {
                        CAPTURED_ROWS.clear();
                        capturedTarget = writer.getTarget();
                        for (Row row : rows) {
                            CAPTURED_ROWS.add(new LinkedHashMap<>(row.values()));
                        }
                    }

                    public static synchronized List<Map<String, Object>> capturedRows() {
                        return List.copyOf(CAPTURED_ROWS);
                    }

                    public static synchronized String capturedTarget() {
                        return capturedTarget;
                    }
                }
                """.formatted(pluginId);
    }
}
