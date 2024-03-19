/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.po.TemplatePO;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.character.StrKit;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 元数据缓存
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@Slf4j
public class ConfigCache implements InitializingBean {
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final DataGeneratorProperties props;
    private final Cache<String, TemplatePO> cache;
    private final YamlParser yamlParser;

    public ConfigCache(DataGeneratorProperties props, YamlParser yamlParser) {
        this.props = Objects.requireNonNull(props);
        this.yamlParser = Objects.requireNonNull(yamlParser);
        cache = CacheBuilder.newBuilder()
                //最大缓存数
                .maximumSize(props.getMetaCacheMaximumSize())
                .recordStats()
                .removalListener(((RemovalListener<String, TemplatePO>) notification ->
                        log.info(notification.getKey() + ":" + notification.getCause())))
                .build();

    }

    public boolean reloadAll() {
        Stream.of(Optional.ofNullable(props.getMetaFolders()).orElse(new String[0]))
                .flatMap(location -> Stream.of(getResources(location)))
                //忽略特定开头的文件
                .filter(r -> {
                    if (Objects.requireNonNull(r.getFilename()).startsWith("___")) {
                        log.warn("已忽略模板文件： {} ", r.getFilename());
                        return false;
                    }
                    return true;
                })
                .filter(r -> {
                    if (Objects.requireNonNull(r.getFilename()).startsWith("!")) {
                        log.warn("已忽略模板文件： {} ", r.getFilename());
                        return false;
                    }
                    return true;
                })
                .forEach(this::readToCache);
        log.info("已加载元数据信息总计 {} 个", cache.size());
        return true;
    }

    public boolean reload(String templateName) {
        if (StrKit.isBlank(templateName)) {
            return false;
        }
        Stream.of(Optional.ofNullable(props.getMetaFolders()).orElse(new String[0]))
                .flatMap(location -> Stream.of(getResources(location)))
                .filter(r -> Objects.equals(r.getFilename(), templateName))
                .forEach(this::readToCache);
        log.info("已重新加载模板文件： {} ", templateName);
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (reloadAll()) {
            log.info("元数据缓存初始化完成");
        } else {
            log.error("元数据缓存初始化失败");
        }
    }

    @SneakyThrows
    private void readToCache(Resource r) {
        TemplatePO meta = yamlParser.parse(r.getFile(), TemplatePO.class);
        if (Objects.nonNull(meta)) {
            cache.put(meta.getName(), meta);
            log.info("已加载模板文件： {} ", r.getFilename());
        } else {
            log.error("加载模板文件失败： {} ", r.getFilename());
        }
    }

    public void put(String key, TemplatePO meta) {
        cache.put(key, meta);
    }

    public TemplatePO get(String key) {
        return cache.getIfPresent(key);
    }

    public void remove(String key) {
        cache.invalidate(key);
    }

    public Cache<String, TemplatePO> cache() {
        return this.cache;
    }

    private Resource[] getResources(String location) {
        try {
            return resolver.getResources(location);
        } catch (IOException e) {
            return new Resource[0];
        }
    }
}
