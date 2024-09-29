/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import org.gensokyo.data.database.DbTypeKit;
import org.gensokyo.data.database.dialect.DialectFactory;

import javax.sql.DataSource;

/**
 * SQL工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/23 , Version 1.0.0
 */
public class SqlKit {

    private SqlKit() {
        throw new UnsupportedOperationException();
    }

    public static String toCountSql(String sql) {
        return "SELECT COUNT(1) FROM (" + sql + ") T";
    }

    public static String toPageSql(DataSource ds, String sql, long limit, long offset) {
        DialectFactory.setDbType(DbTypeKit.getDbType(ds));
        var dialect = DialectFactory.getDialect();
        return dialect.forPagination(new StringBuilder(sql), limit, offset);
    }
}
