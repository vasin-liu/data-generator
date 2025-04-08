/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.controller;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import jakarta.validation.constraints.NotBlank;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.kit.collect.MapKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.util.Set;

/**
 * 数据源管理接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/9/20 , Version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/datasource")
@Validated
public class DataSourceController {

    @Setter(onMethod_ = {@Autowired(required = false)})
    private DynamicRoutingDataSource dynamicRoutingDataSource;

    @GetMapping("/database/list")
    public R<Set<String>> databaseDatasourceList() {
        var dataSources = dynamicRoutingDataSource.getDataSources();
        if (MapKit.isEmpty(dataSources)) {
            return R.ok(Set.of());
        }
        return R.ok(dataSources.keySet());
    }

    @PostMapping("/database/remove/{datasource}")
    public R<String> removeDatabaseDatasource(@NotBlank @PathVariable String datasource) {
        dynamicRoutingDataSource.removeDataSource(datasource);
        return R.ok("删除数据库数据源成功");
    }

    @PostMapping("/database/addDatasource")
    public R<String> addDatabaseDatasource(@RequestParam String name,
                                           @RequestParam String url,
                                           @RequestParam String username,
                                           @RequestParam String password,
                                           @RequestParam String driverClassName,
                                           @RequestParam MultipartFile driverFile) {
        try {
            File driverJar = uploadDriverFile(driverFile);
            loadDriverJar(driverJar.getAbsolutePath(), driverClassName);
            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setDriverClassName(driverClassName);
            dynamicRoutingDataSource.addDataSource(name, dataSource);

        } catch (Exception e) {
            throw new DataGeneratorException("添加数据库数据源失败", e);
        }
        return R.ok("添加数据库数据源成功");
    }

    private File uploadDriverFile(MultipartFile driverFile) throws IOException {
        String fileName = driverFile.getOriginalFilename();
        var destFile = Paths.get(System.getProperty("user.dir"), "..", "uploaded-drivers", fileName);
        var parent = destFile.getParent();
        if (!Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        driverFile.transferTo(destFile);

        return destFile.toFile();
    }

    private void loadDriverJar(String jarFilePath, String driverClassName) throws Exception {
        URL jarUrl = new URL("file:" + jarFilePath);
        URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl});
        Class<?> driverClass = Class.forName(driverClassName, true, loader);
        DriverManager.registerDriver((java.sql.Driver) driverClass.getDeclaredConstructor().newInstance());
    }
}
