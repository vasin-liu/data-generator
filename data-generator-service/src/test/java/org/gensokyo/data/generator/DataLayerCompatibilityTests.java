package org.gensokyo.data.generator;

import org.gensokyo.data.datasource.elasticsearch.DynamicElasticsearchClientRegistry;
import org.gensokyo.data.datasource.kafka.DynamicKafkaTemplateRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataLayerCompatibilityTests {

    @Test
    void requiredDataLayerTypesAreAvailable() throws ClassNotFoundException {
        assertPresent("com.alibaba.druid.pool.DruidDataSource");
        assertPresent("com.baomidou.dynamic.datasource.DynamicRoutingDataSource");

        assertPresent("org.postgresql.Driver");
        assertPresent("com.mysql.cj.jdbc.Driver");
        assertPresent("com.clickhouse.jdbc.ClickHouseDriver");
        assertPresent("dm.jdbc.driver.DmDriver");

        Assertions.assertNotNull(DynamicKafkaTemplateRegistry.class);
        Assertions.assertNotNull(DynamicElasticsearchClientRegistry.class);
    }

    private static void assertPresent(String className) throws ClassNotFoundException {
        Assertions.assertNotNull(Class.forName(className), className);
    }
}
