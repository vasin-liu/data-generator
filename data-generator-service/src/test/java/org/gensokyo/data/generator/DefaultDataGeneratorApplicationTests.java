package org.gensokyo.data.generator;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.gensokyo.data.kafka.support.DynamicKafkaTemplateRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class DefaultDataGeneratorApplicationTests {

    @Autowired
    private DynamicRoutingDataSource dynamicRoutingDataSource;

    @Autowired
    private DynamicKafkaTemplateRegistry kafkaTemplateRegistry;

    @Test
    void contextLoads() {
        Assertions.assertNotNull(kafkaTemplateRegistry);
        Assertions.assertNotNull(dynamicRoutingDataSource);
        Assertions.assertTrue(dynamicRoutingDataSource.getDataSources().containsKey("data-generator"));
    }
}
