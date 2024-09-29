/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.model.vo.stage.WriteStageVO;

import java.util.List;
import java.util.Map;

/**
 * JSON数据写入器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/9/19 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class JsonWriter<S extends WriteStageVO, T extends JsonWriterVO> implements Writer<S, T>  {

    @Override
    public long write(StageContext<S> ctx, T wvo, List<Map<String, Object>> dataset) {
        return 0;
    }
}
