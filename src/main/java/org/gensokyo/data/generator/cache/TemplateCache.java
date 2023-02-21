/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.cache;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.generator.config.DataGeneratorProperties;
import org.gensokyo.data.generator.constant.FieldType;
import org.gensokyo.data.generator.constant.ReaderType;
import org.gensokyo.data.generator.constant.ScriptType;
import org.gensokyo.data.generator.constant.WriterType;
import org.gensokyo.data.generator.domain.TemplatePO;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.data.generator.yaml.CaseInsensitiveEnumSerializer;
import org.gensokyo.data.generator.yaml.ClassSerializer;
import org.gensokyo.kit.character.StrKit;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.FileReader;
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
public class TemplateCache implements InitializingBean {
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final DataGeneratorProperties props;
    private final Cache<String, TemplatePO> cache;
    private final YamlConfig config;

    public TemplateCache(DataGeneratorProperties props) {
        this.props = props;
        cache = CacheBuilder.newBuilder()
                //最大缓存数
                .maximumSize(props.getMetaCacheMaximumSize())
                .recordStats()
                .removalListener(((RemovalListener<String, TemplatePO>) notification ->
                        log.info(notification.getKey() + ":" + notification.getCause())))
                .build();
        config = new YamlConfig();
        config.setScalarSerializer(ScriptType.class, new CaseInsensitiveEnumSerializer<>(ScriptType.class));
        config.setScalarSerializer(FieldType.class, new CaseInsensitiveEnumSerializer<>(FieldType.class));
        config.setScalarSerializer(ReaderType.class, new CaseInsensitiveEnumSerializer<>(ReaderType.class));
        config.setScalarSerializer(WriterType.class, new CaseInsensitiveEnumSerializer<>(WriterType.class));
        config.setScalarSerializer(Class.class, new ClassSerializer());
    }

    public boolean reloadAll() {
        Stream.of(Optional.ofNullable(props.getMetaFolders()).orElse(new String[0]))
                .flatMap(location -> Stream.of(getResources(location)))
                //忽略特定开头的文件
                .filter(r -> !Objects.requireNonNull(r.getFilename()).startsWith("___"))
                .forEach(this::readToCache);
        log.info("已加载元数据信息总计 [{}] 个", cache.size());
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
        log.info("已重新加载模板文件： [{}] ", templateName);
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        reloadAll();
    }

    private void readToCache(Resource r) {
        try {
            var reader = new YamlReader(new FileReader(r.getFile()), config);
            TemplatePO meta = reader.read(TemplatePO.class);
            if (Objects.nonNull(meta)) {
                cache.put(meta.getName(), meta);
            }
        } catch (IOException e) {
            throw new DataGeneratorException(e);
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
