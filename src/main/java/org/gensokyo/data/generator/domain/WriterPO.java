/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.domain;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.generator.constant.WriterType;

import java.io.Serializable;

/**
 * 数据写入器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@Setter
@Getter
public class WriterPO implements Serializable {

    private WriterType type;
    private String dataSourceId;
    private String target;
    private String template;
}
