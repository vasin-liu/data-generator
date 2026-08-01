/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.data.ai.usage.AiQuotaService;
import org.gensokyo.data.ai.runtime.AiRateLimiter;
import org.gensokyo.data.ai.runtime.CompositeAiRuntimeBridge;
import org.gensokyo.data.ai.runtime.InMemoryAiRateLimiter;
import org.gensokyo.data.ai.runtime.JdbcAiRateLimiter;
import org.gensokyo.data.ai.runtime.OpenAiCompatibleRuntimeBridge;
import org.gensokyo.data.ai.runtime.OllamaAiRuntimeBridge;
import org.gensokyo.data.repository.AiRateLimitStateRepository;
import org.gensokyo.data.udf.InMemoryUdfRegistry;
import org.gensokyo.data.udf.JdbcUdfRegistry;
import org.gensokyo.data.udf.UdfRegistry;
import org.gensokyo.data.udf.UdfRegistryService;
import org.gensokyo.data.udf.TransformCatalogSource;
import org.gensokyo.data.repository.UdfArtifactRepository;
import org.gensokyo.data.udf.DefaultRegistrySqlFunctionSource;
import org.gensokyo.data.calcite.udf.GraalJsScriptUdfExecutor;
import org.gensokyo.data.calcite.udf.RegistryBackedRuntimePluginProvider;
import org.gensokyo.data.calcite.udf.RegistrySqlFunctionSource;
import org.gensokyo.data.cache.Templates;
import org.gensokyo.data.calcite.AiRuntimeBridge;
import org.gensokyo.data.calcite.plugin.DirectoryAwareTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.plugin.ElasticsearchTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.plugin.JdbcTemplateTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.plugin.KafkaTemplateTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.plugin.PathBasedPf4jRuntimeExtensionLocator;
import org.gensokyo.data.calcite.plugin.Pf4jRuntimeExtensionLocator;
import org.gensokyo.data.calcite.plugin.Pf4jTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.runtime.RefreshableTemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.calcite.TemplateV2PluginFramework;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeContext;
import org.gensokyo.data.calcite.TemplateV2RuntimePlugin;
import org.gensokyo.data.calcite.TemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistryFactory;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeServices;
import org.gensokyo.data.calcite.V2SinkFactory;
import org.gensokyo.data.calcite.V2SourceFactory;
import org.gensokyo.data.calcite.V2TransformFactory;
import org.gensokyo.data.calcite.sink.ConsoleSinkFactory;
import org.gensokyo.data.calcite.sink.CsvSinkFactory;
import org.gensokyo.data.calcite.sink.JsonSinkFactory;
import org.gensokyo.data.calcite.source.AiSourceFactory;
import org.gensokyo.data.calcite.source.CsvSourceFactory;
import org.gensokyo.data.calcite.source.IteratorSourceFactory;
import org.gensokyo.data.calcite.source.GeoJsonSourceFactory;
import org.gensokyo.data.calcite.source.GeoSyntheticSourceFactory;
import org.gensokyo.data.calcite.source.InlineRowsSourceFactory;
import org.gensokyo.data.calcite.source.JsonSourceFactory;
import org.gensokyo.data.calcite.sql.SpelTransformFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.calcite.sql.TemplateV2SqlFunctionRegistry;
import org.gensokyo.data.calcite.transform.JsTransformFactory;
import org.gensokyo.data.calcite.transform.JsonTransformFactory;
import org.gensokyo.data.calcite.transform.LookupTransformFactory;
import org.gensokyo.data.calcite.transform.MaskTransformFactory;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.catalog.ConnectionCatalogImpl;
import org.gensokyo.data.geo.GeoAssetResolver;
import org.gensokyo.data.geo.GeoAssetService;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.yaml.JacksonParser;
import org.gensokyo.data.yaml.YamlParser;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Configuration
public class CoreConfig {

    @Bean
    @ConditionalOnMissingBean(JacksonParser.class)
    public JacksonParser jacksonParser() {
        return new JacksonParser();
    }

    @Bean
    @ConditionalOnMissingBean(Templates.class)
    public Templates templates(DataGeneratorProperties props,
                               YamlParser yamlParser,
                               TemplateRepository repository) {
        return new Templates(props, yamlParser, repository);
    }

