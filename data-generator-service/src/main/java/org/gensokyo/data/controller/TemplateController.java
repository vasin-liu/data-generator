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
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.qo.PreviewTemplateV2QO;
import org.gensokyo.data.model.qo.UpdateTemplateQO;
import org.gensokyo.data.model.qo.ValidateTemplateQO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateDefinitionDetector;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.TemplateV2ControlPlaneService;
import org.gensokyo.data.template.TemplateV2PlanExplain;
import org.gensokyo.data.template.TemplateV2PreviewDTO;
import org.gensokyo.data.template.TemplateV2ValidationResult;
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
    private final TemplateV2RuntimeRegistryProvider templateV2RuntimeRegistryProvider;
    private final TemplateV2ControlPlaneService templateV2ControlPlaneService;

    @PostMapping("/updateById")
    public R<String> updateById(@Validated @RequestBody UpdateTemplateQO qo) {
        var po = repository.findById(qo.getId()).orElse(null);
        if (Objects.isNull(po)) {
            return R.fail(String.format("模板 '%s' 不存在", qo.getId()));
        }
        var parsed = parseTemplate(qo.getYaml());
        if (Objects.isNull(parsed)) {
            return R.fail("文件内容解析失败，请检查文件内容格式是否正确");
        }
        po.setName(parsed.name());
        po.setContentJson(parsed.contentJson());
        po.setContentYaml(qo.getYaml());
        repository.save(po);
        return R.ok(String.format("模板 '%s' 已更新", qo.getId()));
    }

    @PostMapping("/reloadAllFromFile")
    public R<String> reloadFromFile() {
        var list = repository.saveAllAndFlush(templates.reloadAll());
        return R.ok(String.format("所有模板重新加载完成，总共 %s 个文件", list.size()));
    }

    @PostMapping("/refreshV2Runtime")
    public R<String> refreshV2Runtime() {
        templateV2RuntimeRegistryProvider.refresh();
        return R.ok("V2 runtime registry refreshed");
    }

    @PostMapping("/uploadTemplate")
    public R<String> uploadTemplate(@NotNull @RequestParam("file") MultipartFile file) {
        try (var is = file.getInputStream()) {
            var content = IOKit.toString(is, StandardCharsets.UTF_8);
            var parsed = parseTemplate(content);
            if (Objects.isNull(parsed)) {
                return R.fail("文件内容解析失败，请检查文件内容格式是否正确");
            }
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
            po.setName(parsed.name());
            po.setFileExt(FileKit.getExtension(fileName));
            po.setFileName(fileName);
            po.setPathMd5(Md5Kit.encrypt(fileName));
            po.setContentYaml(content);
            po.setContentJson(parsed.contentJson());
            repository.save(po);
            return R.ok("文件上传成功");
        } catch (Exception e) {
            throw new DataGeneratorException("模板文件上传失败", e);
        }
    }

    /**
     * Validates a Template V2 draft YAML (structural rules and execution-shape warnings).
     *
     * @param qo request with draft YAML
     * @return validation outcome; HTTP wrapper is success when YAML parses and validation runs
     */
    @PostMapping("/v2/validate")
    public R<TemplateV2ValidationResult> validateV2(@Validated @RequestBody ValidateTemplateQO qo) {
        TemplateV2DraftVO draft = tryParse(qo.getYaml(), TemplateV2DraftVO.class);
        if (Objects.isNull(draft)) {
            return R.fail("YAML could not be parsed as Template V2 draft");
        }
        TemplateV2ValidationResult result = templateV2ControlPlaneService.validate(draft);
        return R.ok("Validation completed", result);
    }

    /**
     * Explains a persisted template's effective V2 plan (sources, Calcite notes).
     *
     * @param templateId persisted template id
     * @return bounded plan explain block
     */
    @GetMapping("/v2/explain/{templateId}")
    public R<TemplateV2PlanExplain> explainV2(@NotNull @PathVariable Long templateId) {
        try {
            TemplateV2PlanExplain explain = templateV2ControlPlaneService.explain(templateId);
            return R.ok("Explain generated", explain);
        }
        catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * Bounded sample run for a persisted Template V2 definition.
     *
     * @param templateId persisted template id
     * @param options    optional row cap (default {@code 50})
     * @return bounded preview schema and sample rows
     */
    @PostMapping("/v2/preview/{templateId}")
    public R<TemplateV2PreviewDTO> previewV2(
            @NotNull @PathVariable Long templateId,
            @RequestBody(required = false) PreviewTemplateV2QO options) {
        Integer maxRows = options == null ? null : options.getMaxRows();
        try {
            TemplateV2PreviewDTO preview = templateV2ControlPlaneService.preview(templateId, maxRows);
            return R.ok("Preview completed", preview);
        }
        catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    private ParsedTemplate parseTemplate(String yaml) {
        var v2 = tryParse(yaml, TemplateV2DraftVO.class);
        var v1 = tryParse(yaml, TemplateVO.class);
        var kind = TemplateDefinitionDetector.detect(v1, v2);
        if (kind == TemplateDefinitionKind.V2 && Objects.nonNull(v2)) {
            return new ParsedTemplate(v2.getName(), TemplateJsonCodec.write(v2));
        }
        if (Objects.nonNull(v1)) {
            return new ParsedTemplate(v1.getName(), TemplateJsonCodec.write(v1));
        }
        return null;
    }

    private <T> T tryParse(String yaml, Class<T> clazz) {
        try {
            return yamlParser.parse(yaml, clazz);
        } catch (Exception ignored) {
            return null;
        }
    }

    private record ParsedTemplate(String name, String contentJson) {
    }
}
