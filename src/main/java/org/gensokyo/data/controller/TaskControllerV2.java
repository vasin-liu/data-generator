/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.controller;

import com.baomidou.dynamic.datasource.annotation.DS;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.Context;
import org.gensokyo.data.cache.ConfigCache;
import org.gensokyo.data.pipeline.DefaultDataPipelineFactory;
import org.gensokyo.data.po.TemplatePO;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.value.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;


/**
 * 任务控制器V2
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@RestController
@RequestMapping("/v2/task")
@Validated
@RequiredArgsConstructor
public class TaskControllerV2 {

    private final DefaultDataPipelineFactory defaultDataPipelineFactory;
    private final ConfigCache cache;

    @DS("data-generator")
    @GetMapping("/run/{templateName}")
    public String runTask(@NotBlank @PathVariable String templateName) {
        TemplatePO template = cache.get(templateName);
        if (Objects.nonNull(template)) {
            long id = RandomKit.id();
            defaultDataPipelineFactory.startup(new Context(template, Value.EMPTY));
            return String.valueOf(id);
        } else {
            return String.format("模板 '%s' 不存在", templateName);
        }
    }
}
