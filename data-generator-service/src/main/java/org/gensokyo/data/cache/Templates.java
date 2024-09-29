/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.constant.Status;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.io.FileKit;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
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
@RequiredArgsConstructor
public class Templates implements InitializingBean {
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final DataGeneratorProperties props;
    private final YamlParser yamlParser;
    private final TemplateRepository repository;

    public List<TemplatePO> reloadAll() {
        repository.deleteAll();
        return Stream.of(Optional.ofNullable(props.getMetaFolders()).orElse(new String[0]))
                .flatMap(location -> Stream.of(getResources(location)))
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

    @Override
    public void afterPropertiesSet() throws Exception {
        repository.saveAll(reloadAll());
        log.info("元数据缓存初始化完成");
    }

    private TemplatePO parse(Resource r) {
        try {
            var file = r.getFile();
            var t = yamlParser.parse(file, TemplateVO.class);
            var fileName = file.getName();
            var fileNameExt = FileKit.getExtension(fileName);
            var entity = new TemplatePO();
            var id = RandomKit.snowFlake().nextId();
            t.setId(id);
            entity.setId(id);
            entity.setName(t.getName());
            entity.setFileName(fileName);
            entity.setFileExt(fileNameExt);
            entity.setJsonContent(JsonKit.write(t));
            entity.setYamlContent(Files.readString(file.toPath()));
            entity.setStatus(Status.S0A);
            return entity;
        } catch (Exception e) {
            log.error("解析模板文件失败：", e);
        }
        return null;
    }

    private Resource[] getResources(String location) {
        try {
            return resolver.getResources(location);
        } catch (IOException e) {
            return new Resource[0];
        }
    }
}
