/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;

import java.io.Serializable;

/**
 * 元数据
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Setter
@Getter
public class TemplatePO implements Serializable {

    private String name;
    private Integer amount = 0;
    private Integer batchSize = Const.BATCH_SIZE;
    private GlobalPO global;
    private TablePO table;
}
