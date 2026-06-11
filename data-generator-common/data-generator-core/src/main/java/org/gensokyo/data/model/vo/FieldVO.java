/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.model.vo.stage.StageVO;

import java.io.Serializable;
import java.util.List;

/**
 * 字段配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Setter
@Getter
public class FieldVO implements Serializable {
    /**
     * 字段名称，唯一标识
     */
    private String name;

    /**
     * 字段依赖的字段
     */
    private List<String> dependsOn;

    /**
     * 字段处理阶段
     */
    private List<StageVO> stages;

    public FieldVO() {
    }

    public FieldVO(String name, List<String> dependsOn) {
        this.name = name;
        this.dependsOn = dependsOn;
    }
}
