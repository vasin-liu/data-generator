/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.ReaderType;

import java.io.Serializable;
import java.util.List;

/**
 * 数据读取阶段配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/28 , Version 1.0.0
 */
@Getter
@Setter
public class ReadStagePO extends StagePO {

    /**
     * 数据集ID，唯一标识
     */
    private String dataSetId;

    /**
     * 缓存数据在内存中
     */
    private boolean inMemory = true;

    /**
     * 数据读取器列表
     */
    private List<ReaderPO> readers;

    @Getter
    @Setter
    public static class ReaderPO implements Serializable {
        /**
         * 数据集ID，唯一标识
         */
        private String dataSetId;

        /**
         * 数据集读取类型
         */
        private ReaderType type;

        /**
         * 数据源ID，数据集所在数据源的唯一标识
         */
        private String dataSourceId;

        /**
         * 数据集
         */
        private Object dataSet;

        /**
         * 数据集执行阶段列表
         */
        private List<StagePO> stages;
    }
}
