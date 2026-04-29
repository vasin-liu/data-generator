/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.Templates;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.json.TemplateJsonCodec;
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
import org.gensokyo.kit.security.Md5Kit;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 模板管理接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/9/20 , Version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/template")
@Validated
@RequiredArgsConstructor
public class TemplateController {
    private final TemplateRepository repository;
    private final YamlParser yamlParser;
    private final Templates templates;

    @PostMapping("/updateById")
    public R<String> updateById(@Validated @RequestBody UpdateTemplateQO qo) {
        var po = repository.findById(qo.getId()).orElse(null);
        if (Objects.isNull(po)) {
            return R.fail(String.format("模板 '%s' 不存在", qo.getId()));
        }
        var vo = yamlParser.parse(qo.getYaml(), TemplateVO.class);
        if (Objects.isNull(vo)) {
            return R.fail("文件内容解析失败，请检查文件内容格式是否正确");
        }
        po.setName(vo.getName());
        vo.setId(po.getId());
        po.setContentJson(TemplateJsonCodec.write(vo));
        po.setContentYaml(qo.getYaml());
        repository.save(po);
        return R.ok(String.format("模板 '%s' 已更新", qo.getId()));
    }

    @PostMapping("/reloadAllFromFile")
    public R<String> reloadFromFile() {
        var list = repository.saveAllAndFlush(templates.reloadAll());
        return R.ok(String.format("所有模板重新加载完成，总共 %s 个文件", list.size()));
    }

    @PostMapping("/uploadTemplate")
    public R<String> uploadTemplate(@NotNull @RequestParam("file") MultipartFile file) {
        try (var is = file.getInputStream()) {
            var content = IOKit.toString(is, StandardCharsets.UTF_8);
            var vo = yamlParser.parse(content, TemplateVO.class);
            var fileName = file.getName();
            var id = RandomKit.snowFlake().nextId();
            var prefix = "upload";
            if (StrKit.isBlank(fileName)) {
                fileName = prefix + File.separator + id;
            } else {
                fileName = prefix + File.separator + fileName;
            }
            var po = new TemplatePO();
            po.setId(id);
            po.setName(vo.getName());
            po.setFileExt(FileKit.getExtension(fileName));
            po.setFileName(fileName);
            po.setPathMd5(Md5Kit.encrypt(fileName));
            po.setContentYaml(content);
            po.setContentJson(TemplateJsonCodec.write(vo));
            repository.save(po);
            return R.ok("文件上传成功");
        } catch (Exception e) {
            throw new DataGeneratorException("模板文件上传失败", e);
        }
    }
}
