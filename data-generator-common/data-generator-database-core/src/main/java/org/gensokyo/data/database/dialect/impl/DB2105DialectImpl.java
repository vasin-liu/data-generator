/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.database.dialect.impl;


import org.gensokyo.data.database.dialect.KeywordWrap;
import org.gensokyo.data.database.dialect.PaginationProcessor;
import org.gensokyo.data.database.SqlConsts;

import java.util.regex.Pattern;


/**
 * DB2105方言实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/6 , Version 1.0.0
 */
public class DB2105DialectImpl extends CommonsDialectImpl {

    //TODO: 根据DatabaseMetaData获取数据库厂商名和版本号
    public static final String DB2_1005_PRODUCT_VERSION = "1005";
    public static final String DB2_PRODUCT_NAME = "DB2";
    private static final Pattern ORDERBY_PATTERN = Pattern.compile("(\\S+)\\s+(\\S*)\\s*(" + SqlConsts.NULLS_FIRST.trim() + "|" + SqlConsts.NULLS_LAST.trim() + ")");
    private static final Pattern SUBSTRING_PATTERN = Pattern.compile("((?i)" + "SUBSTRING".trim() + ")(\\s*)(\\(.*?\\))");

    public DB2105DialectImpl(KeywordWrap keywordWrap, PaginationProcessor paginationProcessor) {
        super(keywordWrap, paginationProcessor);
    }

    public interface DB2105PaginationProcessor {
    }
}
