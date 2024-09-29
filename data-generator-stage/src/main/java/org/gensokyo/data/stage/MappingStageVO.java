/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.stage.StageVO;

import java.util.Map;

/**
 * 数据映射阶段配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/28 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(StageVO.class)
@JsonSubType(value = "MAPPING")
public class MappingStageVO extends StageVO {

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 映射关系
     */
    private Map<String, Object> mapping;
}
