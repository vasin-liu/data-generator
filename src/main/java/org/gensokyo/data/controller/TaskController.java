/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.controller;

import com.baomidou.dynamic.datasource.annotation.DS;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.cache.ConfigCache;
import org.gensokyo.data.context.TemplateContext;
import org.gensokyo.data.pipeline.DefaultDataPipelineFactory;
import org.gensokyo.data.po.TemplatePO;
import org.gensokyo.data.value.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;


/**
 * 任务控制器V2
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@RestController
@RequestMapping("/task")
@Validated
@RequiredArgsConstructor
public class TaskController {

    private final DefaultDataPipelineFactory defaultDataPipelineFactory;
    private final ConfigCache cache;

    @DS("data-generator")
    @GetMapping("/run/{templateName}")
    public String runTask(@NotBlank @PathVariable String templateName,
                          @RequestParam("cleanup") Boolean cleanup) {
        TemplatePO template = cache.get(templateName);
        if (Objects.nonNull(template)) {
            var ctx = new TemplateContext(template, Value.EMPTY);
            if (Boolean.TRUE.equals(cleanup)) {
                defaultDataPipelineFactory.cleanup(ctx);
            }
            defaultDataPipelineFactory.startup(ctx);
            return String.format("任务 '%s' 已启动", templateName);
        } else {
            return String.format("模板 '%s' 不存在", templateName);
        }
    }
}
