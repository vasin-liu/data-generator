/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.database.dialect.impl;

import org.gensokyo.data.database.dialect.Dialect;
import org.gensokyo.data.database.dialect.PaginationProcessor;
import org.gensokyo.data.database.dialect.KeywordWrap;

/**
 * 通用方言实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/6 , Version 1.0.0
 */
public class CommonsDialectImpl implements Dialect {

    protected KeywordWrap keywordWrap = KeywordWrap.BACK_QUOTE;
    private PaginationProcessor paginationProcessor = PaginationProcessor.MYSQL;

    public CommonsDialectImpl() {
    }

    public CommonsDialectImpl(PaginationProcessor paginationProcessor) {
        this.paginationProcessor = paginationProcessor;
    }

    public CommonsDialectImpl(KeywordWrap keywordWrap, PaginationProcessor paginationProcessor) {
        this.keywordWrap = keywordWrap;
        this.paginationProcessor = paginationProcessor;
    }

    @Override
    public String forPagination(StringBuilder sqlBuilder, Long limit, Long offset) {
        return paginationProcessor.process(this, sqlBuilder, limit, offset).toString();
    }
}
