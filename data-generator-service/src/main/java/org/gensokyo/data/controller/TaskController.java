/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.Templates;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.constant.Status;
import org.gensokyo.data.context.TemplateContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.dto.TemplateDTO;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.qo.UpdateTemplateQO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.pipeline.DefaultDataPipelineFactory;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.value.Value;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.io.FileKit;
import org.gensokyo.kit.io.IOKit;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 任务控制器V2
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/task")
@Validated
@RequiredArgsConstructor
public class TaskController {
    private final DefaultDataPipelineFactory defaultDataPipelineFactory;
    private final TemplateRepository repository;
    private final YamlParser yamlParser;
    private final Templates templates;

    @GetMapping("/list")
    public R<List<TemplateDTO>> list() {
        var all = repository.findAll()
                .stream()
                .map(TemplateDTO::new)
                .toList();
        return R.ok(all);
    }

    @GetMapping("/findByName/{templateName}")
    public R<List<TemplateDTO>> findByName(@NotBlank @PathVariable String templateName) {
        var result = repository.findByNameContaining(templateName)
                .stream()
                .map(TemplateDTO::new)
                .toList();
        return R.ok(result);
    }

    @GetMapping("/findById/{templateId}")
    public R<TemplateDTO> findById(@NotBlank @PathVariable Long templateId) {
        var result = repository.findById(templateId)
                .map(TemplateDTO::new)
                .orElse(null);
        return R.ok(result);
    }

    @GetMapping("/runByName/{templateName}")
    public R<String> runByName(@NotBlank @PathVariable String templateName,
                               @RequestParam(value = "cleanup", required = false, defaultValue = "true") Boolean cleanup) {
        var result = repository.findByNameContaining(templateName);
        if (CollectKit.isEmpty(result)) {
            return R.fail(String.format("模板 '%s' 不存在", templateName));
        }

        if (result.size() > 1) {
            var msg = result.stream()
                    .map(t -> t.getId() + Const.COLON + t.getName())
                    .collect(Collectors.joining(Const.COMMA));
            return R.fail(String.format("存在多个模板名为 '%s' 的模板，请根据模板ID启动任务：%s", templateName, msg));
        }

        var template = JsonKit.read(result.get(0).getJsonContent(), TemplateVO.class);
        run(template, cleanup);
        return R.ok(String.format("模板 '%s' 已启动数据生成任务", templateName));
    }

    @GetMapping("/runById/{templateId}")
    public R<String> runById(@NotBlank @PathVariable Long templateId,
                             @RequestParam(value = "cleanup", required = false, defaultValue = "true") Boolean cleanup) {
        var result = repository.findById(templateId).orElse(null);

        if (Objects.isNull(result)) {
            return R.fail(String.format("模板 '%s' 不存在", templateId));
        }

        var template = JsonKit.read(result.getJsonContent(), TemplateVO.class);
        run(template, cleanup);
        return R.ok(String.format("模板 '%s' 已启动数据生成任务", templateId));
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

    private void run(final TemplateVO template, final boolean cleanup) {
        var ctx = new TemplateContext(template, Value.EMPTY);
        try {
            if (Boolean.TRUE.equals(cleanup)) {
                defaultDataPipelineFactory.cleanup(ctx);
            }
            defaultDataPipelineFactory.startup(ctx);
        } catch (Exception e) {
            log.error("数据生成任务执行出现异常：", e);
        } finally {
            defaultDataPipelineFactory.shutdown(ctx);
        }
    }
}
