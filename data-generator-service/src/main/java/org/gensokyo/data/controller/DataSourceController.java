/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.datasource.DataSourceConfigService;
import org.gensokyo.data.datasource.DataSourceConfigSummary;
import org.gensokyo.data.datasource.DataSourceConnectionTestRequest;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * JDBC datasource management API (persisted config + runtime registry).
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/9/20 , Version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/datasource")
@Validated
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceConfigService dataSourceConfigService;

    /**
     * Lists runtime datasource keys (includes yaml-configured and persisted).
     *
     * @return datasource names
     */
    @GetMapping("/database/list")
    public R<Set<String>> databaseDatasourceList() {
        return R.ok(dataSourceConfigService.listRuntimeNames());
    }

    /**
     * Lists persisted datasource definitions (passwords omitted).
     *
     * @return config summaries
     */
    @GetMapping("/database/configs")
    public R<List<DataSourceConfigSummary>> listDatabaseConfigs() {
        return R.ok("Configs loaded", dataSourceConfigService.listAll());
    }

    /**
     * Removes a datasource from runtime and disables persisted row when present.
     *
     * @param datasource datasource name
     * @return success message
     */
    @PostMapping("/database/remove/{datasource}")
    public R<String> removeDatabaseDatasource(@NotBlank @PathVariable String datasource) {
        dataSourceConfigService.remove(datasource);
        return R.ok("删除数据库数据源成功");
    }

    /**
     * Adds or updates a JDBC datasource (multipart; optional driver JAR).
     *
     * @param name            unique name
     * @param url             JDBC URL
     * @param username        user
     * @param password        password
     * @param driverClassName driver class
     * @param driverFile      optional driver jar
     * @return success message
     */
    @PostMapping("/database/addDatasource")
    public R<String> addDatabaseDatasource(
            @RequestParam String name,
            @RequestParam String url,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String driverClassName,
            @RequestParam(required = false) MultipartFile driverFile) {
        try {
            dataSourceConfigService.save(name, url, username, password, driverClassName, driverFile);
        } catch (Exception e) {
            throw new DataGeneratorException("添加数据库数据源失败", e);
        }
        return R.ok("添加数据库数据源成功");
    }

    /**
     * Tests JDBC connectivity without persisting.
     *
     * @param request connection parameters
     * @return success message
     */
    @PostMapping("/database/test")
    public R<String> testDatabaseConnection(@RequestBody DataSourceConnectionTestRequest request) {
        try {
            String message = dataSourceConfigService.testConnection(
                    request.url(),
                    request.username(),
                    request.password(),
                    request.driverClassName(),
                    request.driverJarPath());
            return R.ok(message);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * Tests a persisted datasource by name.
     *
     * @param name datasource key
     * @return success message
     */
    @PostMapping("/database/test/{name}")
    public R<String> testDatabaseConnectionByName(@PathVariable String name) {
        try {
            return R.ok(dataSourceConfigService.testConnectionByName(name));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
