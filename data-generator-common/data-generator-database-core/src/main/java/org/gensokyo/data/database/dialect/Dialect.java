/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.database.dialect;

/**
 * 方言接口定义
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/6 , Version 1.0.0
 */
public interface Dialect {

    /**
     * 数据方言分页SQL构建
     *
     * @param sqlBuilder 查询SQL
     * @param limit      每页记录数
     * @param offset     偏移量
     * @return 分页SQL
     */
    String forPagination(StringBuilder sqlBuilder, Long limit, Long offset);
}
