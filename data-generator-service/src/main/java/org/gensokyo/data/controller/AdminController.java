/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.controller;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.DriverManager;

/**
 * 管理接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/9/20 , Version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@Validated
@RequiredArgsConstructor
public class AdminController {

    private final DynamicRoutingDataSource dynamicRoutingDataSource;

    @Value("${gensokyo.drivers.directory:./uploaded-drivers}")
    private String uploadDir;

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
            DruidDataSource  dataSource = new DruidDataSource();
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
        File destFile = new File(uploadDir + "/" + fileName);

        // 确保目标目录存在
        if (destFile.getParentFile().mkdirs()) {
            driverFile.transferTo(destFile);
        }

        return destFile;
    }

    private void loadDriverJar(String jarFilePath, String driverClassName) throws Exception {
        // 动态加载JAR
        URL jarUrl = new URL("file:" + jarFilePath);
        URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl});
        Class<?> driverClass = Class.forName(driverClassName, true, loader);
        DriverManager.registerDriver((java.sql.Driver) driverClass.getDeclaredConstructor().newInstance());
    }
}
