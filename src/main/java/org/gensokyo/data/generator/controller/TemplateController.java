/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.generator.cache.TemplateCache;
import org.gensokyo.data.generator.domain.TemplatePO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 模板控制器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/29 , Version 1.0.0
 */
@RestController
@RequestMapping("/template")
@Validated
@RequiredArgsConstructor
public class TemplateController {
    private final TemplateCache cache;
    private final ObjectMapper om = new ObjectMapper();

    @GetMapping("/view/{templateName}")
    public String view(@NotBlank @PathVariable String templateName) throws JsonProcessingException {
        TemplatePO template = cache.get(templateName);
        if (Objects.nonNull(template)) {
            return om.writeValueAsString(template);
        } else {
            return String.format("模板 '%s' 不存在", templateName);
        }
    }

    @GetMapping("/reload/{templateName}")
    public String reload(@NotBlank @PathVariable String templateName) {
        if (cache.reload(templateName)) {
            return String.format("模板 '%s' 重新加载成功", templateName);
        } else {
            return String.format("模板 '%s' 重新加载失败", templateName);
        }
    }
}
