/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.context.TemplateContext;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.value.Value;
import org.springframework.context.ApplicationContext;

/**
 * 默认的数据生成任务工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/14 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultDataPipelineTaskFactory {
    private final ApplicationContext ctx;

    public DefaultDataPipelineTask newInstance(TemplateVO template) {
        return newInstance(template, Value.EMPTY);
    }

    public DefaultDataPipelineTask newInstance(TemplateVO template, Value dataset) {
        var task = new DefaultDataPipelineTask(new TemplateContext(template, dataset));
        var beanFactory = this.ctx.getAutowireCapableBeanFactory();
        beanFactory.autowireBean(task);
        beanFactory.initializeBean(task, task.getClass().getSimpleName());
        return task;
    }
}
