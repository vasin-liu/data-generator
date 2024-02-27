/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.po.WriterPO;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.data.write.Writer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 数据写入阶段类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class WriteStage implements Stage {
    private final WriterPO wpo;
    private final Writer writer;

    @SuppressWarnings("unchecked")
    @Override
    public Value execute(Value input) {
        if (Objects.isNull(input) || input.isNullOrEmpty()) {
            return Value.EMPTY;
        }
        if (input instanceof ListValue lv) {
            Value el = lv.first();
            if (el instanceof MapValue) {
                List<Map<String, Object>> data =(List<Map<String, Object>>) lv.get();
                long rows = writer.write(data);
                log.info("数据写入完成，数据源ID为：{}，目标表为：{}，写入行数：{}。", wpo.getDataSourceId(), wpo.getTarget(), rows);
            }
        }

        return input;
    }
}
