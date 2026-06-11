/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.writer;

import com.alibaba.excel.EasyExcel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.WriteStageVO;

import java.util.List;
import java.util.Map;

/**
 * Excel数据写入器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/9/19 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ExcelWriter<S extends WriteStageVO, T extends ExcelWriterVO> implements Writer<S, T> {

    @Override
    public long write(StageContext<S> ctx, T wvo, List<Map<String, Object>> dataset) {
        var file = wvo.getTarget();
        var headers = wvo.getHeaders();
        try (var writer = EasyExcel.write(file).head(headers).build()) {
            var sheet = EasyExcel.writerSheet(wvo.getName()).build();
            writer.write(dataset, sheet);
            return dataset.size();
        } catch (Exception e) {
            throw new DataGeneratorException(e);
        }
    }
}
