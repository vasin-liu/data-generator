package org.gensokyo.data.generator;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.kafka.support.DynamicKafkaTemplateRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.gensokyo.data.DataGeneratorApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class DefaultDataGeneratorApplicationTests {

    @Autowired
    private DynamicRoutingDataSource dynamicRoutingDataSource;

    @Autowired
    private DynamicKafkaTemplateRegistry kafkaTemplateRegistry;

    @Autowired
    private RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver;

    @Autowired
    private TemplateV2RuntimeRegistryProvider templateV2RuntimeRegistryProvider;

    @Test
    void contextLoads() {
        Assertions.assertNotNull(kafkaTemplateRegistry);
        Assertions.assertNotNull(dynamicRoutingDataSource);
        Assertions.assertNotNull(runtimeJdbcEndpointResolver);
        Assertions.assertNotNull(templateV2RuntimeRegistryProvider);
        Assertions.assertTrue(dynamicRoutingDataSource.getDataSources().containsKey("data-generator"));
    }
}
