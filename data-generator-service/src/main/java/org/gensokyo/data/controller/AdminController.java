/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.controller;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.Templates;
import org.gensokyo.data.constant.Status;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.qo.UpdateTemplateQO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.io.FileKit;
import org.gensokyo.kit.io.IOKit;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.Objects;

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
    private final TemplateRepository repository;
    private final YamlParser yamlParser;
    private final Templates templates;
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

    @PostMapping("/updateById")
    public R<String> updateById(@Validated @RequestBody UpdateTemplateQO qo) {
        var po = repository.findById(qo.getId()).orElse(null);
        if (Objects.isNull(po)) {
            return R.fail(String.format("模板 '%s' 不存在", qo.getId()));
        }
        var vo = yamlParser.parse(qo.getYaml(), TemplateVO.class);
        po.setName(vo.getName());
        if (StrKit.isNotBlank(qo.getFileName())) {
            var fn = FileKit.getNameWithoutExtension(qo.getFileName());
            var fe = FileKit.getExtension(qo.getFileName());
            po.setFileName(fn);
            if (StrKit.isNotBlank(fe)) {
                po.setFileExt(fe);
            }
        }
        vo.setId(po.getId());
        po.setJsonContent(JsonKit.write(vo));
        po.setYamlContent(qo.getYaml());
        repository.save(po);
        return R.ok(String.format("模板 '%s' 已更新", qo.getId()));
    }

    @PostMapping("/reloadAllFromFile")
    public R<String> reloadFromFile() {
        var list = repository.saveAll(templates.reloadAll());
        return R.ok(String.format("所有模板重新加载完成，总共 %s 个文件", list.size()));
    }

    @PostMapping("/uploadTemplate")
    public R<String> uploadTemplate(@NotNull @RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "persistent", required = false, defaultValue = "false") boolean persistent) {
        try (var is = file.getInputStream()) {
            var content = IOKit.toString(is, StandardCharsets.UTF_8);
            var vo = yamlParser.parse(content, TemplateVO.class);
            var po = new TemplatePO();
            po.setId(RandomKit.snowFlake().nextId());
            po.setName(vo.getName());
            po.setFileExt(FileKit.getExtension(file.getName()));
            po.setFileName(file.getName());
            po.setYamlContent(content);
            po.setJsonContent(JsonKit.write(vo));
            po.setStatus(Status.S0A);
            repository.save(po);
            return R.ok("文件上传成功");
        } catch (Exception e) {
            throw new DataGeneratorException("模板文件上传失败", e);
        }
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
