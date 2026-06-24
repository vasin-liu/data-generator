/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import jakarta.validation.constraints.NotBlank;
import org.gensokyo.data.api.console.dto.DataSourcesOverviewDto;
import org.gensokyo.data.api.console.dto.JdbcDriverPresetDto;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.BundledJdbcDriverRegistry;
import org.gensokyo.data.datasource.DataSourceConfigService;
import org.gensokyo.data.datasource.JdbcDriverPresetCatalog;
import org.gensokyo.data.api.console.dto.KafkaClusterUpsertRequest;
import org.gensokyo.data.api.console.dto.ElasticsearchClusterUpsertRequest;
import org.gensokyo.data.datasource.DataSourceConnectionTestRequest;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.messaging.MessagingClusterConfigService;
import org.gensokyo.data.model.vo.R;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Console facade for JDBC datasource administration under {@code /api/datasources}.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
@RestController
@RequestMapping("/api/datasources")
public class ConsoleDataSourceController {

    private final DataSourceConfigService dataSourceConfigService;
    private final BundledJdbcDriverRegistry bundledJdbcDriverRegistry;
    private final MessagingClusterConfigService messagingClusterConfigService;
    private final ConnectionCatalog connectionCatalog;

    /**
     * @param dataSourceConfigService       JDBC persistence
     * @param bundledJdbcDriverRegistry     driver catalog
     * @param messagingClusterConfigService Kafka/ES console persistence
     * @param connectionCatalog             merged bootstrap + managed connection list
     */
    public ConsoleDataSourceController(
            DataSourceConfigService dataSourceConfigService,
            BundledJdbcDriverRegistry bundledJdbcDriverRegistry,
            MessagingClusterConfigService messagingClusterConfigService,
            ConnectionCatalog connectionCatalog) {
        this.dataSourceConfigService = dataSourceConfigService;
        this.bundledJdbcDriverRegistry = bundledJdbcDriverRegistry;
        this.messagingClusterConfigService = messagingClusterConfigService;
        this.connectionCatalog = connectionCatalog;
    }

    /**
     * @return persisted configs and runtime keys
     */
    @GetMapping
    public R<DataSourcesOverviewDto> overview() {
        List<String> runtimeKeys = dataSourceConfigService.listRuntimeNames().stream().sorted().toList();
        return R.ok(DataSourcesOverviewDto.of(
                dataSourceConfigService.listAll(),
                runtimeKeys,
                bundledJdbcDriverRegistry,
                messagingClusterConfigService,
                connectionCatalog));
    }

    /**
     * @param request cluster definition
     * @return saved summary message
     */
    @PostMapping("/kafka-clusters")
    public R<String> upsertKafkaCluster(@RequestBody KafkaClusterUpsertRequest request) {
        messagingClusterConfigService.saveKafka(request);
        return R.ok("Kafka cluster saved");
    }

    /**
     * @param name cluster id
     * @return success message
     */
    @DeleteMapping("/kafka-clusters/{name}")
    public R<String> removeKafkaCluster(@PathVariable String name) {
        messagingClusterConfigService.remove(name);
        return R.ok("Kafka cluster removed");
    }

    /**
     * @param request cluster definition
     * @return saved summary message
     */
    @PostMapping("/elasticsearch-clusters")
    public R<String> upsertElasticsearchCluster(@RequestBody ElasticsearchClusterUpsertRequest request) {
        messagingClusterConfigService.saveElasticsearch(request);
        return R.ok("Elasticsearch cluster saved");
    }

    /**
     * @param name cluster id
     * @return success message
     */
    @DeleteMapping("/elasticsearch-clusters/{name}")
    public R<String> removeElasticsearchCluster(@PathVariable String name) {
        messagingClusterConfigService.remove(name);
        return R.ok("Elasticsearch cluster removed");
    }

    /**
     * @return built-in JDBC driver presets (primary + alternate driver classes per version)
     */
    @GetMapping("/driver-presets")
    public R<List<JdbcDriverPresetDto>> driverPresets() {
        List<JdbcDriverPresetDto> presets = JdbcDriverPresetCatalog.all().stream()
                .map(p -> JdbcDriverPresetDto.from(p, bundledJdbcDriverRegistry))
                .toList();
        return R.ok(presets);
    }

    /**
     * @param name            unique datasource key
     * @param url             JDBC URL
     * @param username        user
     * @param password        password
     * @param driverClassName driver class
     * @param driverFile      optional driver JAR
     * @return success message
     */
    @PostMapping
    public R<String> upsert(
            @RequestParam String name,
            @RequestParam String url,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String passwordSecretRef,
            @RequestParam String driverClassName,
            @RequestParam(required = false) MultipartFile driverFile) {
        try {
            dataSourceConfigService.save(
                    name, url, username, password, passwordSecretRef, driverClassName, driverFile);
        } catch (Exception e) {
            throw new DataGeneratorException("Failed to save datasource", e);
        }
        return R.ok("Datasource saved");
    }

    /**
     * @param name datasource key
     * @return success message
     */
    @DeleteMapping("/{name}")
    public R<String> remove(@NotBlank @PathVariable String name) {
        dataSourceConfigService.remove(name);
        return R.ok("Datasource removed");
    }

    /**
     * @param request connection parameters (no persist)
     * @return test outcome message
     */
    @PostMapping("/test")
    public R<String> testConnection(@RequestBody DataSourceConnectionTestRequest request) {
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
     * @param name persisted datasource key
     * @return test outcome message
     */
    @PostMapping("/{name}/test")
    public R<String> testConnectionByName(@PathVariable String name) {
        try {
            return R.ok(dataSourceConfigService.testConnectionByName(name));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
