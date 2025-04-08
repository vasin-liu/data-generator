/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.controller;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.TemplateContext;
import org.gensokyo.data.generator.BlockWhenQueueFullHandler;
import org.gensokyo.data.generator.MdcTaskDecorator;
import org.gensokyo.data.model.dto.TemplateDTO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.pipeline.DefaultDataPipelineFactory;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 任务控制器
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
    private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    @PostConstruct
    public void init() {
        executor.setTaskDecorator(new MdcTaskDecorator());
        //核心线程池大小
        executor.setCorePoolSize(5);
        //最大线程数
        executor.setMaxPoolSize(5);
        //队列容量
        executor.setQueueCapacity(10);
        //活跃时间
        executor.setKeepAliveSeconds(120);
        //线程名字前缀
        executor.setThreadNamePrefix("DG-TASK-");
        // 设置线程池关闭的时候等待所有任务都完成再继续销毁其他的Bean
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        //队列满时阻塞主线程提交任务动作
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
            return R.fail(String.format("模板 '%s' 不存在", templateName));
        }

        if (result.size() > 1) {
            var msg = result.stream()
                    .map(t -> t.getId() + Const.COLON + t.getName())
                    .collect(Collectors.joining(Const.COMMA));
            return R.fail(String.format("存在多个模板名为 '%s' 的模板，请根据模板ID启动任务：%s", templateName, msg));
        }

        var template = JsonKit.read(result.get(0).getContentJson(), TemplateVO.class);
        run(template);
        return R.ok(String.format("模板 '%s' 已启动数据生成任务, 模板ID：%s, 实例ID：%s",
                template.getName(), template.getId(), template.getInstanceId()));
    }

    @GetMapping("/runById/{templateId}")
    public R<String> runById(@NotNull @PathVariable Long templateId) {
        var result = repository.findById(templateId).orElse(null);

        if (Objects.isNull(result)) {
            return R.fail(String.format("模板 '%s' 不存在", templateId));
        }

        var template = JsonKit.read(result.getContentJson(), TemplateVO.class);
        run(template);
        return R.ok(String.format("模板 '%s' 已启动数据生成任务, 模板ID：%s, 实例ID：%s",
                template.getName(), template.getId(), template.getInstanceId()));
    }

    private void run(final TemplateVO template) {
        template.setInstanceId(RandomKit.snowFlake().nextId());
        executor.execute(() -> {
            var ctx = new TemplateContext(template, Value.EMPTY);
            try {
                defaultDataPipelineFactory.startup(ctx);
            } catch (Exception e) {
                log.error("数据生成任务执行出现异常：", e);
            } finally {
                defaultDataPipelineFactory.shutdown(ctx);
            }
        });
    }
}
