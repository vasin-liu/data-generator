package org.gensokyo.data.generator;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.gensokyo.boot.kafka.support.MultipleKafkaTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.gensokyo.boot.kafka.MultipleKafkaAutoConfiguration",
        "spring.config.location=classpath:/application-phase7-test.yaml"
})
class DefaultDataGeneratorApplicationTests {

    @MockitoBean
    private MultipleKafkaTemplate multipleKafkaTemplate;

    @Autowired
    private DynamicRoutingDataSource dynamicRoutingDataSource;

    @Test
    void contextLoads() {
        Assertions.assertNotNull(dynamicRoutingDataSource);
        Assertions.assertTrue(dynamicRoutingDataSource.getDataSources().containsKey("data-generator"));
    }
}
