/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.data.ai.runtime.OllamaAiRuntimeBridge;
import org.gensokyo.data.cache.Templates;
import org.gensokyo.data.calcite.AiRuntimeBridge;
import org.gensokyo.data.calcite.ConsoleSinkFactory;
import org.gensokyo.data.calcite.CsvSourceFactory;
import org.gensokyo.data.calcite.CsvSinkFactory;
import org.gensokyo.data.calcite.ElasticsearchTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.IteratorSourceFactory;
import org.gensokyo.data.calcite.JdbcTemplateTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.JsonSourceFactory;
import org.gensokyo.data.calcite.JsonSinkFactory;
import org.gensokyo.data.calcite.KafkaTemplateTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.DirectoryAwareTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.PathBasedPf4jRuntimeExtensionLocator;
import org.gensokyo.data.calcite.Pf4jRuntimeExtensionLocator;
import org.gensokyo.data.calcite.Pf4jTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.RefreshableTemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.calcite.SqlTransformFactory;
import org.gensokyo.data.calcite.TemplateV2Runner;
import org.gensokyo.data.calcite.TemplateV2PluginFramework;
import org.gensokyo.data.calcite.TemplateV2RuntimeContext;
import org.gensokyo.data.calcite.TemplateV2RuntimePlugin;
import org.gensokyo.data.calcite.TemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.TemplateV2RuntimeRegistryFactory;
import org.gensokyo.data.calcite.TemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.calcite.TemplateV2RuntimeServices;
import org.gensokyo.data.calcite.V2SinkFactory;
import org.gensokyo.data.calcite.V2SourceFactory;
import org.gensokyo.data.calcite.V2TransformFactory;
import org.gensokyo.data.elasticsearch.support.DynamicElasticsearchClientRegistry;
import org.gensokyo.data.kafka.support.DynamicKafkaTemplateRegistry;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.yaml.JacksonParser;
import org.gensokyo.data.yaml.YamlParser;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import java.nio.file.Path;
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
    @ConditionalOnMissingBean(name = "jsonSourceFactory")
    public V2SourceFactory jsonSourceFactory() {
        return new JsonSourceFactory();
    }

    @Bean
    @ConditionalOnMissingBean(V2TransformFactory.class)
    public V2TransformFactory sqlTransformFactory() {
        return new SqlTransformFactory();
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
                                                             ObjectProvider<DynamicKafkaTemplateRegistry> kafkaTemplateRegistryProvider,
                                                             ObjectProvider<DynamicElasticsearchClientRegistry> elasticsearchClientRegistryProvider,
                                                             ObjectProvider<AiRuntimeBridge> aiRuntimeBridgeProvider) {
        List<Path> pluginDirectories = properties.getV2PluginDirectories().stream()
                .map(Path::of)
                .toList();
        return new TemplateV2RuntimeContext(
                runtimeJdbcEndpointResolver,
                new TemplateV2RuntimeServices(
                        namedParameterJdbcTemplate,
                        kafkaTemplateRegistryProvider.getIfAvailable(),
                        elasticsearchClientRegistryProvider.getIfAvailable(),
                        aiRuntimeBridgeProvider.getIfAvailable()
                ),
                pluginDirectories,
                getClass().getClassLoader()
        );
    }

    @Bean
    @ConditionalOnMissingBean(AiRuntimeBridge.class)
    public AiRuntimeBridge aiRuntimeBridge(ApplicationContext applicationContext,
                                           AutowireCapableBeanFactory beanFactory) {
        return new OllamaAiRuntimeBridge(applicationContext, beanFactory);
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
    @ConditionalOnMissingBean(RuntimeJdbcEndpointResolver.class)
    public RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver(ObjectProvider<DynamicRoutingDataSource> dynamicRoutingDataSourceProvider) {
        return new DefaultRuntimeJdbcEndpointResolver(dynamicRoutingDataSourceProvider);
    }

    private boolean usePf4j(DataGeneratorProperties properties) {
        return TemplateV2PluginFramework.PF4J.name().equalsIgnoreCase(properties.getV2PluginFramework());
    }
}
