/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.stage.StageVO;

/**
 * 共享阶段配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/6 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(StageVO.class)
@JsonSubType(value = "SHARED")
public class SharedStageVO extends StageVO {
    /**
     * 共享类型，默认为模板实例内共享
     */
    private SharedType sharedType = SharedType.TEMPLATE_INSTANCE;

    /**
     * 共享Key
     */
    private String sharedKey;

    /**
     * 共享类型枚举
     */
    public enum SharedType {
        /**
         * 所有模板共享
         */
        ALL_TEMPLATE,
        /**
         * 模板共享
         */
        TEMPLATE,
        /**
         * 模板实例共享
         */
        TEMPLATE_INSTANCE,
    }
}
