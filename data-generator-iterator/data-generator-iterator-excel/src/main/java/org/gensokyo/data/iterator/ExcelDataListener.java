/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.LongAdder;

/**
 * Excel数据读取监听器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/26 , Version 1.0.0
 */
public class ExcelDataListener implements ReadListener<Map<String, String>> {
    private final BlockingQueue<Value> data;
    private final int maxRows;
    private final LongAdder counter = new LongAdder();

    public ExcelDataListener(BlockingQueue<Value> data, int maxRows) {
        this.data = data;
        this.maxRows = maxRows;
    }

    @Override
    public void invoke(Map<String, String> row, AnalysisContext analysisContext) {
        try {
            var newRow = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : row.entrySet()) {
                newRow.put(Objects.toString(entry.getKey()), entry.getValue());
            }
            this.data.put(MapValue.fromMap(newRow));
            counter.increment();
        } catch (Exception e) {
            throw new DataGeneratorException(e);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }

    @Override
    public boolean hasNext(AnalysisContext context) {
        return counter.intValue() < maxRows;
    }
}