    @Bean
    @ConditionalOnMissingBean(name = "iteratorSourceFactory")
    public V2SourceFactory iteratorSourceFactory() {
        return new IteratorSourceFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "csvSourceFactory")
    public V2SourceFactory csvSourceFactory() {
        return new CsvSourceFactory();
    }

    @Bean
    @ConditionalOnMissingBean(GeoAssetResolver.class)
    public GeoAssetResolver geoAssetResolver(GeoAssetService geoAssetService) {
        return geoAssetService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "geoJsonSourceFactory")
    public V2SourceFactory geoJsonSourceFactory(ObjectProvider<GeoAssetResolver> geoAssetResolverProvider) {
        return new GeoJsonSourceFactory(geoAssetResolverProvider.getIfAvailable());
    }

    /**
     * Template V2 {@code geo_synthetic} source factory (Phase 19 — D-11).
     *
     * @return factory that materializes synthetic geographic point rows
     */
    @Bean
    @ConditionalOnMissingBean(name = "geoSyntheticSourceFactory")
    public V2SourceFactory geoSyntheticSourceFactory(ObjectProvider<GeoAssetResolver> geoAssetResolverProvider) {
        return new GeoSyntheticSourceFactory(geoAssetResolverProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(name = "jsonSourceFactory")
    public V2SourceFactory jsonSourceFactory() {
        return new JsonSourceFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "inlineRowsSourceFactory")
    public V2SourceFactory inlineRowsSourceFactory() {
        return new InlineRowsSourceFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "aiSourceFactory")
    public V2SourceFactory aiSourceFactory(ObjectProvider<AiRuntimeBridge> aiRuntimeBridgeProvider) {
        return new AiSourceFactory(aiRuntimeBridgeProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(name = "sqlTransformFactory")
    public V2TransformFactory sqlTransformFactory() {
        return new SqlTransformFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "spelTransformFactory")
    public V2TransformFactory spelTransformFactory() {
        return new SpelTransformFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "jsTransformFactory")
    public V2TransformFactory jsTransformFactory() {
        return new JsTransformFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "jsonTransformFactory")
    public V2TransformFactory jsonTransformFactory() {
        return new JsonTransformFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "maskTransformFactory")
    public V2TransformFactory maskTransformFactory() {
        return new MaskTransformFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "lookupTransformFactory")
    public V2TransformFactory lookupTransformFactory() {
        return new LookupTransformFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "consoleSinkFactory")
    public V2SinkFactory consoleSinkFactory() {
        return new ConsoleSinkFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "csvSinkFactory")
    public V2SinkFactory csvSinkFactory() {
        return new CsvSinkFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "jsonSinkFactory")
    public V2SinkFactory jsonSinkFactory() {
        return new JsonSinkFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "springTemplateV2RuntimePluginProvider")
    public TemplateV2RuntimePluginProvider springTemplateV2RuntimePluginProvider(List<V2SourceFactory> sourceFactories,
                                                                                 List<V2TransformFactory> transformFactories,
                                                                                 List<V2SinkFactory> sinkFactories) {
        TemplateV2RuntimePlugin springPlugin = new TemplateV2RuntimePlugin() {
            @Override
            public List<V2SourceFactory> sourceFactories() {
                return sourceFactories;
            }

            @Override
            public List<V2TransformFactory> transformFactories() {
                return transformFactories;
            }

            @Override
            public List<V2TransformFactory> transformFactories(TemplateV2SqlFunctionRegistry sqlFunctionRegistry) {
                // Rebind the SQL transform factory to the merged registry so published UDF SQL functions
                // (contributed by the registry-backed plugin) become resolvable at run time (D-08). Other
                // factories are passed through unchanged. Mirrors DefaultTemplateV2RuntimePlugin.
                List<V2TransformFactory> rebound = new ArrayList<>(transformFactories.size());
                for (V2TransformFactory factory : transformFactories) {
                    if (factory instanceof SqlTransformFactory) {
                        rebound.add(new SqlTransformFactory(sqlFunctionRegistry));
                    } else {
                        rebound.add(factory);
                    }
                }
                return rebound;
            }

            @Override
            public List<V2SinkFactory> sinkFactories() {
                return sinkFactories;
            }
        };
        return context -> springPlugin;
    }

    @Bean
    @ConditionalOnMissingBean(name = "jdbcTemplateTemplateV2RuntimePluginProvider")
    public TemplateV2RuntimePluginProvider jdbcTemplateTemplateV2RuntimePluginProvider() {
        return new JdbcTemplateTemplateV2RuntimePluginProvider();
    }

    @Bean
    @ConditionalOnMissingBean(name = "kafkaTemplateTemplateV2RuntimePluginProvider")
    public TemplateV2RuntimePluginProvider kafkaTemplateTemplateV2RuntimePluginProvider() {
        return new KafkaTemplateTemplateV2RuntimePluginProvider();
    }

    @Bean
    @ConditionalOnMissingBean(name = "elasticsearchTemplateV2RuntimePluginProvider")
    public TemplateV2RuntimePluginProvider elasticsearchTemplateV2RuntimePluginProvider() {
        return new ElasticsearchTemplateV2RuntimePluginProvider();
    }

    @Bean
    @ConditionalOnMissingBean(Pf4jRuntimeExtensionLocator.class)
    public Pf4jRuntimeExtensionLocator pf4jRuntimeExtensionLocator(DataGeneratorProperties properties) {
        List<Path> pluginDirectories = properties.getV2PluginDirectories().stream()
                .map(Path::of)
                .toList();
        return new PathBasedPf4jRuntimeExtensionLocator(pluginDirectories);
    }

    @Bean(name = "externalTemplateV2RuntimePluginProvider")
    @ConditionalOnMissingBean(name = "externalTemplateV2RuntimePluginProvider")
    public TemplateV2RuntimePluginProvider externalTemplateV2RuntimePluginProvider(DataGeneratorProperties properties,
                                                                                   Pf4jRuntimeExtensionLocator locator) {
        if (usePf4j(properties)) {
            return new Pf4jTemplateV2RuntimePluginProvider(locator);
        }
        return new DirectoryAwareTemplateV2RuntimePluginProvider();
    }

    @Bean
    @ConditionalOnMissingBean(TemplateV2RuntimeContext.class)
    public TemplateV2RuntimeContext templateV2RuntimeContext(DataGeneratorProperties properties,
                                                             RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver,
                                                             NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                                             ConnectionCatalog connectionCatalog,
                                                             ObjectProvider<AiRuntimeBridge> aiRuntimeBridgeProvider) {
        List<Path> pluginDirectories = properties.getV2PluginDirectories().stream()
                .map(Path::of)
                .toList();
        return new TemplateV2RuntimeContext(
                runtimeJdbcEndpointResolver,
                new TemplateV2RuntimeServices(
                        namedParameterJdbcTemplate,
                        connectionCatalog,
                        aiRuntimeBridgeProvider.getIfAvailable()
                ),
                pluginDirectories,
                getClass().getClassLoader()
        );
    }

    @Bean
    @ConditionalOnMissingBean(AiRateLimiter.class)
    public AiRateLimiter aiRateLimiter(
            DataGeneratorProperties properties,
            ObjectProvider<AiRateLimitStateRepository> rateLimitStateRepositoryProvider,
            ObjectProvider<TransactionTemplate> transactionTemplateProvider) {
        if (properties.getAiRuntime().isDistributedRateLimitEnabled()) {
            AiRateLimitStateRepository repository = rateLimitStateRepositoryProvider.getIfAvailable();
            TransactionTemplate transactionTemplate = transactionTemplateProvider.getIfAvailable();
            if (repository != null && transactionTemplate != null) {
                return new JdbcAiRateLimiter(repository, transactionTemplate);
            }
        }
        return new InMemoryAiRateLimiter();
    }

    @Bean
    @ConditionalOnMissingBean(AiRuntimeBridge.class)
    public AiRuntimeBridge aiRuntimeBridge(ApplicationContext applicationContext,
                                           AutowireCapableBeanFactory beanFactory,
                                           org.gensokyo.data.secret.SecretResolver secretResolver,
                                           AiRateLimiter aiRateLimiter,
                                           AiQuotaService aiQuotaService,
                                           DataGeneratorProperties properties) {
        return new CompositeAiRuntimeBridge(List.of(
                new OllamaAiRuntimeBridge(applicationContext, beanFactory, aiRateLimiter, aiQuotaService, properties),
                new OpenAiCompatibleRuntimeBridge(applicationContext, beanFactory, secretResolver, aiRateLimiter, aiQuotaService, properties)));
    }

    @Bean
    @ConditionalOnMissingBean(TemplateV2RuntimeRegistryFactory.class)
    public TemplateV2RuntimeRegistryFactory templateV2RuntimeRegistryFactory() {
        return new TemplateV2RuntimeRegistryFactory();
    }

    @Bean
    @ConditionalOnMissingBean(TemplateV2RuntimeRegistryProvider.class)
    public TemplateV2RuntimeRegistryProvider templateV2RuntimeRegistryProvider(List<TemplateV2RuntimePluginProvider> pluginProviders,
                                                                               TemplateV2RuntimeRegistryFactory registryFactory,
                                                                               TemplateV2RuntimeContext runtimeContext) {
        return new RefreshableTemplateV2RuntimeRegistryProvider(pluginProviders, registryFactory, runtimeContext);
    }

    @Bean
    @ConditionalOnMissingBean(TemplateV2Runner.class)
    public TemplateV2Runner templateV2Runner(TemplateV2RuntimeRegistryProvider runtimeRegistryProvider) {
        return new TemplateV2Runner(runtimeRegistryProvider);
    }

    @Bean
    @ConditionalOnMissingBean(ConnectionCatalog.class)
    public ConnectionCatalog connectionCatalogFallback(ConnectionCatalogImpl connectionCatalogImpl) {
        return connectionCatalogImpl;
    }

    @Bean
    @ConditionalOnMissingBean(RuntimeJdbcEndpointResolver.class)
    public RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver(
            ObjectProvider<DynamicRoutingDataSource> dynamicRoutingDataSourceProvider,
            org.gensokyo.data.secret.SecretResolver secretResolver,
            ObjectProvider<ConnectionCatalog> connectionCatalogProvider) {
        return new DefaultRuntimeJdbcEndpointResolver(
                dynamicRoutingDataSourceProvider, secretResolver, connectionCatalogProvider);
    }

    @Bean
    @ConditionalOnMissingBean(org.gensokyo.data.secret.SecretResolver.class)
    public org.gensokyo.data.secret.SecretResolver passthroughSecretResolver() {
        return new PassthroughSecretResolver();
    }

    /**
     * JDBC-backed UDF registry so uploaded/published UDFs survive restart (D-01). Declared with a
     * DISTINCT bean name {@code jdbcUdfRegistry} (not a second {@code udfRegistry} overload, which Spring
     * skips before {@code @ConditionalOnMissingBean} is evaluated) and ahead of the in-memory default so
     * the conditional fallback backs off to it.
     *
     * @param udfArtifactRepository persistence repository for UDF artifact rows
     * @return persistence-backed registry implementation
     */
    @Bean
    public UdfRegistry jdbcUdfRegistry(UdfArtifactRepository udfArtifactRepository) {
        return new JdbcUdfRegistry(udfArtifactRepository);
    }

    @Bean
    @ConditionalOnMissingBean(UdfRegistry.class)
    public UdfRegistry udfRegistry() {
        return new InMemoryUdfRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(GraalJsScriptUdfExecutor.class)
    public GraalJsScriptUdfExecutor graalJsScriptUdfExecutor() {
        return new GraalJsScriptUdfExecutor();
    }

    @Bean
    @ConditionalOnMissingBean(RegistrySqlFunctionSource.class)
    public RegistrySqlFunctionSource registrySqlFunctionSource(UdfRegistry udfRegistry,
                                                               GraalJsScriptUdfExecutor graalJsScriptUdfExecutor) {
        return new DefaultRegistrySqlFunctionSource(udfRegistry, graalJsScriptUdfExecutor);
    }

    @Bean
    @ConditionalOnMissingBean(TransformCatalogSource.class)
    public TransformCatalogSource transformCatalogSource(UdfRegistryService udfRegistryService) {
        return new TransformCatalogSource(udfRegistryService);
    }

    @Bean(name = "registryBackedTemplateV2RuntimePluginProvider")
    @ConditionalOnMissingBean(name = "registryBackedTemplateV2RuntimePluginProvider")
    public TemplateV2RuntimePluginProvider registryBackedTemplateV2RuntimePluginProvider(
            RegistrySqlFunctionSource registrySqlFunctionSource) {
        return new RegistryBackedRuntimePluginProvider(registrySqlFunctionSource);
    }

    private boolean usePf4j(DataGeneratorProperties properties) {
        return TemplateV2PluginFramework.PF4J.name().equalsIgnoreCase(properties.getV2PluginFramework());
    }
}
