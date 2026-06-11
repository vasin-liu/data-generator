/*
 * Copyright 漏 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address锛歅CI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou锛孋hina锛圸ip code锛?10653锛?
 */
package org.gensokyo.data.reader;

import tools.jackson.databind.ObjectMapper;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;

import java.io.FileReader;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

/**
 * JSON鏂囦欢璇诲彇鍣?
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/26 , Version 1.0.0
 */
public class JsonReader<S extends ReadStageVO, T extends JsonReaderVO> implements Reader<S, T> {
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public Value read(StageContext<S> ctx, T rvo, Value input) {
        try {
            var file = new FileReader(rvo.getPath());
            var jn = om.readTree(file);
            var r = new ListValue();
            if (jn.isArray()) {
                var startRow = Math.max(rvo.getStartRow(), 1);
                var endRow = rvo.getEndRow() < 1 ? Const.AMOUNT : rvo.getEndRow();
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(jn.iterator(), Spliterator.ORDERED), false)
                        .skip(startRow - 1)
                        .limit(endRow - startRow + 1)
                        .forEach(it -> {
                            if (Objects.nonNull(it)) {
                                //涓嶅鐞嗗祵濂楃殑鏁扮粍
                                r.addValue(SingleValue.of(it));
                            }
                        });
            } else {
                r.addValue(SingleValue.of(jn));
            }
            return r;
        } catch (Exception e) {
            throw new DataGeneratorException(e);
        }
    }
}

