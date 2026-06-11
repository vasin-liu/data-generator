/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.value.Value;

/**
 * 数据读取器接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/27 , Version 1.0.0
 */
public interface Reader<S extends ReadStageVO, T extends ReaderVO> {

    /**
     * 写入数据集
     *
     * @param ctx   读取上下文
     * @param rvo   读取器配置
     * @param input 数据集
     * @return 读取数据结果
     */
    Value read(final StageContext<S> ctx, final T rvo, final Value input);
}
