/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.database.dialect.impl;


import org.gensokyo.data.database.dialect.KeywordWrap;
import org.gensokyo.data.database.dialect.PaginationProcessor;

/**
 * Clickhouse方言实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/6 , Version 1.0.0
 */
public class ClickhouseDialectImpl extends CommonsDialectImpl {

    public static final String ALTER_TABLE = " ALTER TABLE ";
    public static final String CK_DELETE = " DELETE ";
    public static final String CK_UPDATE = " UPDATE ";

    public ClickhouseDialectImpl(KeywordWrap keywordWrap, PaginationProcessor paginationProcessor) {
        super(keywordWrap, paginationProcessor);
    }
}
