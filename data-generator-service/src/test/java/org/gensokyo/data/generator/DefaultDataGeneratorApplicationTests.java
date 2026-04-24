package org.gensokyo.data.generator;

import org.gensokyo.boot.kafka.support.MultipleKafkaTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration,"
                + "org.gensokyo.boot.kafka.MultipleKafkaAutoConfiguration"
})
class DefaultDataGeneratorApplicationTests {

    @MockitoBean
    private MultipleKafkaTemplate multipleKafkaTemplate;

    @Test
    void contextLoads() {
        Assertions.assertTrue(true);
    }
}
