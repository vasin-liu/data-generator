/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.IteratorContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CSV迭代器实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/28 , Version 1.0.0
 */
public class CsvIterator<T extends CsvIteratorVO> extends AbstractIterator<T> {
    private final LongAdder counter = new LongAdder();
    private final CSVParser parser;
    private final java.util.Iterator<CSVRecord> iterator;
    private final List<String> headerNames;

    protected CsvIterator(IteratorContext<T> ctx) throws IOException {
        super(ctx);
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.iterator(), "迭代器配置不能为空");
        var it = ctx.iterator();
        Assert.isTrue(it.getStartRow() > 0 && it.getStartRow() < Integer.MAX_VALUE,
                "CSV迭代器配置的开始行数值必须大于0");
        Assert.isTrue(it.getEndRow() > 0 && it.getEndRow() < Integer.MAX_VALUE,
                "CSV迭代器配置的结束行数值必须大于0");
        var format = getFormat(it);
        this.parser = new CSVParser(new BufferedReader(new FileReader(it.getPath())), format);
        var hn = parser.getHeaderNames();
        if (CollectKit.isNotEmpty(hn)) {
            hn = parser.getHeaderNames().stream()
                    .map(header -> {
                        if (StrKit.isNotBlank(header) && StrKit.startWith(header, Const.File.UTF8_BOM)) {
                            return header.substring(1);
                        } else {
                            return header;
                        }
                    })
                    .toList();
        }
        this.headerNames = hn;
        this.iterator = parser.iterator();
    }

    @Override
    public boolean hasNext() {
        if (counter.intValue() < ctx.iterator().getStartRow()) {
            counter.add(ctx.iterator().getStartRow());
        }
        return counter.intValue() < ctx.iterator().getEndRow();
    }

    @Override
    public Value next() {
        var val = MapValue.fromMap(toMap(headerNames, iterator.next()));
        counter.increment();
        return val;
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

    @Override
    public void close() throws Exception {
        super.close();
        parser.close();
    }
}
