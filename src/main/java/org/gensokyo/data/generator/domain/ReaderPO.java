/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.domain;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.generator.constant.ReaderType;

import java.io.Serializable;

/**
 * 数据集读取配置器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@Setter
@Getter
public class ReaderPO implements Serializable {
    private String dataSetId;
    private ReaderType type;
    private String dataSourceId;
    private Object dataSet;
    private ScriptPO postScript;
}
