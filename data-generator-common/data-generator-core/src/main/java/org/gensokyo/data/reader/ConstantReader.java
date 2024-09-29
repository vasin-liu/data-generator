/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.model.vo.reader.ConstantReaderVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.value.Value;

/**
 * 常量值数据读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/26 , Version 1.0.0
 */
public class ConstantReader<S extends ReadStageVO, T extends ConstantReaderVO> implements Reader<S, T> {

    @Override
    public Value read(final StageContext<S> ctx, final T rvo, final Value input) {
        return DatasetKit.toValue(rvo.getContent());
    }
}

