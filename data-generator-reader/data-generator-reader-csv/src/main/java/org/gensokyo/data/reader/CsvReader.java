/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CSV文件读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/26 , Version 1.0.0
 */
public class CsvReader<S extends ReadStageVO, T extends CsvReaderVO> implements Reader<S, T> {

    @Override
    public Value read(StageContext<S> ctx, T rvo, Value input) {
        var format = getFormat(rvo);
        try (BufferedReader reader = new BufferedReader(new FileReader(rvo.getPath()));
             CSVParser parser = new CSVParser(reader, format)) {
            var startRow = Math.max(rvo.getStartRow(), 1);
            var endRow = rvo.getEndRow() < 1 ? Const.AMOUNT : rvo.getEndRow();
            var headerNames = parser.getHeaderNames();
            return parser.stream()
                    .skip(startRow - 1)
                    .limit(endRow - startRow)
                    .filter(Objects::nonNull)
                    .map(record -> MapValue.fromMap(toMap(headerNames, record)))
                    .collect(Collectors.collectingAndThen(Collectors.toList(), ListValue::fromValueCollection));
        } catch (Exception e) {
            throw new DataGeneratorException(e);
        }
    }

    private Map<String, String> toMap(List<String> headers, CSVRecord record) {
        final List<String> finalHeaders;
        if (CollectKit.isEmpty(headers)) {
            finalHeaders = IntStream.range(0, record.size()).mapToObj(i -> "" + i).toList();
        } else {
            finalHeaders = headers;
        }

        if (finalHeaders.size() != record.size()) {
            throw new DataGeneratorException("给定的CSV格式与列数不匹配");
        }

        return IntStream.range(0, record.size())
                .boxed()
                .collect(Collectors.toMap(finalHeaders::get, record::get));
    }

    private CSVFormat getFormat(T rpo) {
        CSVFormat.Builder builder;
        var format = rpo.getFormat();
        var vo = rpo.getCustom();

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
