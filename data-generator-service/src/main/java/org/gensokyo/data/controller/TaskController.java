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
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.TemplateContext;
import org.gensokyo.data.model.dto.TemplateDTO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.pipeline.DefaultDataPipelineFactory;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
        var result = repository.findByName(templateName);
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
    public R<String> runById(@NotNull @PathVariable Long templateId,
                             @RequestParam(value = "cleanup", required = false, defaultValue = "true") Boolean cleanup) {
        var result = repository.findById(templateId).orElse(null);

        if (Objects.isNull(result)) {
            return R.fail(String.format("模板 '%s' 不存在", templateId));
        }

        var template = JsonKit.read(result.getJsonContent(), TemplateVO.class);
        run(template, cleanup);
        return R.ok(String.format("模板 '%s' 已启动数据生成任务", templateId));
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
