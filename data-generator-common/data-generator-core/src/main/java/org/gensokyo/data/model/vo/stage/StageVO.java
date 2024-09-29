/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo.stage;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 步骤配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/26 , Version 1.0.0
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME
        , include = JsonTypeInfo.As.EXISTING_PROPERTY
        , property = "type"
        , visible = true
        // 反序列化时，如果没有匹配到子类，则使用默认实现类，即WriteStagePO，
        // 除了WriteStagePO，其他子类都需要在配置时指定type属性
        //, defaultImpl = WriteStageVO.class
)
public class StageVO implements Serializable {

    private String type;
}
