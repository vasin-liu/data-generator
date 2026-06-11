/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.generator.GeneratorVO;

/**
 * 异步生成器配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/19 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(GeneratorVO.class)
@JsonSubType(value = "ASYNC")
public class AsyncGeneratorVO extends GeneratorVO {

    /**
     * 线程池配置
     */
    private ExecutorVO executor = new ExecutorVO();
}
