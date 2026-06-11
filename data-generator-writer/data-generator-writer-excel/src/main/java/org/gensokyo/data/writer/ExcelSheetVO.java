/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.writer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;

import java.io.Serializable;
import java.util.List;

/**
 * Excel的Sheet配置信息
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/26 , Version 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode
public class ExcelSheetVO implements Serializable {
    /**
     * Excel的Sheet名
     */
    private String name;
    /**
     * Excel的列名集合
     */
    private List<List<String>> headers;
    /**
     * 开始行
     */
    private int startRow = 1;
    /**
     * 结束行
     */
    private int endRow = Const.AMOUNT;

    public ExcelSheetVO() {
    }

    public ExcelSheetVO(String name) {
        this.name = name;
    }
}
