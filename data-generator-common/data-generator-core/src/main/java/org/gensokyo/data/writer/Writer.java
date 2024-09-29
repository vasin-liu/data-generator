/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.writer;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;

import java.util.List;
import java.util.Map;

/**
 * 数据读取器接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/27 , Version 1.0.0
 */
@FunctionalInterface
public interface Writer<S extends WriteStageVO, T extends WriterVO> {

    /**
     * 写入数据集
     *
     * @param ctx     写入上下文
     * @param wvo     写入器配置
     * @param dataset 数据集
     * @return 写入数据量
     */
    long write(final StageContext<S> ctx, final T wvo, final List<Map<String, Object>> dataset);
}
