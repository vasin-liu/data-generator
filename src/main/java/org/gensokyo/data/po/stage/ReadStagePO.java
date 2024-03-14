/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po.stage;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.ReaderSelectStrategyType;
import org.gensokyo.data.po.reader.ReaderPO;

import java.util.List;
import java.util.Map;

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
     * 缓存数据在内存中，默认为false
     */
    private boolean inMemory = false;

    /**
     * 参数配置（当前只对SQL生效）
     * key参数变量名，值为通过脚本运行取值（常量不需要使用参数，直接写死在SQL中即可）
     */
    private Map<String, ScriptStagePO> params;

    /**
     * 读取器选择策略，默认使用等值选择策略
     */
    private ReaderSelectStrategyType strategyType = ReaderSelectStrategyType.EQUAL;

    /**
     * 数据读取器列表
     */
    private List<ReaderPO> readers;
}
