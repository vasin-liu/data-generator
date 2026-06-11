/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import com.alibaba.excel.EasyExcel;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;

/**
 * Excel文件读取器
 * 注意：由于EasyExcel并不支持单个文件的并发读取，因此使用该读取器时要么将生成器设置为同步模式，要么就将数据一次性加载到内存，
 * 否则可能会获取到空值
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/26 , Version 1.0.0
 */
public class ExcelReader<S extends ReadStageVO, T extends ExcelReaderVO> implements Reader<S, T> {

    @Override
    public Value read(StageContext<S> ctx, T rvo, Value input) {
        var file = rvo.getPath();
        var result = new ListValue();
        try (var reader = EasyExcel.read(file).build()) {
            var sheets = rvo.getSheets()
                    .stream()
                    .map(sheet -> {
                        var sheetName = sheet.getName();
                        var startRow = Math.max(sheet.getStartRow(), 1);
                        var endRow = sheet.getEndRow() < 1 ? Const.AMOUNT : sheet.getEndRow();
                        var listener = new ExcelDataListener(result, endRow - startRow);
                        return EasyExcel.readSheet(sheetName)
                                .headRowNumber(startRow)
                                .head(sheet.getHeaders())
                                .registerReadListener(listener)
                                .build();
                    })
                    .toList();
            reader.read(sheets);
            return result;
        } catch (Exception e) {
            throw new DataGeneratorException(e);
        }
    }
}
