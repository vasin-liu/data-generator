/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.iterator.IteratorVO;

import java.util.Set;

/**
 * Excel文件迭代器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/28 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(IteratorVO.class)
@JsonSubType(value = "EXCEL")
public class ExcelIteratorVO extends IteratorVO {

    /**
     * 来源路径
     */
    private String path;

    /**
     * 需要读取的sheet列表
     */
    private Set<ExcelSheetVO> sheets = Set.of(new ExcelSheetVO("Sheet1"));
}
