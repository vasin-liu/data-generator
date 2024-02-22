/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.controller;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.generator.DefaultDataGenerator;
import org.gensokyo.data.generator.cache.TemplateCache;
import org.gensokyo.data.generator.constant.TaskStatus;
import org.gensokyo.data.generator.controller.req.BatchTaskQo;
import org.gensokyo.data.generator.domain.TaskPO;
import org.gensokyo.data.generator.domain.TemplatePO;
import org.gensokyo.data.generator.factory.*;
import org.gensokyo.data.generator.listener.GeneratorListener;
import org.gensokyo.data.generator.repository.TaskRepository;
import org.gensokyo.data.generator.util.RandomKit;
import org.gensokyo.kit.collect.CollectKit;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Objects;

/**
 * 任务控制器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/9 , Version 1.0.0
 */
@RestController
@RequestMapping("/task")
@Validated
@RequiredArgsConstructor
public class TaskController {
    private final ExecutorFactory executorFactory;
    private final ReaderFactory readerFactory;
    private final WriterFactory writerFactory;
    private final ConverterFactory converterFactory;
    private final ScriptFactory scriptFactory;
    private final TemplateCache cache;
    private final ThreadPoolTaskExecutor executor;
    private final TaskRepository taskRepository;
    private static final String DATA_GENERATOR_DS = "data-generator";

    @DS(DATA_GENERATOR_DS)
    @GetMapping("/run/{templateName}")
    public String runTask(@NotBlank @PathVariable String templateName) {
        TemplatePO template = cache.get(templateName);
        if (Objects.nonNull(template)) {
            long id = RandomKit.id();
            var task = new TaskPO();
            task.setId(id);
            task.setName(templateName);
            task.setStatus(TaskStatus.INIT);
            task.setStartTime(new Date());
            taskRepository.save(task);
            var dg = new DefaultDataGenerator(executorFactory, readerFactory, writerFactory,
                    converterFactory, scriptFactory, template);
            dg.registerListener(new DbGeneratorListener(id));
            executor.submit(dg);
            return String.valueOf(task.getId());
        } else {
            return String.format("模板 '%s' 不存在", templateName);
        }
    }

    @DS(DATA_GENERATOR_DS)
    @GetMapping("/run/batch")
    public String runBatchTask(@NotNull @RequestBody BatchTaskQo qo) {
        if (CollectKit.isNotEmpty(qo.getTemplateNames())) {
            qo.getTemplateNames().stream()
                    .map(cache::get)
                    .filter(Objects::nonNull)
                    .forEach(template -> {
                        var dg = new DefaultDataGenerator(executorFactory, readerFactory, writerFactory,
                                converterFactory, scriptFactory, template);
                        executor.submit(dg);
                    });
            return "批量任务启动成功";
        }
        return "当前没有任何任务启动";
    }

    @DS(DATA_GENERATOR_DS)
    @GetMapping("/view/{taskId}")
    public TaskPO getTask(@NotNull @PathVariable Long taskId) {
        var task = taskRepository.findById(taskId);
        return task.orElse(null);
    }

    @RequiredArgsConstructor
    private class DbGeneratorListener implements GeneratorListener {
        private final Long id;

        @Override
        public void onReady() {
            try {
                DynamicDataSourceContextHolder.push(DATA_GENERATOR_DS);
                var tpo = taskRepository.findById(id);
                tpo.ifPresent(po -> {
                    po.setStatus(TaskStatus.STARTED);
                    taskRepository.save(po);
                });
            } finally {
                DynamicDataSourceContextHolder.clear();
            }
        }

        @Override
        public void onProcessing(Long count) {
            try {
                DynamicDataSourceContextHolder.push(DATA_GENERATOR_DS);
                var tpo = taskRepository.findById(id);
                tpo.ifPresent(po -> {
                    po.setStatus(TaskStatus.PROCESSING);
                    po.setCount(po.getCount() + count);
                    taskRepository.save(po);
                });
            } finally {
                DynamicDataSourceContextHolder.clear();
            }
        }

        @Override
        public void onComplete(Long total) {
            try {
                DynamicDataSourceContextHolder.push(DATA_GENERATOR_DS);
                var tpo = taskRepository.findById(id);
                tpo.ifPresent(po -> {
                    po.setStatus(TaskStatus.COMPLETE);
                    po.setCount(total);
                    po.setEndTime(new Date());
                    taskRepository.save(po);
                });
            } finally {
                DynamicDataSourceContextHolder.clear();
            }
        }

        @Override
        public void onError(Throwable ex) {
            try {
                DynamicDataSourceContextHolder.push(DATA_GENERATOR_DS);
                var tpo = taskRepository.findById(id);
                tpo.ifPresent(po -> {
                    po.setStatus(TaskStatus.ERROR);
                    po.setReason(ex.getMessage());
                    po.setEndTime(new Date());
                    taskRepository.save(po);
                });
            } finally {
                DynamicDataSourceContextHolder.clear();
            }
        }
    }
}
