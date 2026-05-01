package org.gensokyo.data.config;

import org.gensokyo.data.calcite.Pf4jRuntimeExtensionLocator;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.TemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.Pf4jTemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.DirectoryAwareTemplateV2RuntimePluginProvider;
import org.gensokyo.data.repository.TemplateRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.aop.support.AopUtils;

import static org.mockito.Mockito.mock;

class Pf4jRuntimeConfigTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoreConfig.class))
            .withBean(TemplateRepository.class, () -> mock(TemplateRepository.class))
            .withBean(RuntimeJdbcEndpointResolver.class, () -> mock(RuntimeJdbcEndpointResolver.class))
            .withBean(NamedParameterJdbcTemplate.class, () -> mock(NamedParameterJdbcTemplate.class))
            .withBean("kafkaTemplateRegistryProvider", ObjectProvider.class, () -> new EmptyObjectProvider<>())
            .withBean("elasticsearchClientRegistryProvider", ObjectProvider.class, () -> new EmptyObjectProvider<>());

    @Test
    void usesPf4jByDefault() {
        contextRunner
                .withBean(DataGeneratorProperties.class, DataGeneratorProperties::new)
                .run(context -> {
                    Assertions.assertNotNull(context.getBean(Pf4jRuntimeExtensionLocator.class));
                    Assertions.assertEquals(Pf4jTemplateV2RuntimePluginProvider.class,
                            AopUtils.getTargetClass(context.getBean("externalTemplateV2RuntimePluginProvider", TemplateV2RuntimePluginProvider.class)));
                });
    }

    @Test
    void createsServiceLoaderProviderWhenFrameworkPropertyIsServiceLoader() {
        DataGeneratorProperties properties = new DataGeneratorProperties();
        properties.setV2PluginFramework("SERVICE_LOADER");
        properties.setV2PluginDirectories(java.util.List.of("build/plugins"));
        contextRunner
                .withBean(DataGeneratorProperties.class, () -> properties)
                .run(context -> {
                    Assertions.assertEquals(DirectoryAwareTemplateV2RuntimePluginProvider.class,
                            AopUtils.getTargetClass(context.getBean("externalTemplateV2RuntimePluginProvider", TemplateV2RuntimePluginProvider.class)));
                });
    }

    @Test
    void createsPf4jProviderWhenFrameworkPropertyIsPf4j() {
        DataGeneratorProperties properties = new DataGeneratorProperties();
        properties.setV2PluginFramework("PF4J");
        properties.setV2PluginDirectories(java.util.List.of("build/plugins"));
        contextRunner
                .withBean(DataGeneratorProperties.class, () -> properties)
                .run(context -> Assertions.assertEquals(
                        Pf4jTemplateV2RuntimePluginProvider.class,
                        AopUtils.getTargetClass(context.getBean("externalTemplateV2RuntimePluginProvider", TemplateV2RuntimePluginProvider.class))));
    }

    private static final class EmptyObjectProvider<T> implements ObjectProvider<T> {
        @Override
        public T getObject(Object... args) {
            return null;
        }

        @Override
        public T getIfAvailable() {
            return null;
        }

        @Override
        public T getIfUnique() {
            return null;
        }

        @Override
        public T getObject() {
            return null;
        }
    }
}
