package org.gensokyo.data.calcite.plugin;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class DirectoryAwareTemplateV2RuntimePluginProvider implements TemplateV2RuntimePluginProvider {
    private final TemplateV2ExternalPluginDescriptorResolver descriptorResolver;

    public DirectoryAwareTemplateV2RuntimePluginProvider() {
        this(new TemplateV2ExternalPluginDescriptorResolver());
    }

    public DirectoryAwareTemplateV2RuntimePluginProvider(TemplateV2ExternalPluginDescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    @Override
    public TemplateV2RuntimePlugin createPlugin(TemplateV2RuntimeContext context) {
        List<TemplateV2RuntimePlugin> plugins = new ArrayList<>();
        PluginClassLoaderBundle bundle = buildPluginClassLoader(context);
        ClassLoader loader = bundle.classLoader();
        ServiceLoader.load(TemplateV2RuntimePlugin.class, loader)
                .forEach(plugin -> plugins.add(wrapDescriptorAware(plugin, bundle.jarPaths(), loader)));
        ServiceLoader.load(TemplateV2RuntimePluginProvider.class, loader)
                .forEach(provider -> plugins.add(wrapDescriptorAware(provider.createPlugin(context), bundle.jarPaths(), loader)));
        TemplateV2RuntimePluginContractValidator.validate(plugins);
        return new CompositeTemplateV2RuntimePlugin(plugins);
    }

    private PluginClassLoaderBundle buildPluginClassLoader(TemplateV2RuntimeContext context) {
        List<URL> urls = new ArrayList<>();
        List<Path> jarPaths = new ArrayList<>();
        for (Path directory : context.pluginDirectories()) {
            if (directory == null || !Files.isDirectory(directory)) {
                continue;
            }
            try (var stream = Files.list(directory)) {
                stream.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".jar"))
                        .forEach(path -> {
                            try {
                                jarPaths.add(path);
                                urls.add(path.toUri().toURL());
                            } catch (Exception ignored) {
                            }
                        });
            } catch (Exception ignored) {
            }
        }
        if (urls.isEmpty()) {
            return new PluginClassLoaderBundle(context.pluginClassLoader(), List.of());
        }
        return new PluginClassLoaderBundle(new URLClassLoader(urls.toArray(URL[]::new), context.pluginClassLoader()),
                List.copyOf(jarPaths));
    }

    private TemplateV2RuntimePlugin wrapDescriptorAware(TemplateV2RuntimePlugin plugin,
                                                        List<Path> jarPaths,
                                                        ClassLoader classLoader) {
        if (jarPaths.isEmpty()) {
            return plugin;
        }
        TemplateV2RuntimePluginDescriptor descriptor = descriptorResolver.resolve(jarPaths.getFirst(), classLoader);
        return new DescriptorAwareTemplateV2RuntimePlugin(plugin, mergeDescriptor(plugin.descriptor(), descriptor));
    }

    private TemplateV2RuntimePluginDescriptor mergeDescriptor(TemplateV2RuntimePluginDescriptor original,
                                                              TemplateV2RuntimePluginDescriptor external) {
        TemplateV2RuntimePluginDescriptor.Builder builder = TemplateV2RuntimePluginDescriptor.builder(external.id())
                .version(external.version())
                .hostVersionRange(external.hostVersionRange())
                .provider(external.provider());
        if (!external.capabilities().isEmpty()) {
            builder.capabilities(external.capabilities());
        } else {
            builder.capabilities(original.capabilities());
        }
        return builder.build();
    }

    private record PluginClassLoaderBundle(ClassLoader classLoader, List<Path> jarPaths) {
    }

    static record CompositeTemplateV2RuntimePlugin(List<TemplateV2RuntimePlugin> plugins)
            implements TemplateV2RuntimePlugin {
        @Override
        public TemplateV2RuntimePluginDescriptor descriptor() {
            return TemplateV2RuntimePluginDescriptor.builder("composite:" + plugins.stream()
                            .map(plugin -> plugin.descriptor().id())
                            .sorted()
                            .reduce((left, right) -> left + "+" + right)
                            .orElse("empty"))
                    .version("composite")
                    .hostVersionRange("current")
                    .provider("gensokyo")
                    .build();
        }

        @Override
        public List<V2SourceFactory> sourceFactories() {
            List<V2SourceFactory> factories = new ArrayList<>();
            for (TemplateV2RuntimePlugin plugin : plugins) {
                factories.addAll(plugin.sourceFactories());
            }
            return factories;
        }

        @Override
        public List<V2TransformFactory> transformFactories(TemplateV2SqlFunctionRegistry sqlFunctionRegistry) {
            List<V2TransformFactory> factories = new ArrayList<>();
            for (TemplateV2RuntimePlugin plugin : plugins) {
                factories.addAll(plugin.transformFactories(sqlFunctionRegistry));
            }
            return factories;
        }

        @Override
        public List<V2SinkFactory> sinkFactories() {
            List<V2SinkFactory> factories = new ArrayList<>();
            for (TemplateV2RuntimePlugin plugin : plugins) {
                factories.addAll(plugin.sinkFactories());
            }
            return factories;
        }

        @Override
        public List<TemplateV2SqlFunction> sqlFunctions() {
            List<TemplateV2SqlFunction> functions = new ArrayList<>();
            java.util.Map<String, String> owners = new java.util.LinkedHashMap<>();
            for (TemplateV2RuntimePlugin plugin : plugins) {
                String pluginId = plugin.descriptor().id();
                for (TemplateV2SqlFunction function : plugin.sqlFunctions()) {
                    String normalizedName = TemplateV2SqlFunctionRegistry.normalize(function.name());
                    String previousOwner = owners.putIfAbsent(normalizedName, pluginId);
                    if (previousOwner != null) {
                        throw new TemplateV2RuntimeRegistryBuildException("Duplicate Template V2 SQL function ["
                                + function.name() + "] claimed by [" + previousOwner + "] and [" + pluginId + "]",
                                new IllegalStateException("duplicate SQL function: " + function.name()));
                    }
                    functions.add(function);
                }
            }
            return functions;
        }
    }
}
