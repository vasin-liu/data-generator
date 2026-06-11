/*
 * Copyright 漏 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address锛歅CI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou锛孋hina锛圸ip code锛?10653锛?
 */
package org.gensokyo.data.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.ArrayKit;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CSV鏁版嵁鍐欏叆鍣?
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/9/19 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class CsvWriter<S extends WriteStageVO, T extends CsvWriterVO> implements Writer<S, T> {

    @Override
    public long write(StageContext<S> ctx, T wvo, List<Map<String, Object>> dataset) {
        CSVFormat format = getFormat(wvo);

        var records = new ArrayList<List<Object>>();
        if (ArrayKit.isNotEmpty(format.getHeader())) {
            for (var map : dataset) {
                List<Object> row = new ArrayList<>();
                for (var col : format.getHeader()) {
                    row.add(map.get(col));
                }
                records.add(row);
            }
        } else {
            dataset.forEach(map -> records.add(new ArrayList<>(map.values())));
        }

        try (var writer = new FileWriter(wvo.getTarget(), Charset.forName(wvo.getCharset()), wvo.isAppend());
             var printer = new CSVPrinter(writer, format)) {
            for (var record : records) {
                printer.printRecord(record);
            }
        } catch (IOException e) {
            throw new DataGeneratorException(String.format(
                    "写入数据集时发生异常，写入器类型为：%s，数据源编号为：%s，目标为：%s，写入模板为：%s。",
                    wvo.getType(), wvo.getDataSourceId(), wvo.getTarget(), wvo.getTemplate()), e);
        }
        return dataset.size();
    }

    private CSVFormat getFormat(T wpo) {
        CSVFormat.Builder builder;
        var format = wpo.getFormat();
        var vo = wpo.getCustom();

        if (StrKit.isBlank(format)) {
            builder = CSVFormat.Builder.create();
        } else {
            var f = CSVFormat.valueOf(format);
            builder = CSVFormat.Builder.create(Objects.isNull(f) ? CSVFormat.DEFAULT : f);
        }

        if (Objects.isNull(vo)) {
            return builder.build();
        }

        return vo.copyTo(builder);
    }
}
