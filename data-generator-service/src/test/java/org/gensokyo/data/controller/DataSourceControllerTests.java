/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.R;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Set;

/**
 * Integration tests for {@code /datasource/database/*} endpoints.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class DataSourceControllerTests {

    @Autowired
    private DataSourceController dataSourceController;

    @Autowired
    private DynamicRoutingDataSource dynamicRoutingDataSource;

    @BeforeEach
    void registerRemovableDatasource() {
        if (!dynamicRoutingDataSource.getDataSources().containsKey("removable-ds")) {
            dynamicRoutingDataSource.addDataSource("removable-ds", buildH2DataSource("removable-ds"));
        }
    }

    @Test
    void databaseDatasourceListIncludesConfiguredKeys() {
        R<Set<String>> result = dataSourceController.databaseDatasourceList();

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.getData().contains("data-generator"));
        Assertions.assertTrue(result.getData().contains("compare-inline-ds"));
    }

    @Test
    void removeDatabaseDatasourceRemovesKey() {
        R<String> removed = dataSourceController.removeDatabaseDatasource("removable-ds");

        Assertions.assertTrue(removed.isSuccess());
        R<Set<String>> list = dataSourceController.databaseDatasourceList();
        Assertions.assertFalse(list.getData().contains("removable-ds"));
    }

    @Test
    void addDatabaseDatasourceFailsWhenDriverClassMissing() {
        MockMultipartFile driverFile = new MockMultipartFile(
                "driverFile",
                "empty.jar",
                "application/java-archive",
                new byte[] {1, 2, 3});

        Assertions.assertThrows(
                DataGeneratorException.class,
                () -> dataSourceController.addDatabaseDatasource(
                        "bad-ds",
                        "jdbc:h2:mem:bad;DB_CLOSE_DELAY=-1",
                        "sa",
                        "",
                        "com.example.NonexistentDriver",
                        driverFile));
    }

    private static DruidDataSource buildH2DataSource(String dbName) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setDriverClassName("org.h2.Driver");
        return dataSource;
    }
}
