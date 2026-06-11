/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.reader.ReaderVO;

import java.util.Set;

/**
 * Excel读取器配置对象
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/26 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(ReaderVO.class)
@JsonSubType(value = "EXCEL")
public class ExcelReaderVO extends ReaderVO {
    /**
     * 来源路径
     */
    private String path;

    /**
     * 需要读取的sheet列表
     */
    private Set<ExcelSheetVO> sheets = Set.of(new ExcelSheetVO("Sheet1"));
}
