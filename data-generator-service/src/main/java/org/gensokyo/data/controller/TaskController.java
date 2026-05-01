/*
 * Copyright 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 */
package org.gensokyo.data.controller;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.calcite.TemplateV2Runner;
import org.gensokyo.data.calcite.TemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.generator.BlockWhenQueueFullHandler;
import org.gensokyo.data.generator.MdcTaskDecorator;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.dto.TemplateDTO;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.pipeline.DefaultDataPipelineTaskFactory;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateDefinitionDetector;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.template.TemplateV2Validator;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.collect.CollectKit;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/task")
@Validated
@RequiredArgsConstructor
public class TaskController {
    private final DataGeneratorProperties properties;
    private final DefaultDataPipelineTaskFactory defaultDataPipelineTaskFactory;
    private final TemplateRepository repository;
    private final YamlParser yamlParser;
    private final TemplateV2Runner templateV2Runner;
    private final TemplateV2RuntimeRegistryProvider templateV2RuntimeRegistryProvider;
    private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    @PostConstruct
    public void init() {
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("DG-TASK-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.setRejectedExecutionHandler(new BlockWhenQueueFullHandler());
        executor.initialize();
    }

    @PreDestroy
    public void preDestroy() {
        executor.shutdown();
    }

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
    public R<String> runByName(@NotBlank @PathVariable String templateName) {
        var result = repository.findByName(templateName);
        if (CollectKit.isEmpty(result)) {
            return R.fail(String.format("Template '%s' does not exist", templateName));
        }

        if (result.size() > 1) {
            var msg = result.stream()
                    .map(t -> t.getId() + Const.COLON + t.getName())
                    .collect(Collectors.joining(Const.COMMA));
            return R.fail(String.format("Multiple templates named '%s' exist, use template id instead: %s", templateName, msg));
        }

        var runtime = run(result.get(0));
        return R.ok(String.format("Template '%s' started. templateId=%s, instanceId=%s",
                runtime.name(), runtime.id(), runtime.instanceId()));
    }

    @GetMapping("/runById/{templateId}")
    public R<String> runById(@NotNull @PathVariable Long templateId) {
        var result = repository.findById(templateId).orElse(null);
        if (Objects.isNull(result)) {
            return R.fail(String.format("Template '%s' does not exist", templateId));
        }

        var runtime = run(result);
        return R.ok(String.format("Template '%s' started. templateId=%s, instanceId=%s",
                runtime.name(), runtime.id(), runtime.instanceId()));
    }

    private TemplateRuntimeInfo run(TemplatePO entity) {
        String yaml = entity.getContentYaml();
        TemplateV2DraftVO v2Draft = tryParse(yaml, TemplateV2DraftVO.class);
        TemplateVO v1Template = tryParse(yaml, TemplateVO.class);
        TemplateDefinitionKind kind = TemplateDefinitionDetector.detect(v1Template, v2Draft);
        if (kind == TemplateDefinitionKind.V2 && v2Draft != null) {
            return runV2(entity.getId(), v2Draft);
        }

        TemplateVO template = v1Template != null ? v1Template : TemplateJsonCodec.read(entity.getContentJson());
        return runV1(template);
    }

    private TemplateRuntimeInfo runV1(TemplateVO template) {
        template.setInstanceId(RandomKit.snowFlake().nextId());
        executor.submit(defaultDataPipelineTaskFactory.newInstance(template));
        return new TemplateRuntimeInfo(template.getId(), template.getName(), template.getInstanceId());
    }

    private TemplateRuntimeInfo runV2(Long templateId, TemplateV2DraftVO draft) {
        templateV2RuntimeRegistryProvider.current();
        TemplateV2VO template = TemplateV2Normalizer.normalize(draft);
        template.setId(templateId);
        template.setInstanceId(RandomKit.snowFlake().nextId());
        TemplateV2Validator.validate(template);
        executor.submit(() -> templateV2Runner.run(template));
        return new TemplateRuntimeInfo(template.getId(), template.getName(), template.getInstanceId());
    }

    private <T> T tryParse(String yaml, Class<T> clazz) {
        try {
            return yamlParser.parse(yaml, clazz);
        } catch (Exception ignored) {
            return null;
        }
    }

    private record TemplateRuntimeInfo(Long id, String name, Long instanceId) {
    }
}
