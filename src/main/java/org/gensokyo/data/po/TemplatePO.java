/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.po.stage.WriteStagePO;

import java.io.Serializable;

/**
 * 元数据
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Setter
@Getter
public class TemplatePO implements Serializable {

    /**
     * 模板名称
     */
    private String name;

    /**
     * 数据量
     */
    private Integer amount = 0;

    /**
     * 批次大小
     */
    private Integer batchSize = Const.BATCH_SIZE;

    /**
     * 全局配置
     */
    private GlobalPO global;

    /**
     * 表配置
     */
    private TablePO table;

    /**
     * 输出阶段配置
     */
    private WriteStagePO output;
}
