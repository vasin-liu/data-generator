/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo.stage;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.selector.reader.EqualReaderSelectStrategyVO;
import org.gensokyo.data.model.vo.selector.reader.ReaderSelectStrategyVO;

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
@AutoService(StageVO.class)
@JsonSubType(value = Const.StageType.READ)
public class ReadStageVO extends StageVO {

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
     */
    private List<ParamVO> params;

    /**
     * 读取器选择策略，默认使用等值选择策略
     */
    private ReaderSelectStrategyVO strategy = new EqualReaderSelectStrategyVO();

    /**
     * 数据读取器列表
     */
    private List<ReaderVO> readers;
}
