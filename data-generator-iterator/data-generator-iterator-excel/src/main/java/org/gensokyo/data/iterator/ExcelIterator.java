/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import com.alibaba.excel.EasyExcel;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.IteratorContext;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Excel迭代器实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/28 , Version 1.0.0
 */
public class ExcelIterator<T extends ExcelIteratorVO> extends AbstractIterator<T> {

    private final com.alibaba.excel.ExcelReader reader;
    private final BlockingQueue<Value> queue;

    protected ExcelIterator(IteratorContext<T> ctx) {
        super(ctx);
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.iterator(), "迭代器配置不能为空");
        var it = ctx.iterator();
        this.queue = new LinkedBlockingQueue<>();
        var file = it.getPath();
        this.reader = EasyExcel.read(file).build();
        var sheets = it.getSheets()
                .stream()
                .map(sheet -> {
                    var sheetName = sheet.getName();
                    var startRow = Math.max(sheet.getStartRow(), 1);
                    var endRow = sheet.getEndRow() < 1 ? Const.AMOUNT : sheet.getEndRow();
                    var listener = new ExcelDataListener(this.queue, endRow - startRow);
                    return EasyExcel.readSheet(sheetName)
                            .headRowNumber(startRow)
                            .head(sheet.getHeaders())
                            .registerReadListener(listener)
                            .build();
                })
                .toList();
        reader.read(sheets);
    }

    @Override
    public boolean hasNext() {
        return !queue.isEmpty();
    }

    @Override
    public Value next() {
        if (hasNext()) {
            return queue.poll();
        }

        throw new IllegalStateException("迭代器已经到达最大值");
    }

    @Override
    public void close() throws Exception {
        super.close();
        reader.close();
    }
}
