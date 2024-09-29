/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gensokyo.data.model.vo.generator.GeneratorVO;
import org.gensokyo.data.model.vo.iterator.IteratorVO;
import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;

import java.io.Serializable;
import java.util.List;

/**
 * 元数据
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Setter
@Getter
@NoArgsConstructor
public class TemplateVO implements Serializable {

    /**
     * 模板ID，全局唯一，缓存的KEY以及调用的参数
     */
    private Long id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 迭代器配置，默认为数字迭代器
     */
    private IteratorVO iterator;

    /**
     * 生成器配置，默认为异步生成器
     */
    private GeneratorVO generator;

    /**
     * 字段配置
     */
    private List<FieldVO> fields;

    /**
     * 输出阶段配置
     */
    private WriteStageVO output;
}
