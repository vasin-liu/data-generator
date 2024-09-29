/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.writer;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.writer.WriterVO;

/**
 * CSV文件写入器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/9/19 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(WriterVO.class)
@JsonSubType(value = "CSV")
public class CsvWriterVO extends WriterVO {

    /**
     * 格式类型
     */
    private String format;

    /**
     * 默认字符集
     */
    private String charset = "GBK";

    /**
     * 追加模式
     */
    private boolean append = false;

    /**
     * 自定义格式
     */
    private CsvFormatVO custom = null;
}
