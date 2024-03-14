/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po.reader;

import lombok.Getter;
import lombok.Setter;

/**
 * 直JDBC数据读取配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/28 , Version 1.0.0
 */
@Getter
@Setter
public class JdbcReaderPO extends ReaderPO {

    /**
     * 数据源ID，数据集所在数据源的唯一标识
     */
    private String dataSourceId;

    /**
     * 数据集
     */
    private String sql;
}
