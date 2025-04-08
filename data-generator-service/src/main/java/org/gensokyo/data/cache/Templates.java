/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.io.FileKit;
import org.gensokyo.kit.json.JsonKit;
import org.gensokyo.kit.security.Md5Kit;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * 元数据缓存
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class Templates implements InitializingBean {
    private final DataGeneratorProperties props;
    private final YamlParser yamlParser;
    private final TemplateRepository repository;

    public List<TemplatePO> reloadAll() {
        repository.deleteAll();
        return parse(loadTemplateResources());
    }

    public FileAlterationObserver createResourceObserver() {
        String baseDir;
        if (isRunningFromJar()) {
            baseDir = Paths.get(System.getProperty("user.dir"), "..", "conf", "template").toString();
        } else {
            var url = Templates.class.getClassLoader().getResource("template");
            baseDir = Objects.requireNonNull(url).getPath();
        }
        // 创建观察者，自动递归监控template目录及其子目录
        FileAlterationObserver observer = new FileAlterationObserver(baseDir);
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                var template = parse(file);
                if (Objects.isNull(template)) {
                    return;
                }
                repository.saveAndFlush(template);
                log.info("模板文件 {} 的缓存记录已创建", file.getAbsolutePath());
            }

            @Override
            public void onFileChange(File file) {
                var template = parse(file);
                if (Objects.isNull(template)) {
                    return;
                }
                var templateInDb = repository.findByPathMd5(template.getPathMd5());
                if (Objects.nonNull(templateInDb)) {
                    repository.delete(templateInDb);
                }
                repository.saveAndFlush(template);
                log.info("模板文件 {} 的缓存记录已更新", file.getAbsolutePath());
            }

            @Override
            public void onFileDelete(File file) {
                var templateInDb = repository.findByPathMd5(Md5Kit.encrypt(file.getAbsolutePath()));
                if (Objects.nonNull(templateInDb)) {
                    repository.delete(templateInDb);
                    log.info("模板文件 {} 的缓存记录已删除", file.getAbsolutePath());
                }
            }

            @Override
            public void onDirectoryCreate(File directory) {
                var resources = fromFileSystem(directory.toPath());
                if (CollectKit.isEmpty(resources)) {
                    return;
                }
                var pos = parse(resources);
                if (CollectKit.isEmpty(pos)) {
                    return;
                }
                repository.deleteAllInBatch(pos);
                repository.saveAllAndFlush(pos);
                log.info("模板目录 {} 下的所有文件的缓存记录已创建", directory.getAbsolutePath());
            }

            @Override
            public void onDirectoryDelete(File directory) {
                log.info("模板目录 {} 已删除, 目录下的所有模板文件也一并删除", directory.getAbsolutePath());
            }
        });
        return observer;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        repository.saveAll(reloadAll());
        log.info("元数据缓存初始化完成");
        var monitor = new FileAlterationMonitor(1000, createResourceObserver());
        monitor.start();
        log.info("注册模板文件监听器完成");
    }

    public List<TemplatePO> parse(List<Resource> resources) {
        if (CollectKit.isEmpty(resources)) {
            return Collections.emptyList();
        }
        return resources
                .stream()
                //忽略特定开头的文件
                .filter(r -> {
                    boolean ignored = Arrays.stream(props.getIgnorePrefix())
                            .anyMatch(prefix -> Objects.requireNonNull(r.getFilename()).startsWith(prefix));
                    if (ignored) {
                        log.warn("已忽略模板文件： {} ", r.getFilename());
                        return false;
                    }
                    return true;
                })
                .map(this::parse)
                .filter(Objects::nonNull)
                .toList();
    }

    private TemplatePO parse(Resource r) {
        try {
            return parse(r.getFile());
        } catch (IOException e) {
            log.error("解析模板文件失败：", e);
        }
        return null;
    }

    private TemplatePO parse(File file) {
        try {
            var t = yamlParser.parse(file, TemplateVO.class);
            var id = RandomKit.snowFlake().nextId();
            t.setId(id);
            var fileName = file.getName();
            var entity = new TemplatePO();
            entity.setId(id);
            entity.setName(t.getName());
            entity.setFileName(fileName);
            entity.setFileExt(FileKit.getExtension(fileName));
            entity.setPathMd5(Md5Kit.encrypt(file.getPath()));
            var yamlContent = Files.readString(file.toPath());
            entity.setContentMd5(Md5Kit.encrypt(yamlContent));
            entity.setContentJson(JsonKit.write(t));
            entity.setContentYaml(yamlContent);
            return entity;
        } catch (Exception e) {
            log.error("解析模板文件失败：", e);
        }
        return null;
    }

    /**
     * 从外部模板目录递归加载所有资源。
     *
     * @param baseDir 外部模板目录
     * @return 返回 Resource 列表
     */
    private List<Resource> fromFileSystem(String baseDir) {
        if (StrKit.isBlank(baseDir)) {
            return Collections.emptyList();
        }
        var basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        return fromFileSystem(basePath);
    }

    /**
     * 从外部模板目录递归加载所有资源。
     *
     * @param basePath 外部模板目录
     * @return 返回 Resource 列表
     */
    private List<Resource> fromFileSystem(Path basePath) {
        var resourceList = new ArrayList<Resource>();
        try {
            if (Objects.isNull(basePath) || !Files.exists(basePath) || !Files.isDirectory(basePath)) {
                return resourceList;
            }
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    resourceList.add(new FileSystemResource(file.toFile()));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            log.error("读取模板资源文件出现异常：", e);
        }
        return resourceList;
    }

    /**
     * 从 classpath 下加载模板资源，支持递归搜索子目录。
     *
     * @param baseDir classpath 下模板基础目录
     * @return 返回 Resource 列表
     */
    private List<Resource> fromClasspath(String baseDir) {
        if (StrKit.isBlank(baseDir)) {
            return Collections.emptyList();
        }
        var resourceList = new ArrayList<Resource>();
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            var resources = resolver.getResources("classpath:" + baseDir);
            for (var resource : resources) {
                if (resource.isReadable()) {
                    resourceList.add(resource);
                }
            }
        } catch (Exception e) {
            log.error("读取模板资源文件出现异常：", e);
        }
        return resourceList;
    }

    private List<Resource> loadTemplateResources() {
        String baseDir;
        if (isRunningFromJar()) {
            baseDir = Paths.get(System.getProperty("user.dir"), "..", "conf", "template").toString();
            return fromFileSystem(baseDir);
        } else {
            baseDir = "/template/**/*.yaml";
            return fromClasspath(baseDir);
        }
    }

    private boolean isRunningFromJar() {
        var location = Templates.class.getProtectionDomain().getCodeSource().getLocation();
        return Objects.nonNull(location) && location.getPath().endsWith(".jar");
    }
}
